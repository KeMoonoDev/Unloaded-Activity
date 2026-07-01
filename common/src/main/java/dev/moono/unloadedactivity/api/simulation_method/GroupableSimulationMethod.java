package dev.moono.unloadedactivity.api.simulation_method;

import dev.moono.unloadedactivity.*;
import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class GroupableSimulationMethod extends SeparableSimulationMethod {

    @Nullable
    public final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif simulateWithGroup;

    public GroupableSimulationMethod(SimulationConfig config, boolean hasDependants) {
        super(config, hasDependants);
        String simulateWithGroup = config.getStringNullable("simulate_with_group");
        if (simulateWithGroup == null) {
            this.simulateWithGroup = null;
        } else {
            this.simulateWithGroup = UnloadedActivity.parseId(simulateWithGroup);
        }
    }

    public abstract int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos);

    public abstract DeferredBlockPlacer.SingleBlockPlacement getNewBlockState(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings, @Nullable ActiveGroupSimulateData groupSimulateData);

    @Override
    public boolean simulatesWithGroup() {
        return this.simulateWithGroup != null;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings) {
        DeferredBlockPlacer blockPlacer = DeferredBlockPlacer.empty();
        DeferredBlockPlacer.SingleBlockPlacement singleBlockPlacement = getNewBlockState(state, level, pos, occurrencesAndTimings, null);
        blockPlacer.setBlock(pos, singleBlockPlacement.blockState(), singleBlockPlacement.updateType(), singleBlockPlacement.placedAtTime());
        return blockPlacer;
    }

    @Override
    public DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, SimulatedTime simulatedTime, float randomPickProbability) {
        int updateCount = this.getMaxUpdateCount(state, level, pos);

        if (updateCount <= 0)
            return DeferredBlockPlacer.empty();

        OccurrencesAndTimings result = MathUtils.getOccurrences(level, state, pos, simulatedTime, this, updateCount, randomPickProbability);

        if (result.occurrences() == 0)
            return DeferredBlockPlacer.empty();

        return this.getNewBlockStates(state, level, pos, result);
    }
}
