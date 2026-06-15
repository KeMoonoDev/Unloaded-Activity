package dev.moono.unloadedactivity.datapack;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ExpressionContext {
    public final ServerLevel level;
    public final BlockState state;
    public final BlockPos pos;
    public final long currentTime;
    public boolean isRaining;
    public boolean isThundering;
    public Map<String, Number> numberMap;
    @Nullable
    public final ActiveGroupSimulateData activeGroupSimulateData;


    public static ExpressionContext fixed(ServerLevel level, BlockState state, BlockPos pos, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return new ExpressionContext(level, state, pos, -1, numberMap, activeGroupSimulateData);
    }

    public static ExpressionContext updating(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return new ExpressionContext(level, state, pos, currentTime, numberMap, activeGroupSimulateData);
    }

    // Same as updating. No new info is needed for randomized value expressions.
    public static ExpressionContext randomized(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return new ExpressionContext(level, state, pos, currentTime, numberMap, activeGroupSimulateData);
    }

    private ExpressionContext(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        this.level = level;
        this.state = state;
        this.pos = pos;
        this.currentTime = currentTime;
        if (currentTime > 0) {
            this.isRaining = level.getWeatherForecast().getWeatherAtTime(currentTime);
            this.isThundering = false;
        } else {
            this.isRaining = false;
            this.isThundering = false;
        }
        this.numberMap = numberMap;
        this.activeGroupSimulateData = activeGroupSimulateData;
    }
}
