package lol.zanspace.unloadedactivity.api.simulation_methods;

import lol.zanspace.unloadedactivity.*;
import lol.zanspace.unloadedactivity.api.SimulationConfig;
import lol.zanspace.unloadedactivity.api.SimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class GroupableSimulationMethod extends SeparableSimulationMethod {

    @Nullable
    public final Identifier simulateWithGroup;

    public GroupableSimulationMethod(SimulationConfig config) {
        super(config);
        String simulateWithGroup = config.getStringNullable("simulate_with_group");
        if (simulateWithGroup == null) {
            this.simulateWithGroup = null;
        } else {
            this.simulateWithGroup = Identifier.parse(simulateWithGroup);
        }
    }

    public abstract int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos);

    public abstract DeferredBlockPlacer.SingleBlockPlacement getNewBlockState(BlockState state, ServerLevel level, BlockPos pos, int occurrences, long simulationDuration, long timePassed, @Nullable ActiveGroupSimulateData groupSimulateData);

    @Override
    public boolean simulatesWithGroup() {
        return this.simulateWithGroup != null;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, int occurrences, long simulationDuration, long timePassed, @Nullable ActiveGroupSimulateData groupSimulateData) {
        DeferredBlockPlacer blockPlacer = DeferredBlockPlacer.empty();
        DeferredBlockPlacer.SingleBlockPlacement singleBlockPlacement = getNewBlockState(state, level, pos, occurrences, simulationDuration, timePassed, groupSimulateData);
        blockPlacer.setBlock(pos, singleBlockPlacement.blockState(), singleBlockPlacement.updateType(), singleBlockPlacement.duration());
        return blockPlacer;
    }

    @Override
    public DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, long timePassed, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData) {
        int updateCount = this.getMaxUpdateCount(state, level, pos);
        boolean calculateDuration = hasDependents || this.shouldCalculateDuration(state, level, pos);

        if (updateCount <= 0)
            return DeferredBlockPlacer.empty();

        long currentTime = GameUtils.getTime(level);

        OccurrencesAndDuration result = MathUtils.getOccurrences(level, state, pos, currentTime, timePassed, this.advanceProbability, this.requiresRain, updateCount, randomPickOdds, calculateDuration, random, groupSimulateData);

        if (result.occurrences() == 0)
            return DeferredBlockPlacer.empty();

        return this.getNewBlockStates(state, level, pos, result.occurrences(), result.duration(), timePassed, groupSimulateData);
    }
}
