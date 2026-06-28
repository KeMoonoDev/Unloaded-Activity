package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.*;
import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GrowTreeMethod extends SimulationMethod {
    public GrowTreeMethod(SimulationConfig config) {
        super(config);
    }

    @Override
    public boolean canDoMore(BlockState state, ServerLevel level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public @Nullable DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, SimulatedTime simulatedTime, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData) {
        Block thisBlock = state.getBlock();
        if (thisBlock instanceof SaplingBlock saplingBlock) {
            OccurrencesAndTimings result = MathUtils.getOccurrences(level, state, pos, simulatedTime, this.advanceProbability, this.requiresRain, 1, randomPickOdds, false, random, groupSimulateData);

            if (result.occurrences() == 0)
                return DeferredBlockPlacer.empty();

            saplingBlock.treeGrower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);

            return null;
        }

        return DeferredBlockPlacer.empty();
    }
}
