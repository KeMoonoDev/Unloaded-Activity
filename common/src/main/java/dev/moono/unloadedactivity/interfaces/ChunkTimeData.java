package dev.moono.unloadedactivity.interfaces;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import dev.moono.unloadedactivity.GroupChunkIndex;

import java.util.ArrayList;

public interface ChunkTimeData {
    long getLastTick();

    void setLastTick(long tick);

    long getLastMs();

    void setLastMs(long ms);

    long getSimulationVersion();

    void setSimulationVersion(long ver);

    ArrayList<GroupChunkIndex> getGroupIndexes();

    default GroupChunkIndex getOrCreateGroupIndex(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif groupId) {
        ArrayList<GroupChunkIndex> groupChunkIndexes = getGroupIndexes();

        for (GroupChunkIndex groupChunkIndex : groupChunkIndexes) {
            if (groupChunkIndex.groupId.equals(groupId)) {
                return groupChunkIndex;
            }
        }

        GroupChunkIndex groupChunkIndex = new GroupChunkIndex(new ArrayList<>(), getLastTick(), getLastMs(), groupId);
        groupChunkIndexes.add(groupChunkIndex);
        return groupChunkIndex;
    }

    void setGroupIndexes(ArrayList<GroupChunkIndex> groupIndexes);

    ArrayList<Long> getSimulationBlocks();

    void setSimulationBlocks(ArrayList<Long> positions);
    default void setSimulationBlocks(long[] positions) {
        ArrayList<Long> positionsList = new ArrayList<>();

        for (long value : positions) {
            positionsList.add(value);
        }

        this.setSimulationBlocks(positionsList);
    }

    void addSimulationBlock(long blockPos);

    void removeSimulationBlock(long blockPos);
}
