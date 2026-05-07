package lol.zanspace.unloadedactivity.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface SimulateBlockEntity {
    default void unloadedactivity$simulateTime(long timeDifference)  {}
}
