package dev.moono.unloadedactivity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;

public class DeferredBlockPlacer {
    private final ArrayList<BlockPlacementInfo> blockPlacements = new ArrayList<>();
    private boolean isSorted = false;

    public static DeferredBlockPlacer empty() {
        return new DeferredBlockPlacer();
    }

    public void forEach(Consumer<? super BlockPlacementInfo> action) {
        this.sortListIfNeeded();
        blockPlacements.forEach(action);
    }

    public int size() {
        return blockPlacements.size();
    }

    public boolean isEmpty() {
        return blockPlacements.isEmpty();
    }

    public BlockPlacementInfo get(int index) {
        return blockPlacements.get(index);
    }

    public long maxDuration() {
        sortListIfNeeded();
        if (this.size() <= 0) return 0;
        return blockPlacements.get(this.size()-1).duration();
    }

    private void sortListIfNeeded() {
        if (isSorted) return;
        blockPlacements.sort(Comparator.comparingLong(BlockPlacementInfo::duration));
        isSorted = true;
    }

    public void setBlock(BlockPos pos, BlockState state, boolean updateNeighbors, int flags, long duration) {
        this.blockPlacements.add(new BlockPlacementInfo(pos, state, updateNeighbors, flags, duration));
        isSorted = false;
    }

    public void setBlock(BlockPos pos, BlockState state, boolean updateNeighbors, long duration) {
        this.setBlock(pos, state, updateNeighbors, Block.UPDATE_ALL, duration);
    }

    public void setBlock(BlockPos pos, BlockState state, int flags, long duration) {
        this.setBlock(pos, state, false, flags, duration);
    }

    public void setBlock(BlockPos pos, BlockState state, long duration) {
        this.setBlock(pos, state, false, duration);
    }

    public record BlockPlacementInfo(BlockPos blockPos, BlockState blockState, boolean updateNeighbors, int updateType, long duration) {}

    public record SingleBlockPlacement(BlockState blockState, int updateType, long duration) {
        public SingleBlockPlacement(BlockState blockState, long duration) {
            this(blockState, Block.UPDATE_ALL, duration);
        }

        public static SingleBlockPlacement empty() {
            return new SingleBlockPlacement(null, Block.UPDATE_ALL, 0);
        }
    }
}
