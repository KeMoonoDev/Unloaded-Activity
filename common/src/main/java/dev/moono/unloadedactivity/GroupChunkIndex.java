package dev.moono.unloadedactivity;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import dev.moono.unloadedactivity.api.SimulationMethod;
import dev.moono.unloadedactivity.api.simulation_methods.GroupableSimulationMethod;
import dev.moono.unloadedactivity.datapack.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GroupChunkIndex {
    private ArrayList<Long> positions;
    private long lastTick;
    public final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif groupId;

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

        Level level = chunk.getLevel();

        ServerLevel serverLevel;

        if (level instanceof ServerLevel serverLevelInstanceOf) {
            serverLevel = serverLevelInstanceOf;
        } else {
            throw new RuntimeException("Please run the function getAndFilterBlocks on the server side only pls thx.");
        }

        MinecraftServer server = serverLevel.getServer();

        ArrayList<ActiveGroupSimulateData> blockInfoList = new ArrayList<>(this.positions.size());

        this.positions.removeIf((longPos) -> {
            BlockPos pos = BlockPos.of(longPos);
            BlockState state = chunk.getBlockState(pos);
            Block block = state.getBlock();

            List<GroupMemberInfo> memberInfoList = GroupInfoResource.getBlockMemberInfo(block);
            Optional<GroupMemberInfo> maybeGroupMemberInfo = Optional.empty();

            for (var groupMemberInfo : memberInfoList) {
                if (groupMemberInfo.groupInfo.id.equals(groupId)) {
                    maybeGroupMemberInfo = Optional.of(groupMemberInfo);
                    break;
                }
            }

            if (maybeGroupMemberInfo.isEmpty())
                return true;

            if (UnloadedActivity.config.isBlockBlacklisted(state)) {
                return false;
            }

            GroupMemberInfo groupMemberInfo = maybeGroupMemberInfo.get();

            if (!groupMemberInfo.conditions.isEmpty()) {
                ExpressionContext context = ExpressionContext.fixed(serverLevel, state, pos, Map.of(), null);
                for (var condition : groupMemberInfo.conditions) {
                    if (!condition.isValid(context)) {
                        return false;
                    }
                }
            }

            Optional<GroupableSimulationMethod> method = Optional.empty();

            Optional<SimulationData> maybeSimulationData = SimulationDataResource.getSimulationData(block);
            if (maybeSimulationData.isPresent()) {
                SimulationData simulationData = maybeSimulationData.get();
                for (SimulationMethod simulationMethod : simulationData.methodMap.values()) {
                    if (!simulationMethod.simulatesWithGroup()) continue;
                    GroupableSimulationMethod groupableMethod = (GroupableSimulationMethod)simulationMethod;
                    if (this.groupId.equals(groupableMethod.simulateWithGroup)) {
                        method = Optional.of(groupableMethod);
                        break;
                    }
                }
            }

            blockInfoList.add(new ActiveGroupSimulateData(pos, state, method, groupMemberInfo, serverLevel));

            return false;
        });

        return blockInfoList;
    }

    public GroupChunkIndex(ArrayList<Long> positions, long lastTicked, #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif groupId) {
        this.positions = positions;
        this.lastTick = lastTicked;
        this.groupId = groupId;
    }
}
