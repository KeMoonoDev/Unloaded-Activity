package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.world.level.block.SnowLayerBlock;

public class MaxSnowHeightValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(FixedContext context) {
        #if MC_VER >= MC_1_21_11
        int maxSnowHeight = context.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
        #elif MC_VER >= MC_1_19_4
        int maxSnowHeight = context.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
        #else
        int maxSnowHeight = 1;
        #endif

        return Math.min(maxSnowHeight, SnowLayerBlock.MAX_HEIGHT);
    }
}
