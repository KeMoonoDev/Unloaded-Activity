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
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RotFruitLeavesMethod extends SeparableSimulationMethod {
    public final FruitLeavesBlock fruitLeavesBlock;

    public RotFruitLeavesMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);
        if (block instanceof FruitLeavesBlock) {
            fruitLeavesBlock = (FruitLeavesBlock) block;
        } else {
            throw new RuntimeException("The block " + block + " cannot have this simulation method.");
        }
    }

    private boolean canRot(ServerLevel level, BlockPos pos) {
        int fruitLeaves = 0;
        int regularLeaves = 0;
        for (Direction direction : Direction.values()) {
            BlockPos relativePos = pos.relative(direction);
            if (level.getBlockState(relativePos).is(fruitLeavesBlock)) fruitLeaves++;
            if (level.getBlockState(relativePos).is(fruitLeavesBlock.leaves)) regularLeaves++;
            for (Direction direction1 : Direction.values()) {
                if (direction1 == direction.getOpposite()) continue;
                BlockPos relativePos1 = relativePos.relative(direction1);
                if (level.getBlockState(relativePos1).is(fruitLeavesBlock)) fruitLeaves++;
                if (level.getBlockState(relativePos1).is(fruitLeavesBlock.leaves)) regularLeaves++;
            }
        }

        return regularLeaves < 4 || fruitLeaves > 4;
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        return canRot(level, pos) ? 1 : 0;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings) {
        DeferredBlockPlacer blockPlacer = new DeferredBlockPlacer();

        SimulatedTime finalTime = occurrencesAndTimings.getFinalTime();

        int distance = state.getValue(FruitLeavesBlock.DISTANCE);
        boolean waterlogged = state.getValue(FruitLeavesBlock.WATERLOGGED);
        if (level.getBlockState(pos.below()).getBlock() instanceof FruitBlock) blockPlacer.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), finalTime);
        blockPlacer.setBlock(pos, fruitLeavesBlock.leaves.value().defaultBlockState().setValue(FruitLeavesBlock.DISTANCE, distance).setValue(FruitLeavesBlock.WATERLOGGED, waterlogged), finalTime);

        return blockPlacer;
    }
}
#else
public class RotFruitLeavesMethod {}
#endif