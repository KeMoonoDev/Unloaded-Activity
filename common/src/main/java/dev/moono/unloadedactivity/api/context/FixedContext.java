package dev.moono.unloadedactivity.api.context;

#if MC_VER >= MC_1_21_11
import net.minecraft.world.level.gamerules.GameRules;
#else
import net.minecraft.world.level.GameRules;
#endif

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface FixedContext {
    LevelReader getLevel();
    BlockState getBlockState();
    BlockPos getBlockPos();
    Map<String, Number> getNumberMap();
    @Nullable ActiveGroupSimulateData getGroupSimulateData();
    GameRules getGameRules();
}
