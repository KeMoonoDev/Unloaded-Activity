package dev.moono.unloadedactivity.api.number_fetcher;

import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.api.context.UpdatingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface WeatherAndTimeDependantNumberFetcher extends NumberFetcher {
    Number evaluate(UpdatingContext context);

    long getNextValueSwitchDuration(UpdatingContext context);

    @Override
    default Number evaluate(ExpressionContext context) {
        return this.evaluate((UpdatingContext)context);
    }

    @Override
    default boolean canBeAffectedByWeather() {
        return true;
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
        return this.getNextValueSwitchDuration((UpdatingContext)context);
    }
}
