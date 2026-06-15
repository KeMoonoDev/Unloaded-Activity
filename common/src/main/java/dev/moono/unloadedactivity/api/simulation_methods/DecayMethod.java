package dev.moono.unloadedactivity.api.simulation_methods;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.api.SimulationConfig;
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
    public DeferredBlockPlacer.SingleBlockPlacement getNewBlockState(BlockState state, ServerLevel level, BlockPos pos, int occurrences, long simulationDuration, long timePassed, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (this.dropsResources) {
            Block.dropResources(state, level, pos);
        }

        return new DeferredBlockPlacer.SingleBlockPlacement(state.getFluidState().createLegacyBlock(), simulationDuration);
    }
}
