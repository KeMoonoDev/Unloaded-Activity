package lol.zanspace.unloadedactivity.datapack.calculate_value;

import lol.zanspace.unloadedactivity.datapack.CalculateValue;
import lol.zanspace.unloadedactivity.datapack.CalculationData;
import lol.zanspace.unloadedactivity.datapack.Condition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class ConditionalValue implements CalculateValue {

    Condition condition;
    CalculateValue trueValue;
    CalculateValue falseValue;

    public ConditionalValue(Condition condition, CalculateValue trueValue, CalculateValue falseValue) {
        this.condition = condition;
        this.trueValue = trueValue;
        this.falseValue = falseValue;
    }

    @Override
    public double calculateValue(CalculationData data) {
        if (condition.isValid(data)) {
            return trueValue.calculateValue(data);
        } else {
            return falseValue.calculateValue(data);
        }
    }

    @Override
    public boolean isAffectedByWeather(CalculationData data) {
        return condition.isAffectedByWeather(data)
                || trueValue.isAffectedByWeather(data)
                || falseValue.isAffectedByWeather(data);
    }

    @Override
    public boolean canBeAffectedByWeather() {
        return condition.canBeAffectedByWeather()
                || trueValue.canBeAffectedByWeather()
                || falseValue.canBeAffectedByWeather();
    }

    @Override
    public boolean canBeAffectedByTime() {
        return false;
    }

    @Override
    public long getNextValueSwitchDuration(CalculationData data) {
        boolean isValid = condition.isValid(data);
        long conditionSwitch = condition.getNextConditionSwitchDuration(data);

        return Math.min(
                isValid ?
                        trueValue.getNextValueSwitchDuration(data) :
                        falseValue.getNextValueSwitchDuration(data)
                ,
                conditionSwitch
        );
    }

    @Override
    public CalculateValue replicate() {
        return new ConditionalValue(condition, trueValue.replicate(), falseValue.replicate());
    }

    @Override
    public void replaceSuper(CalculateValue superValue) {
        if (trueValue.isSuper()) {
            trueValue = superValue;
        } else {
            trueValue.replaceSuper(superValue);
        }

        if (falseValue.isSuper()) {
            falseValue = superValue;
        } else {
            falseValue.replaceSuper(superValue);
        }
    }
}