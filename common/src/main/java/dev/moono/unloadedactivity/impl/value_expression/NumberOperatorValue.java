package dev.moono.unloadedactivity.impl.value_expression;

import dev.moono.unloadedactivity.api.number_fetcher.NumberFetcher;
import dev.moono.unloadedactivity.api.value_expression.ValueExpression;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.impl.Operator;
import org.jetbrains.annotations.Nullable;

public class NumberOperatorValue implements NumberFetcher {
    public final Operator operator;
    public ValueExpression<Number> value;
    @Nullable
    public ValueExpression<Number> secondaryValue;

    public NumberOperatorValue(Operator operator, ValueExpression<Number> value) {
        this(operator, value, null);
    }

    public NumberOperatorValue(Operator operator, ValueExpression<Number> value, @Nullable ValueExpression<Number> secondaryValue) {
        this.operator = operator;
        this.value = value;
        this.secondaryValue = secondaryValue;
    }

    @Override
    public Number evaluate(ExpressionContext context) {

        float value1 = value.evaluate(context).floatValue();
        float value2;
        if (secondaryValue != null) {
            value2 = secondaryValue.evaluate(context).floatValue();
        } else {
            value2 = 0F;
        }

        switch (operator) {
            case ADD -> {
                return value1 + value2;
            }
            case SUB -> {
                return value1 - value2;
            }
            case DIV -> {
                return value1 / value2;
            }
            case MUL -> {
                return value1 * value2;
            }
            case POW -> {
                return Math.pow(value1, value2);
            }
            case POW2 -> {
                return value1 * value1;
            }
            case FLOOR -> {
                return Math.floor(value1);
            }
        }

        return Float.NaN;
    }

    @Override
    public boolean canBeAffectedByWeather() {
        if (value.canBeAffectedByWeather())
            return true;

        if (secondaryValue != null)
            return secondaryValue.canBeAffectedByWeather();

        return false;
    }

    @Override
    public boolean canBeAffectedByTime() {
        if (value.canBeAffectedByTime())
            return true;

        if (secondaryValue != null)
            return secondaryValue.canBeAffectedByTime();

        return false;
    }

    @Override
    public boolean isRandom() {
        if (value.isRandom())
            return true;

        if (secondaryValue != null)
            return secondaryValue.isRandom();

        return false;
    }

    @Override
    public long getNextValueSwitchDuration(ExpressionContext context) {
        long firstLong = value.getNextValueSwitchDuration(context);

        if (secondaryValue != null) {
            long secondaryLong = secondaryValue.getNextValueSwitchDuration(context);
            return Math.min(firstLong, secondaryLong);
        }

        return firstLong;
    }

    @Override
    public ValueExpression<Number> replicate() {
        return new NumberOperatorValue(operator, value.replicate(), secondaryValue == null ? null : secondaryValue.replicate());
    }

    @Override
    public void replaceSuper(ValueExpression<Number> superValue) {
        if (value.isSuper()) {
            value = superValue;
        } else {
            value.replaceSuper(superValue);
        }

        if (secondaryValue != null) {
            if (secondaryValue.isSuper()) {
                secondaryValue = superValue;
            } else {
                secondaryValue.replaceSuper(superValue);
            }
        }
    }
}
