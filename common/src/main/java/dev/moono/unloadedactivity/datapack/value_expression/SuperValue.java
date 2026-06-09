package dev.moono.unloadedactivity.datapack.value_expression;

import dev.moono.unloadedactivity.datapack.ValueExpression;
import dev.moono.unloadedactivity.datapack.ValueContext;

import java.util.function.Function;

public class SuperValue<T> implements ValueExpression<T> {
    @Override
    public T evaluate(ValueContext context) {
        return null;
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
    public boolean isSuper() {
        return true;
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
        return new SuperValue<>();
    }
}
