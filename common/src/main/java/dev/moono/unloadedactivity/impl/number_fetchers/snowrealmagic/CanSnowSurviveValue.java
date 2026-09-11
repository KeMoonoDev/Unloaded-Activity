package dev.moono.unloadedactivity.impl.number_fetchers.snowrealmagic;

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.tags.BlockTags;
import snownee.snow.Hooks;
import snownee.snow.SnowCommonConfig;

public class CanSnowSurviveValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(FixedContext context) {
        return Hooks.canSnowSurvive(context.getLevel(), context.getBlockPos()) ? 1 : 0;
    }
}
