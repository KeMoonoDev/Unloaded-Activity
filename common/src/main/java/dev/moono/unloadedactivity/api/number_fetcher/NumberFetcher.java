package dev.moono.unloadedactivity.api.number_fetcher;

import dev.moono.unloadedactivity.api.value_expression.ValueExpression;

import java.util.function.Function;
import java.util.stream.Stream;

public interface NumberFetcher extends ValueExpression<Number> {
    @Override
    default Stream<Number> getPossibleValues() {
        throw new RuntimeException("getPossibleValues function not supported on this type.");
    }

    @Override
    default ValueExpression<Number> replicate() {
        return this;
    }

    @Override
    default void replaceSuper(ValueExpression<Number> superValue) {}

    @Override
    default <U> ValueExpression<U> map(Function<Number, U> mapFunction) {
        throw new RuntimeException("Map function not supported on this type.");
    }
}
