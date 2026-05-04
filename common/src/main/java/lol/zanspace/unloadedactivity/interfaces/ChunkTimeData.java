package lol.zanspace.unloadedactivity.interfaces;

import com.mojang.datafixers.util.Pair;
import lol.zanspace.unloadedactivity.GroupChunkIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface ChunkTimeData {
    default long getLastTick() {return 0;};

    default void setLastTick(long tick) {};

    default long getSimulationVersion() {return 0;};

    default void setSimulationVersion(long ver) {};

    default HashMap<ResourceLocation, GroupChunkIndex> getGroupIndexes() {return new HashMap<>();};

    default void setGroupIndexes(HashMap<ResourceLocation, GroupChunkIndex> groupIndexes) {};

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
