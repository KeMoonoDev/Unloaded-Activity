package lol.zanspace.unloadedactivity.api.simulation_methods;

import lol.zanspace.unloadedactivity.*;
import lol.zanspace.unloadedactivity.api.SimulationConfig;
import lol.zanspace.unloadedactivity.api.SimulationMethod;
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
    public boolean isFinished(BlockState state, ServerLevel level, BlockPos pos) {
        return false;
    }

    @Override
    public @Nullable DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, long timePassed, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData) {
        Block thisBlock = state.getBlock();
        if (thisBlock instanceof SaplingBlock saplingBlock) {
            OccurrencesAndDuration result = MathUtils.getOccurrences(level, state, pos, GameUtils.getTime(level), timePassed, this.advanceProbability, this.requiresRain, 1, randomPickOdds, false, random, groupSimulateData);

            if (result.occurrences() == 0)
                return DeferredBlockPlacer.empty();

            saplingBlock.treeGrower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);

            return null;
        }

        return DeferredBlockPlacer.empty();
    }
}
