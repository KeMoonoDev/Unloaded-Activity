package dev.moono.unloadedactivity.api.number_fetcher;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.ExpressionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface RandomizedNumberFetcher extends NumberFetcher {
    Number evaluate(LevelReader level, BlockState state, BlockPos pos, long currentSimulatedTime, boolean isRaining, RandomSource random);

    @Override
    default Number evaluate(ExpressionContext context) {
        return this.evaluate(context.level, context.state, context.pos, context.currentTime, context.isRaining, GameUtils.getRand(context.level));
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
