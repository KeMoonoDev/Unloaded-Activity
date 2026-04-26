package lol.zanspace.unloadedactivity.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface ChunkTimeData {
    default long getLastTick() {return 0;};

    default void setLastTick(long tick) {};

    default Map<ResourceLocation, Long> getLastGroupTicks() {return Map.of();};

    default long getLastGroupTick(ResourceLocation groupId) {return 0;};

    default void setLastGroupTick(ResourceLocation groupId, long tick) {};

    default long getSimulationVersion() {return 0;};

    default void setSimulationVersion(long ver) {};

    default HashMap<ResourceLocation, ArrayList<BlockPos>> getGroupedBlocks() {return new HashMap<>();};

    default void setGroupedBlocks(HashMap<ResourceLocation, ArrayList<BlockPos>> groupedBlocks) {};

    default ArrayList<BlockPos> getBlocksInGroup(ResourceLocation id) {return this.getGroupedBlocks().getOrDefault(id, new ArrayList<>());};

    default ArrayList<Long> getSimulationBlocks() {return new ArrayList<>();};

    default void setSimulationBlocks(ArrayList<Long> positions) {};
    default void setSimulationBlocks(long[] positions) {
        ArrayList<Long> positionsList = new ArrayList<>();

        for (long value : positions) {
            positionsList.add(value);
        }

        this.setSimulationBlocks(positionsList);
    };

    default void addSimulationBlock(long blockPos) {};

    default void removeSimulationBlock(long blockPos) {};
}
