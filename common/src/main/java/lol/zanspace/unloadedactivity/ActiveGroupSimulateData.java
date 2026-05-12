package lol.zanspace.unloadedactivity;

import lol.zanspace.unloadedactivity.datapack.GroupMemberInfo;
import lol.zanspace.unloadedactivity.datapack.SimulateProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

public class ActiveGroupSimulateData {
    // Simulation data that affects this data.
    public ArrayList<ActiveGroupSimulateData> surroundingData;
    public ArrayList<ActiveGroupSimulateData> extendingData;
    public boolean isActive;
    public int groupIndex;
    public BlockPos position;
    public BlockState blockState;
    public ServerLevel level;
    private Optional<SimulateProperty> simulateProperty;
    private GroupMemberInfo groupMemberInfo;

    public ActiveGroupSimulateData(BlockPos position, BlockState blockState, Optional<SimulateProperty> simulateProperty, GroupMemberInfo groupMemberInfo, ServerLevel level) {
        this.surroundingData = new ArrayList<>();
        this.extendingData = new ArrayList<>();
        this.position = position;
        this.blockState = blockState;
        this.level = level;
        this.groupMemberInfo = groupMemberInfo;
        this.groupIndex = -1;

        this.setSimulateProperty(simulateProperty);
    }

    public GroupMemberInfo getGroupMemberInfo() {
        return groupMemberInfo;
    }

    public void setGroupMemberInfo(GroupMemberInfo groupMemberInfo) {
        this.groupMemberInfo = groupMemberInfo;
    }

    public Optional<SimulateProperty> getSimulateProperty() {
        return simulateProperty;
    }

    public void setSimulateProperty(Optional<SimulateProperty> simulateProperty) {
        this.simulateProperty = simulateProperty;
        if (this.simulateProperty.isPresent()) {
            SimulateProperty someSimulateProperty = this.simulateProperty.get();
            Block block = this.blockState.getBlock();
            this.isActive = block.canSimulateProperty(this.blockState, this.level, this.position, someSimulateProperty);
        } else {
            this.isActive = false;
        }
    }
}
