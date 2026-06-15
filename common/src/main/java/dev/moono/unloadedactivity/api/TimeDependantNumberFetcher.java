package dev.moono.unloadedactivity.api;

import dev.moono.unloadedactivity.datapack.ExpressionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface TimeDependantNumberFetcher extends NumberFetcher {
    Number evaluate(LevelReader level, BlockState state, BlockPos pos, long currentSimulatedTime);

    long getNextValueSwitchDuration(LevelReader level, BlockState state, BlockPos pos, long currentSimulatedTime);

    @Override
    default Number evaluate(ExpressionContext context) {
        return this.evaluate(context.level, context.state, context.pos, context.currentTime);
    }

    @Override
    default boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    default boolean canBeAffectedByTime() {
        return true;
    }

    @Override
    default boolean isRandom() {
        return false;
    }

    @Override
    default long getNextValueSwitchDuration(ExpressionContext context) {
        return this.getNextValueSwitchDuration(context.level, context.state, context.pos, context.currentTime);
    }
}
