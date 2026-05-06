package lol.zanspace.unloadedactivity;

import lol.zanspace.unloadedactivity.datapack.GroupInfoResource;
import lol.zanspace.unloadedactivity.datapack.GroupMemberInfo;
import lol.zanspace.unloadedactivity.datapack.SimulateProperty;
import lol.zanspace.unloadedactivity.datapack.SimulationData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class GroupChunkIndex {
    private ArrayList<Long> positions;
    private long lastTick;
    public final ResourceLocation groupId;

    public long getLastTick(long lastSimulationTick) {
        return Math.max(lastSimulationTick, lastTick);
    }

    public void setLastTick(long currentTime) {
        this.lastTick = currentTime;
    }

    public void setPositions(long[] positions) {
        ArrayList<Long> positionsList = new ArrayList<>();

        for (long value : positions) {
            positionsList.add(value);
        }

        this.setPositions(positionsList);
    }

    public void setPositions(ArrayList<Long> positions) {
        this.positions = positions;
    }

    public ArrayList<Long> getPositions() {
        return this.positions;
    }

    public ArrayList<ActiveGroupSimulateData> getAndFilterBlocks(LevelChunk chunk) {

        ArrayList<ActiveGroupSimulateData> blockInfoList = new ArrayList<>(this.positions.size());

        this.positions.removeIf((longPos) -> {
            BlockPos pos = BlockPos.of(longPos);
            BlockState state = chunk.getBlockState(pos);
            Block block = state.getBlock();

            List<GroupMemberInfo> memberInfoList = GroupInfoResource.getBlockMemberInfo(block);
            Optional<GroupMemberInfo> maybeGroupMemberInfo = memberInfoList.stream().filter((info) -> info.groupInfo.id.equals(groupId)).findFirst();
            if (maybeGroupMemberInfo.isEmpty())
                return true;

            if (UnloadedActivity.config.isBlockBlacklisted(state)) {
                return false;
            }

            GroupMemberInfo groupMemberInfo = maybeGroupMemberInfo.get();

            Optional<SimulateProperty> property = Optional.empty();

            SimulationData simulationData = block.getSimulationData();
            for (SimulateProperty simulateProperty : simulationData.propertyMap.values()) {
                var maybePropertyGroupId = simulateProperty.simulateWithGroup;

                if (maybePropertyGroupId.isEmpty())
                    continue;

                var propertyGroupId = maybePropertyGroupId.get();

                if (propertyGroupId.equals(this.groupId)) {
                    property = Optional.of(simulateProperty);
                    break;
                }
            }

            blockInfoList.add(new ActiveGroupSimulateData(pos, state, property, groupMemberInfo));

            return false;
        });

        return blockInfoList;
    }

    public GroupChunkIndex(ArrayList<Long> positions, long lastTicked, ResourceLocation groupId) {
        this.positions = positions;
        this.lastTick = lastTicked;
        this.groupId = groupId;
    }
}
