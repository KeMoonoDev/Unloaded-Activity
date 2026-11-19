package dev.moono.unloadedactivity.impl.simulation_methods.dynamic_trees;

import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.network.MapSignal;
import com.ferreusveritas.dynamictrees.api.network.NodeInspector;
import com.ferreusveritas.dynamictrees.api.treedata.TreePart;
import com.ferreusveritas.dynamictrees.block.DynamicSaplingBlock;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.init.DTConfigs;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.systems.nodemapper.ShrinkerNode;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SeparableSimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.util.List;

public class GrowDynamicTreeMethod extends SeparableSimulationMethod {
    private final RootyBlock rootyBlock;

    public GrowDynamicTreeMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);

        if (block instanceof RootyBlock rooty) {
            this.rootyBlock = rooty;
        }else{
            throw new RuntimeException("The block " + block + " cannot have this simulation method.");
        }
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        // TODO make a new simulation method which calls a function for each update.
        // This will allow for large update counts while only paying for the updates the method uses.
        return 10000;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings) {
        int iter = 0;
        while (iter < occurrencesAndTimings.occurrences()) {
            iter++;
            if (rootyBlock.getFertility(state, level, pos) == 0) break;
            rootyBlock.updateTree(state, level, pos, GameUtils.getRand(level), true);
            if (Math.floorMod(iter, 10) == 5) { // ageVolume is super expensive. Only call it sometimes. 
                TreeHelper.ageVolume(level, pos, 8, 32, 1, SafeChunkBounds.ANY);
            }
            BlockState newState = level.getBlockState(pos);
            if (newState.getBlock() != state.getBlock()) break;
            state = newState;
        }
        return null;
    }

    @Override
    public boolean isDependable() {
        return false;
    }
}
