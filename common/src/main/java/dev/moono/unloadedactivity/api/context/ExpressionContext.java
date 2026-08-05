package dev.moono.unloadedactivity.api.context;

#if MC_VER >= MC_1_21_11
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.SimulatedTime;
import net.minecraft.world.level.gamerules.GameRules;
#else
import net.minecraft.world.level.GameRules;
#endif

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.GameUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ExpressionContext extends RandomizedContext {
    private final ServerLevel level;
    private final BlockState state;
    private final BlockPos pos;
    private final long currentTick;
    private final boolean isRaining;
    private final boolean isThundering;
    private final Map<String, Number> numberMap;
    @Nullable
    private final ActiveGroupSimulateData activeGroupSimulateData;


    public static ExpressionContext fixed(ServerLevel level, BlockState state, BlockPos pos, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return new ExpressionContext(level, state, pos, SimulatedTime.PLACEHOLDER, numberMap, activeGroupSimulateData);
    }

    public static ExpressionContext updating(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return new ExpressionContext(level, state, pos, simulatedTime, numberMap, activeGroupSimulateData);
    }

    // Same as updating. No new info is needed for randomized value expressions.
    public static ExpressionContext randomized(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return new ExpressionContext(level, state, pos, simulatedTime, numberMap, activeGroupSimulateData);
    }

    private ExpressionContext(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        this.level = level;
        this.state = state;
        this.pos = pos;
        this.currentTick = simulatedTime.currentTick();

        this.isRaining = GameUtils.isRainingAtTime(level, simulatedTime);
        this.isThundering = false; // todo make thunder be recorded in the weather history

        this.numberMap = numberMap;
        this.activeGroupSimulateData = activeGroupSimulateData;
    }

    @Override
    public RandomSource getRandomSource() {
        return GameUtils.getRand(this.level);
    }

    @Override
    public long getCurrentTime() {
        return this.currentTick;
    }

    @Override
    public boolean isRaining() {
        return this.isRaining;
    }

    @Override
    public boolean isThundering() {
        return this.isThundering;
    }

    @Override
    public LevelReader getLevel() {
        return this.level;
    }

    @Override
    public BlockState getBlockState() {
        return this.state;
    }

    @Override
    public BlockPos getBlockPos() {
        return this.pos;
    }

    @Override
    public Map<String, Number> getNumberMap() {
        return this.numberMap;
    }

    @Override
    public @Nullable ActiveGroupSimulateData getGroupSimulateData() {
        return this.activeGroupSimulateData;
    }

    @Override
    public GameRules getGameRules() {
        return this.level.getGameRules();
    }
}
