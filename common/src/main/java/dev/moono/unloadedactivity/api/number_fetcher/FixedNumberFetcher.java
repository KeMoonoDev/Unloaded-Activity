package dev.moono.unloadedactivity.api.number_fetcher;

import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.api.context.FixedContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface FixedNumberFetcher extends NumberFetcher {
    Number evaluate(FixedContext context);

    @Override
    default Number evaluate(ExpressionContext context) {
        return this.evaluate((FixedContext)context);
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
        return false;
    }

    @Override
    default long getNextValueSwitchDuration(ExpressionContext context) {
        return Long.MAX_VALUE;
    }
}
