package dev.moono.unloadedactivity.impl.number_fetchers;

#if MC_VER < MC_1_21_1
import dev.moono.unloadedactivity.mixin.CropBlockInvoker;
#endif

import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class GrowthSpeedValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(FixedContext context) {
        BlockState state = context.getBlockState();
        LevelReader level = context.getLevel();
        BlockPos pos = context.getBlockPos();
        #if MC_VER >= MC_1_21_1
        return UnloadedActivity.platform.getGrowthSpeed(state, level, pos);
        #else
        return CropBlockInvoker.invokeGetGrowthSpeed(state.getBlock(), level, pos);
        #endif
    }
}
