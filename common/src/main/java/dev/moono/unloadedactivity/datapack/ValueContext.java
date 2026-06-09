package dev.moono.unloadedactivity.datapack;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ValueContext {
    public ServerLevel level;
    public BlockState state;
    public BlockPos pos;
    public long currentTime;
    public boolean isRaining;
    public boolean isThundering;
    @Nullable
    public ActiveGroupSimulateData activeGroupSimulateData;


    public ValueContext(ServerLevel level, BlockState state, BlockPos pos) {
        this(level, state, pos, -1, false, false, null);
    }

    public ValueContext(ServerLevel level, BlockState state, BlockPos pos, long currentTime) {
        this(level, state, pos, currentTime, false, false, null);
    }

    public ValueContext(ServerLevel level, BlockState state, BlockPos pos, long currentTime, boolean isRaining) {
        this(level, state, pos, currentTime, isRaining, false, null);
    }

    public ValueContext(ServerLevel level, BlockState state, BlockPos pos, long currentTime, boolean isRaining, boolean isThundering) {
        this(level, state, pos, currentTime, isRaining, isThundering, null);
    }

    public ValueContext(ServerLevel level, BlockState state, BlockPos pos, long currentTime, boolean isRaining, boolean isThundering, ActiveGroupSimulateData activeGroupSimulateData) {
        this.level = level;
        this.state = state;
        this.pos = pos;
        this.currentTime = currentTime;
        this.isRaining = isRaining;
        this.isThundering = isThundering;
        this.activeGroupSimulateData = activeGroupSimulateData;
    }
}
