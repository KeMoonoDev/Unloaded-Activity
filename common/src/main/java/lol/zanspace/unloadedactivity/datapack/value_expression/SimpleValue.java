package lol.zanspace.unloadedactivity.datapack.value_expression;

import lol.zanspace.unloadedactivity.datapack.ValueExpression;
import lol.zanspace.unloadedactivity.datapack.ValueContext;

import java.util.function.Function;

public record SimpleValue<T>(T v) implements ValueExpression<T> {
    @Override
    public T evaluate(ValueContext context) {
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
    public long getNextValueSwitchDuration(ValueContext context) {
        return Long.MAX_VALUE;
    }

    @Override
    public ValueExpression<T> replicate() {
        return this;
    }

    @Override
    public void replaceSuper(ValueExpression<T> superValue) {}

    @Override
    public <U> ValueExpression<U> map(Function<T, U> mapFunction) {
        return new SimpleValue<>(mapFunction.apply(v));
    }
}