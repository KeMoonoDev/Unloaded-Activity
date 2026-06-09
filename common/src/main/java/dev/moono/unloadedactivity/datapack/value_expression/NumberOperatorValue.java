package dev.moono.unloadedactivity.datapack.value_expression;

import dev.moono.unloadedactivity.datapack.ValueExpression;
import dev.moono.unloadedactivity.datapack.ValueContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class NumberOperatorValue implements ValueExpression<Number> {
    public final Operator operator;
    public ValueExpression<Number> value;
    @Nullable
    public ValueExpression<Number> secondaryValue;

    public NumberOperatorValue(Operator operator, ValueExpression<Number> value) {
        this(operator, value, null);
    };

    public NumberOperatorValue(Operator operator, ValueExpression<Number> value, @Nullable ValueExpression<Number> secondaryValue) {
        this.operator = operator;
        this.value = value;
        this.secondaryValue = secondaryValue;
    };

    @Override
    public <U> ValueExpression<U> map(Function<Number, U> mapFunction) {
        throw new RuntimeException("Map function not supported on this type.");
    }

    @Override
    public Number evaluate(ValueContext context) {

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
    public boolean isAffectedByWeather(ValueContext context) {
        if (value.isAffectedByWeather(context))
            return true;

        if (secondaryValue != null)
            return secondaryValue.isAffectedByWeather(context);

        return false;
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
    public long getNextValueSwitchDuration(ValueContext context) {
        long firstLong = value.getNextValueSwitchDuration(context);

        if (secondaryValue != null) {
            long secondaryLong = secondaryValue.getNextValueSwitchDuration(context);
            return Math.min(firstLong, secondaryLong);
        }

        return firstLong;
    }

    @Override
    public ValueExpression replicate() {
        return new NumberOperatorValue(operator, value.replicate(), secondaryValue == null ? null : secondaryValue.replicate());
    }

    @Override
    public void replaceSuper(ValueExpression superValue) {
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
