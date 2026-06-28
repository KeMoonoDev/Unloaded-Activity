package dev.moono.unloadedactivity;

import dev.moono.unloadedactivity.api.SimulatedTime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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

    /// Check isEmpty first before calling. Throws if empty.
    public BlockPlacementInfo lastPlacedBlock() {
        sortListIfNeeded();
        if (this.size() <= 0) throw new RuntimeException("Block placer was empty.");
        return blockPlacements.get(this.size()-1);
    }

    private void sortListIfNeeded() {
        if (isSorted) return;
        blockPlacements.sort(Comparator.comparingLong((blockPlacement -> blockPlacement.placedAtTime.currentTime())));
        isSorted = true;
    }

    public void setBlock(BlockPos pos, BlockState state, boolean updateNeighbors, int flags, SimulatedTime placedAtTime) {
        this.blockPlacements.add(new BlockPlacementInfo(pos, state, updateNeighbors, flags, placedAtTime));
        isSorted = false;
    }

    public void setBlock(BlockPos pos, BlockState state, boolean updateNeighbors, SimulatedTime placedAtTime) {
        this.setBlock(pos, state, updateNeighbors, Block.UPDATE_ALL, placedAtTime);
    }

    public void setBlock(BlockPos pos, BlockState state, int flags, SimulatedTime placedAtTime) {
        this.setBlock(pos, state, false, flags, placedAtTime);
    }

    public void setBlock(BlockPos pos, BlockState state, SimulatedTime placedAtTime) {
        this.setBlock(pos, state, false, placedAtTime);
    }

    public record BlockPlacementInfo(BlockPos blockPos, BlockState blockState, boolean updateNeighbors, int updateType, SimulatedTime placedAtTime) {}

    public record SingleBlockPlacement(BlockState blockState, int updateType, SimulatedTime placedAtTime) {
        public SingleBlockPlacement(BlockState blockState, SimulatedTime placedAtTime) {
            this(blockState, Block.UPDATE_ALL, placedAtTime);
        }
    }
}
