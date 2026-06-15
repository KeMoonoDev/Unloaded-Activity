package dev.moono.unloadedactivity.api.number_fetchers;

#if MC_VER >= MC_1_21_11
import net.minecraft.world.level.gamerules.GameRules;
#else
import net.minecraft.world.level.GameRules;
#endif

import dev.moono.unloadedactivity.api.NumberFetcher;
import dev.moono.unloadedactivity.datapack.ExpressionContext;
import net.minecraft.world.level.block.SnowLayerBlock;

public class MaxSnowHeightValue implements NumberFetcher {
    @Override
    public Number evaluate(ExpressionContext context) {
        #if MC_VER >= MC_1_21_11
        int maxSnowHeight = context.level.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
        #elif MC_VER >= MC_1_19_4
        int maxSnowHeight = context.level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
        #else
        int maxSnowHeight = 1;
        #endif

        return Math.min(maxSnowHeight, SnowLayerBlock.MAX_HEIGHT);
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
    public long getNextValueSwitchDuration(ExpressionContext context) {
        return Long.MAX_VALUE;
    }
}
