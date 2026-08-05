package dev.moono.unloadedactivity.api.context;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.SimulatedTime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class UpdatingContext implements TimeDependantContext, WeatherDependantContext {
    public static UpdatingContext of(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return ExpressionContext.updating(level, state, pos, simulatedTime, numberMap, activeGroupSimulateData);
    }

    public static UpdatingContext of(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap) {
        return ExpressionContext.updating(level, state, pos, simulatedTime, numberMap, null);
    }

    public static UpdatingContext of(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return ExpressionContext.updating(level, state, pos, simulatedTime, Map.of(), activeGroupSimulateData);
    }

    public static UpdatingContext of(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime) {
        return ExpressionContext.updating(level, state, pos, simulatedTime, Map.of(), null);
    }
}
