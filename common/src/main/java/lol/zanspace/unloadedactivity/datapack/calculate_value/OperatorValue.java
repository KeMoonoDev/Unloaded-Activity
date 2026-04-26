package lol.zanspace.unloadedactivity.datapack.calculate_value;

import lol.zanspace.unloadedactivity.datapack.CalculateValue;
import lol.zanspace.unloadedactivity.datapack.CalculationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class OperatorValue implements CalculateValue {
    public final Operator operator;
    public CalculateValue value;
    @Nullable
    public CalculateValue secondaryValue;

    public OperatorValue(Operator operator, CalculateValue value) {
        this(operator, value, null);
    };

    public OperatorValue(Operator operator, CalculateValue value, @Nullable CalculateValue secondaryValue) {
        this.operator = operator;
        this.value = value;
        this.secondaryValue = secondaryValue;
    };

    @Override
    public double calculateValue(CalculationData data) {

        double value1 = value.calculateValue(data);
        double value2;
        if (secondaryValue != null) {
            value2 = secondaryValue.calculateValue(data);
        } else {
            value2 = 0.0;
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

        return 0;
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
        return new OperatorValue(operator, value.replicate(), secondaryValue == null ? null : secondaryValue.replicate());
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
