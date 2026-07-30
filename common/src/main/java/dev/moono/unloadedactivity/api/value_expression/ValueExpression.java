package dev.moono.unloadedactivity.api.value_expression;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.impl.Comparison;
import dev.moono.unloadedactivity.api.condition.Condition;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.impl.value_expression.*;
import dev.moono.unloadedactivity.impl.Operator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public interface ValueExpression<T> {
    T evaluate(ExpressionContext context);

    Stream<T> getPossibleValues();

    boolean canBeAffectedByWeather();

    boolean canBeAffectedByTime();

    boolean isRandom();

    long getNextValueSwitchDuration(ExpressionContext context);

    default long getNextConditionSwitchDuration(ExpressionContext context, float target, Comparison comparison) {
        return getNextValueSwitchDuration(context);
    }

    /// Doesn't guarantee a clone. If a type doesn't get mutated, it's able to return itself.
    ValueExpression<T> replicate();

    void replaceSuper(ValueExpression<T> superValue);

    default boolean isSuper() {
        return false;
    }

    <U> ValueExpression<U> map(Function<T, U> mapFunction);

    static @NotNull ValueExpression<Number> parseNumber(JsonElement input) {
        ValueExpression<Number> result = parseNumberNullable(input);
        if (result == null)
            throw new RuntimeException("Couldn't identify number expression type.");
        return result;
    }

    static @Nullable ValueExpression<Number> parseNumberNullable(JsonElement input) {

        if (input.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = input.getAsJsonPrimitive();

            if (jsonPrimitive.isNumber()) {
                return new SimpleValue<>(jsonPrimitive.getAsNumber().floatValue());
            }

            if (jsonPrimitive.isBoolean()) {
                return new SimpleValue<>(jsonPrimitive.getAsBoolean() ? 1 : 0);
            }

            if (jsonPrimitive.isString()) {
                String fetcherIdUnparsed = jsonPrimitive.getAsString();

                if (fetcherIdUnparsed.equals("super")) {
                    return new SuperValue<>();
                }

                var fetcherId = UnloadedActivity.parseId(fetcherIdUnparsed);

                Optional<ValueExpression<Number>> resolvedFetcher = UnloadedActivity.numberFetcherRegistry.resolve(fetcherId);

                if (resolvedFetcher.isPresent()) {
                    return resolvedFetcher.get();
                }

                throw new RuntimeException(fetcherId + " is not a valid number fetcher.");
            }
        }

        if (input.isJsonObject()) {
            JsonObject jsonObject = input.getAsJsonObject();

            JsonElement fetcherIdUnparsed = jsonObject.get("fetcher_id");
            if (fetcherIdUnparsed != null) {
                var fetcherId = UnloadedActivity.parseId(fetcherIdUnparsed.getAsString());

                Optional<ValueExpression<Number>> resolvedFetcher = UnloadedActivity.numberFetcherRegistry.resolve(fetcherId, jsonObject);

                if (resolvedFetcher.isPresent()) {
                    return resolvedFetcher.get();
                }

                throw new RuntimeException(fetcherId + " is not a valid number fetcher.");
            }

            JsonElement operatorUnparsed = jsonObject.get("operator");
            if (operatorUnparsed != null) {
                String operatorValue = operatorUnparsed.getAsString();

                JsonElement oneValue = jsonObject.get("value");
                JsonElement value1 = jsonObject.get("value1");
                JsonElement value2 = jsonObject.get("value2");

                switch (operatorValue.toLowerCase()) {
                    case "+" -> {
                        return new NumberOperatorValue(Operator.ADD, parseNumber(value1), parseNumber(value2));
                    }
                    case "-" -> {
                        return new NumberOperatorValue(Operator.SUB, parseNumber(value1), parseNumber(value2));
                    }
                    case "/" -> {
                        return new NumberOperatorValue(Operator.DIV, parseNumber(value1), parseNumber(value2));
                    }
                    case "*" -> {
                        return new NumberOperatorValue(Operator.MUL, parseNumber(value1), parseNumber(value2));
                    }
                    case "^" -> {
                        return new NumberOperatorValue(Operator.POW, parseNumber(value1), parseNumber(value2));
                    }
                    case "^2" -> {
                        return new NumberOperatorValue(Operator.POW2, parseNumber(oneValue));
                    }
                    case "floor" -> {
                        return new NumberOperatorValue(Operator.FLOOR, parseNumber(oneValue));
                    }
                }

                throw new RuntimeException("Invalid operator " + operatorValue);

            }

            JsonElement predicateUnparsed = jsonObject.get("predicate");

            if (predicateUnparsed != null) {
                Condition condition = Condition.parse(predicateUnparsed);

                JsonElement trueValue = jsonObject.get("success");
                JsonElement falseValue = jsonObject.get("fail");

                return new ConditionalValue<>(condition, parseNumber(trueValue), parseNumber(falseValue));

            }

            JsonElement timelineUnparsed = jsonObject.get("timeline");

            if (timelineUnparsed != null) {
                JsonObject timelineMapResult = timelineUnparsed.getAsJsonObject();;

                JsonElement periodLengthUnparsed = jsonObject.get("period_length");

                long periodLength;

                if (periodLengthUnparsed == null) {
                    periodLength = 24000;
                } else {
                    periodLength = periodLengthUnparsed.getAsLong();
                }

                JsonElement useDimensionFixedTimeUnparsed = jsonObject.get("use_dimension_fixed_time");

                boolean useDimensionFixedTime;

                if (useDimensionFixedTimeUnparsed == null) {
                    useDimensionFixedTime = true;
                } else {
                    useDimensionFixedTime = useDimensionFixedTimeUnparsed.getAsBoolean();
                }

                ArrayList<Pair<Long, ValueExpression<Number>>> list = new ArrayList<>();

                for (var entry: timelineMapResult.entrySet()) {
                    String key = entry.getKey();
                    try {
                        long number = Long.parseLong(key);
                        list.add(Pair.of(number, parseNumber(entry.getValue())));
                    } catch(NumberFormatException e){
                        throw new RuntimeException("Timeline does contains key values that can't be parsed as a Long.");
                    }
                }

                if (list.isEmpty()) {
                    throw new RuntimeException("Timeline is empty.");
                }

                return new TimelineValue<>(list, periodLength, useDimensionFixedTime);
            }
        }

        return null;
    }

    static @NotNull ValueExpression<String> parseString(JsonElement input) {
        ValueExpression<String> result = parseStringNullable(input);
        if (result == null)
            throw new RuntimeException("Couldn't identify string expression type.");
        return result;
    }

    static @Nullable ValueExpression<String> parseStringNullable(JsonElement input) {

        if (input.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = input.getAsJsonPrimitive();

            if (jsonPrimitive.isString())
                return new SimpleValue<>(jsonPrimitive.getAsString());
        }

        if (input.isJsonObject()) {
            JsonObject jsonObject = input.getAsJsonObject();

            JsonElement predicateUnparsed = jsonObject.get("predicate");

            if (predicateUnparsed != null) {
                Condition condition = Condition.parse(predicateUnparsed);

                JsonElement trueValue = jsonObject.get("success");
                JsonElement falseValue = jsonObject.get("fail");

                return new ConditionalValue<>(condition, parseString(trueValue), parseString(falseValue));

            }

            JsonElement timelineUnparsed = jsonObject.get("timeline");

            if (timelineUnparsed != null) {
                JsonObject timelineMapResult = timelineUnparsed.getAsJsonObject();;

                JsonElement periodLengthUnparsed = jsonObject.get("period_length");

                long periodLength;

                if (periodLengthUnparsed == null) {
                    periodLength = 24000;
                } else {
                    periodLength = periodLengthUnparsed.getAsLong();
                }

                JsonElement useDimensionFixedTimeUnparsed = jsonObject.get("use_dimension_fixed_time");

                boolean useDimensionFixedTime;

                if (useDimensionFixedTimeUnparsed == null) {
                    useDimensionFixedTime = true;
                } else {
                    useDimensionFixedTime = useDimensionFixedTimeUnparsed.getAsBoolean();
                }

                ArrayList<Pair<Long, ValueExpression<String>>> list = new ArrayList<>();

                for (var entry: timelineMapResult.entrySet()) {
                    String key = entry.getKey();
                    try {
                        long number = Long.parseLong(key);
                        list.add(Pair.of(number, parseString(entry.getValue())));
                    } catch(NumberFormatException e){
                        throw new RuntimeException("Timeline does contains key values that can't be parsed as a Long.");
                    }
                }

                if (list.isEmpty()) {
                    throw new RuntimeException("Timeline is empty.");
                }

                return new TimelineValue<>(list, periodLength, useDimensionFixedTime);
            }
        }

        return null;
    }
}