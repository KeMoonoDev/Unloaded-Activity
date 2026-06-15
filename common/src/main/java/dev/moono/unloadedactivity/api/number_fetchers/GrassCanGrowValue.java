package dev.moono.unloadedactivity.api.number_fetchers;

#if MC_VER >= MC_26_1_2
import net.minecraft.world.level.block.SpreadingSnowyBlock;
#else
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
#endif

import dev.moono.unloadedactivity.api.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelReader;

public class GrassCanGrowValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(LevelReader level, BlockState state, BlockPos pos) {
        #if MC_VER >= MC_26_1_2
        return SpreadingSnowyBlock.canPropagate(state, level, pos) ? 1 : 0;
        #else
        return SpreadingSnowyDirtBlock.canPropagate(state, level, pos) ? 1 : 0;
        #endif
    }
}
