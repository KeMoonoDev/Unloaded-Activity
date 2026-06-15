package dev.moono.unloadedactivity.api.number_fetchers;

import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.FixedNumberFetcher;
import dev.moono.unloadedactivity.api.NumberFetcher;
import dev.moono.unloadedactivity.datapack.ExpressionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class GrowthSpeedValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(LevelReader level, BlockState state, BlockPos pos) {
        #if MC_VER >= MC_1_21_1
        return UnloadedActivity.platform.getGrowthSpeed(state, level, pos);
        #else
        return CropBlockInvoker.invokeGetGrowthSpeed(state.getBlock(), level, pos);
        #endif
    }
}
