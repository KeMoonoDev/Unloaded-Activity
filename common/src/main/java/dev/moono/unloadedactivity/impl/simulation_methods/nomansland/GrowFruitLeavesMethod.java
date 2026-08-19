package dev.moono.unloadedactivity.impl.simulation_methods.nomansland;

#if MC_VER == MC_1_21_1
import com.farcr.nomansland.common.block.fruit_trees.FruitBlock;
import com.farcr.nomansland.common.block.fruit_trees.FruitLeavesBlock;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SeparableSimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GrowFruitLeavesMethod extends SeparableSimulationMethod {

    public final FruitLeavesBlock fruitLeavesBlock;

    public GrowFruitLeavesMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);
        if (block instanceof FruitLeavesBlock) {
            fruitLeavesBlock = (FruitLeavesBlock) block;
        } else {
            throw new RuntimeException("The block " + block + " cannot have this simulation method.");
        }
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        BlockPos fruitPos = pos.below();
        BlockState fruitState = level.getBlockState(fruitPos);

        if (fruitState.getBlock() instanceof FruitBlock fruitBlock) {
            int fruitAge = fruitState.getValue(FruitBlock.AGE);
            int maxAge = fruitBlock.getMaxAge();
            return maxAge - fruitAge;
        } else if (fruitState.isAir()) {
            Block maybeFruitBlock = fruitLeavesBlock.fruit.value();
            if (maybeFruitBlock instanceof FruitBlock fruitBlock) {
                return fruitBlock.getMaxAge() + 1;
            }
        }
        return 0;
    }

    @Override
    public float probabilityMultiplier(BlockState state, ServerLevel level, BlockPos pos) {
        float growthProbability = 1f / (10f - fruitLeavesBlock.growthSpeed);
        return growthProbability * super.probabilityMultiplier(state, level, pos);
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings) {
        DeferredBlockPlacer blockPlacer = new DeferredBlockPlacer();

        int occurrences = occurrencesAndTimings.occurrences();
        SimulatedTime finalTime = occurrencesAndTimings.getFinalTime();

        BlockPos fruitPos = pos.below();
        BlockState fruitState = level.getBlockState(fruitPos);

        if (fruitState.getBlock() instanceof FruitBlock) {
            int fruitAge = fruitState.getValue(FruitBlock.AGE);
            int newAge = fruitAge + occurrences;
            blockPlacer.setBlock(fruitPos, fruitState.setValue(FruitBlock.AGE, newAge), finalTime);
        } else if (fruitState.isAir()) {
            blockPlacer.setBlock(fruitPos, fruitLeavesBlock.fruit.value().defaultBlockState().setValue(FruitBlock.AGE, occurrences - 1), finalTime);
        }

        return blockPlacer;
    }
}
#else
public class GrowFruitLeavesMethod {}
#endif
