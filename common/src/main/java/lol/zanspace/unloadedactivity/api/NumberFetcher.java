package lol.zanspace.unloadedactivity.api;

import lol.zanspace.unloadedactivity.datapack.ValueExpression;

import java.util.function.Function;

public interface NumberFetcher extends ValueExpression<Number> {
    @Override
    default ValueExpression<Number> replicate() {
        return this;
    };

    @Override
    default void replaceSuper(ValueExpression<Number> superValue) {}

    @Override
    default <U> ValueExpression<U> map(Function<Number, U> mapFunction) {
        throw new RuntimeException("Map function not supported on this type.");
    }
}
