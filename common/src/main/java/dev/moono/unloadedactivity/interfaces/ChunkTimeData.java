package dev.moono.unloadedactivity.interfaces;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import dev.moono.unloadedactivity.GroupChunkIndex;

import java.util.ArrayList;

public interface ChunkTimeData {
    default long getLastTick() {return 0;}

    default void setLastTick(long tick) {}

    default long getSimulationVersion() {return 0;}

    default void setSimulationVersion(long ver) {}

    default ArrayList<GroupChunkIndex> getGroupIndexes() {return new ArrayList<>();}

    default GroupChunkIndex getOrCreateGroupIndex(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif groupId) {
        ArrayList<GroupChunkIndex> groupChunkIndexes = getGroupIndexes();

        for (GroupChunkIndex groupChunkIndex : groupChunkIndexes) {
            if (groupChunkIndex.groupId.equals(groupId)) {
                return groupChunkIndex;
            }
        }

        GroupChunkIndex groupChunkIndex = new GroupChunkIndex(new ArrayList<>(), getLastTick(), groupId);
        groupChunkIndexes.add(groupChunkIndex);
        return groupChunkIndex;
    }

    default void setGroupIndexes(ArrayList<GroupChunkIndex> groupIndexes) {}

    default ArrayList<Long> getSimulationBlocks() {return new ArrayList<>();}

    default void setSimulationBlocks(ArrayList<Long> positions) {}
    default void setSimulationBlocks(long[] positions) {
        ArrayList<Long> positionsList = new ArrayList<>();

        for (long value : positions) {
            positionsList.add(value);
        }

        this.setSimulationBlocks(positionsList);
    }

    default void addSimulationBlock(long blockPos) {}

    default void removeSimulationBlock(long blockPos) {}
}
