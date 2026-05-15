package lol.zanspace.unloadedactivity.datapack.calculate_value;

import lol.zanspace.unloadedactivity.datapack.CalculateValue;
import lol.zanspace.unloadedactivity.datapack.CalculationData;

import java.util.function.Function;

public record SimpleValue<T>(T v) implements CalculateValue<T> {
    @Override
    public T calculateValue(CalculationData data) {
        return v;
    }

    @Override
    public boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    public boolean canBeAffectedByTime() {
        return false;
    }

    @Override
    public boolean isRandom() {
        return false;
    }

    @Override
    public long getNextValueSwitchDuration(CalculationData data) {
        return Long.MAX_VALUE;
    }

    @Override
    public CalculateValue<T> replicate() {
        return this;
    }

    @Override
    public void replaceSuper(CalculateValue<T> superValue) {}

    @Override
    public <U> CalculateValue<U> map(Function<T, U> mapFunction) {
        return new SimpleValue<>(mapFunction.apply(v));
    }
}