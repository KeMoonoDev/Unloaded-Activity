package lol.zanspace.unloadedactivity.datapack.calculate_value;

import lol.zanspace.unloadedactivity.datapack.CalculateValue;
import lol.zanspace.unloadedactivity.datapack.CalculationData;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class NumberOperatorValue implements CalculateValue<Number> {
    public final Operator operator;
    public CalculateValue<Number> value;
    @Nullable
    public CalculateValue<Number> secondaryValue;

    public NumberOperatorValue(Operator operator, CalculateValue<Number> value) {
        this(operator, value, null);
    };

    public NumberOperatorValue(Operator operator, CalculateValue<Number> value, @Nullable CalculateValue<Number> secondaryValue) {
        this.operator = operator;
        this.value = value;
        this.secondaryValue = secondaryValue;
    };

    @Override
    public <U> CalculateValue<U> map(Function<Number, U> mapFunction) {
        throw new RuntimeException("Map function not supported on this type.");
    }

    @Override
    public Number calculateValue(CalculationData data) {

        float value1 = value.calculateValue(data).floatValue();
        float value2;
        if (secondaryValue != null) {
            value2 = secondaryValue.calculateValue(data).floatValue();
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
            case FLOOR -> {
                return Math.floor(value1);
            }
        }

        return Float.NaN;
    }

    @Override
    public boolean isAffectedByWeather(CalculationData data) {
        if (value.isAffectedByWeather(data))
            return true;

        if (secondaryValue != null)
            return secondaryValue.isAffectedByWeather(data);

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
    public long getNextValueSwitchDuration(CalculationData data) {
        long firstLong = value.getNextValueSwitchDuration(data);

        if (secondaryValue != null) {
            long secondaryLong = secondaryValue.getNextValueSwitchDuration(data);
            return Math.min(firstLong, secondaryLong);
        }

        return firstLong;
    }

    @Override
    public CalculateValue replicate() {
        return new NumberOperatorValue(operator, value.replicate(), secondaryValue == null ? null : secondaryValue.replicate());
    }

    @Override
    public void replaceSuper(CalculateValue superValue) {
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
