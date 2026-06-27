package dev.moono.unloadedactivity.api.context;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class RandomizedContext extends UpdatingContext {
    public abstract RandomSource getRandomSource();

    public static RandomizedContext of(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return ExpressionContext.randomized(level, state, pos, currentTime, numberMap, activeGroupSimulateData);
    }

    public static RandomizedContext of(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap) {
        return ExpressionContext.randomized(level, state, pos, currentTime, numberMap, null);
    }

    public static RandomizedContext of(ServerLevel level, BlockState state, BlockPos pos, long currentTime, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return ExpressionContext.randomized(level, state, pos, currentTime, Map.of(), activeGroupSimulateData);
    }

    public static RandomizedContext of(ServerLevel level, BlockState state, BlockPos pos, long currentTime) {
        return ExpressionContext.randomized(level, state, pos, currentTime, Map.of(), null);
    }
}
