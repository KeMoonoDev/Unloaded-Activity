package dev.moono.unloadedactivity.api.number_fetcher;

import dev.moono.unloadedactivity.api.ExpressionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface WeatherDependantNumberFetcher extends NumberFetcher {
    Number evaluate(LevelReader level, BlockState state, BlockPos pos, boolean isRaining);

    @Override
    default Number evaluate(ExpressionContext context) {
        return this.evaluate(context.level, context.state, context.pos, context.isRaining);
    }

    @Override
    default boolean canBeAffectedByWeather() {
        return true;
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
