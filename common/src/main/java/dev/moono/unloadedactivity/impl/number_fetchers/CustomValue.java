package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import dev.moono.unloadedactivity.api.number_fetcher.NumberFetcher;
import dev.moono.unloadedactivity.api.context.ExpressionContext;

public class CustomValue implements FixedNumberFetcher {
    public final String valueKey;

    public CustomValue(String valueKey) {
        this.valueKey = valueKey;
    }

    @Override
    public Number evaluate(FixedContext context) {
        return context.getNumberMap().get(valueKey);
    }
}
