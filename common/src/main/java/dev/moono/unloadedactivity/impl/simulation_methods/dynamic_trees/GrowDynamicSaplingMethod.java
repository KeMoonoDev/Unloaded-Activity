package dev.moono.unloadedactivity.impl.simulation_methods.dynamic_trees;

import com.ferreusveritas.dynamictrees.block.DynamicSaplingBlock;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SeparableSimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GrowDynamicSaplingMethod extends SeparableSimulationMethod {
    private final Block block;
    private final Method getSpecies;

    public GrowDynamicSaplingMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);

        if (!(block instanceof DynamicSaplingBlock)) {
            throw new RuntimeException("The block " + block + " cannot have this simulation method.");
        }

        try {
            this.getSpecies = DynamicSaplingBlock.class.getMethod("getSpecies");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.block = block;
    }

    @Override
    public boolean shouldCalculateDuration(BlockState state, ServerLevel level, BlockPos pos) {
        return true;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        return 1;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings) {
        Species species;
        try {
            species = (Species)this.getSpecies.invoke(block);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (block.canSurvive(state, level, pos)) {
            Species overrideSpecies = species.selfOrLocationOverride(level, pos);
            if (overrideSpecies.canSaplingGrow(level, pos)) {
                overrideSpecies.transitionToTree(level, pos);

                var blockPlacer = new DeferredBlockPlacer();
                BlockPos below = pos.below();
                blockPlacer.reportBlock(below, level.getBlockState(below), occurrencesAndTimings.getTimeAtOccurrence(1));
                return blockPlacer;
            }
        } else {
            //dynamicSaplingBlock.dropBlock(level, state, pos); this method is PROTECTEEDDDD
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, species.getSeedStack(1)));
            level.removeBlock(pos, false);
        }

        return null;
    }

    @Override
    public boolean isDependable() {
        return false;
    }
}
