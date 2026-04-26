package lol.zanspace.unloadedactivity;

import lol.zanspace.unloadedactivity.datapack.GroupMemberInfo;
import lol.zanspace.unloadedactivity.datapack.SimulateProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class ActiveGroupSimulateData {
    // Simulation data that affects this data.
    public ArrayList<ActiveGroupSimulateData> surroundingData;
    public boolean isActive;
    public int groupIndex;
    public BlockPos position;
    public BlockState blockState;
    public SimulateProperty simulateProperty;
    public GroupMemberInfo groupMemberInfo;

    public ActiveGroupSimulateData(BlockPos position, BlockState blockState, SimulateProperty simulateProperty, GroupMemberInfo groupMemberInfo, boolean isActive) {
        this.surroundingData = new ArrayList<>();
        this.position = position;
        this.blockState = blockState;
        this.simulateProperty = simulateProperty;
        this.groupMemberInfo = groupMemberInfo;
        this.groupIndex = -1;
        this.isActive = isActive;
    }
}
