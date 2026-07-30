package dev.moono.unloadedactivity.api.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.api.value_expression.ValueExpression;
import dev.moono.unloadedactivity.impl.Comparison;
import dev.moono.unloadedactivity.impl.value_expression.SimpleValue;

import java.util.Optional;

public record Condition (ValueExpression<Number> value1, ValueExpression<Number> value2, Comparison comparison) {
    public boolean isValid(ExpressionContext context) {
        Number calculatedValue1 = value1.evaluate(context);
        Number calculatedValue2 = value2.evaluate(context);
        boolean result = comparison.compare(calculatedValue1.floatValue(), calculatedValue2.floatValue());

        if (UnloadedActivity.config.debugLogs)
            UnloadedActivity.LOGGER.info("Checking if " + value1.getClass().getSimpleName() + " (" + calculatedValue1 + ") " + comparison.name() + " " + value2.getClass().getSimpleName() +  " (" + calculatedValue2 + ") (" + result + ")");

        return result;
    }

    public boolean canBeAffectedByWeather() {
        return value1.canBeAffectedByWeather() || value2.canBeAffectedByWeather();
    }
    public boolean isRandom() {
        return value1.isRandom() || value2.isRandom();
    }
    public boolean canBeAffectedByTime() {
        return value1.canBeAffectedByTime() || value2.canBeAffectedByTime();
    }

    public long getNextConditionSwitchDuration(ExpressionContext context) {
        float value2Float = value2.evaluate(context).floatValue();

        return Math.min(
            value1.getNextConditionSwitchDuration(context, value2Float, comparison),
            value2.getNextValueSwitchDuration(context)
        );
    }

    public static Condition parse(JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            JsonElement comparisonUnparsed = jsonObject.get("comparison");

            if (comparisonUnparsed != null) {
                String comparisonString = comparisonUnparsed.getAsString();
                Optional<Comparison> maybeComparison = Comparison.fromString(comparisonString);

                if (maybeComparison.isEmpty()) {
                    throw new RuntimeException(comparisonString + " is not a valid comparison.");
                }

                Comparison comparison = maybeComparison.get();

                JsonElement checkUnparsed = jsonObject.get("check");
                ValueExpression<Number> check = ValueExpression.parseNumber(checkUnparsed);

                JsonElement valueUnparsed = jsonObject.get("value");
                ValueExpression<Number> value = ValueExpression.parseNumber(valueUnparsed);

                return new Condition(check, value, comparison);
            }
        }

        ValueExpression<Number> numberExpression = ValueExpression.parseNumberNullable(jsonElement);

        if (numberExpression != null)
            // TODO make Condition an interface and make a NonZeroCondition class to maybe save on performance
            return new Condition(numberExpression, new SimpleValue<>(0), Comparison.NE);

        throw new RuntimeException("Couldn't identify condition type.");
    }
}
