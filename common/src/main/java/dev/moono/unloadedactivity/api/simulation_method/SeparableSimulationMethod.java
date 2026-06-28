package dev.moono.unloadedactivity.api.simulation_method;

import dev.moono.unloadedactivity.*;
import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class SeparableSimulationMethod extends SimulationMethod {
    public SeparableSimulationMethod(SimulationConfig config) {
        super(config);
    }

    @Override
    public boolean canDoMore(BlockState state, ServerLevel level, BlockPos pos) {
        return this.getMaxUpdateCount(state, level, pos) > 0;
    }

    public abstract int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos);

    public abstract DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings, @Nullable ActiveGroupSimulateData groupSimulateData);

    @Override
    public DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, SimulatedTime simulatedTime, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData) {
        int updateCount = this.getMaxUpdateCount(state, level, pos);
        boolean calculateDuration = hasDependents || this.shouldCalculateDuration(state, level, pos);

        if (updateCount <= 0)
            return DeferredBlockPlacer.empty();

        long currentTime = GameUtils.getTime(level);

        OccurrencesAndTimings result = MathUtils.getOccurrences(level, state, pos, simulatedTime, this.advanceProbability, this.requiresRain, updateCount, randomPickOdds, calculateDuration, random, groupSimulateData);

        if (result.occurrences() == 0)
            return DeferredBlockPlacer.empty();

        return this.getNewBlockStates(state, level, pos, result, groupSimulateData);
    }
}
