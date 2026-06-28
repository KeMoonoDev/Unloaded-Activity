package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.GroupableSimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DecayMethod extends GroupableSimulationMethod {
    public final boolean dropsResources;

    public DecayMethod(SimulationConfig config) {
        super(config);
        this.dropsResources = config.getBooleanOrDefault("drops_resources", true);
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        return 1;
    }

    @Override
    public DeferredBlockPlacer.SingleBlockPlacement getNewBlockState(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (this.dropsResources) Block.dropResources(state, level, pos);
        return new DeferredBlockPlacer.SingleBlockPlacement(state.getFluidState().createLegacyBlock(), occurrencesAndTimings.getFinalTime());
    }
}
