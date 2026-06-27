package dev.moono.unloadedactivity.impl.value_expression;

import dev.moono.unloadedactivity.api.value_expression.ValueExpression;
import dev.moono.unloadedactivity.api.context.ExpressionContext;

import java.util.function.Function;
import java.util.stream.Stream;

public class SuperValue<T> implements ValueExpression<T> {
    @Override
    public T evaluate(ExpressionContext context) {
        return null;
    }

    @Override
    public Stream<T> getPossibleValues() {
        return Stream.empty();
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
    public long getNextValueSwitchDuration(ExpressionContext context) {
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
