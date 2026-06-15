package dev.moono.unloadedactivity.api.number_fetchers;

import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.NumberFetcher;
import dev.moono.unloadedactivity.datapack.ExpressionContext;

public class CustomValue implements NumberFetcher {
    public final String valueKey;

    public CustomValue(String valueKey) {
        this.valueKey = valueKey;
    }

    @Override
    public Number evaluate(ExpressionContext context) {
        return context.numberMap.get(valueKey);
    }

    @Override
    public boolean canBeAffectedByTime() {
        return false;
    }

    @Override
    public boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    public boolean isRandom() {
        return false;
    }

    @Override
    public long getNextValueSwitchDuration(ExpressionContext context) {
        return Long.MAX_VALUE;
    }
}
