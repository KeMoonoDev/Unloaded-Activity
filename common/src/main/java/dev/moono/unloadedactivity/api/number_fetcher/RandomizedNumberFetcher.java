package dev.moono.unloadedactivity.api.number_fetcher;

import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.api.context.RandomizedContext;

public interface RandomizedNumberFetcher extends NumberFetcher {
    Number evaluate(RandomizedContext context);

    @Override
    default Number evaluate(ExpressionContext context) {
        return this.evaluate((RandomizedContext)context);
    }

    @Override
    default boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    default boolean canBeAffectedByTime() {
        return false;
    }

    @Override
    default boolean isRandom() {
        return true;
    }

    @Override
    default long getNextValueSwitchDuration(ExpressionContext context) {
        return 0;
    }
}
