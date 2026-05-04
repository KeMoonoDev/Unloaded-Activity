package lol.zanspace.unloadedactivity;

import lol.zanspace.unloadedactivity.datapack.GroupMemberInfo;
import lol.zanspace.unloadedactivity.datapack.SimulateProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

public class ActiveGroupSimulateData {
    // Simulation data that affects this data.
    public ArrayList<ActiveGroupSimulateData> surroundingData;
    public boolean isActive;
    public int groupIndex;
    public BlockPos position;
    public BlockState blockState;
    public Optional<SimulateProperty> simulateProperty;
    public GroupMemberInfo groupMemberInfo;

    public ActiveGroupSimulateData(BlockPos position, BlockState blockState, Optional<SimulateProperty> simulateProperty, GroupMemberInfo groupMemberInfo) {
        this.surroundingData = new ArrayList<>();
        this.position = position;
        this.blockState = blockState;
        this.simulateProperty = simulateProperty;
        this.groupMemberInfo = groupMemberInfo;
        this.groupIndex = -1;

        this.isActive = false;
    }
}
