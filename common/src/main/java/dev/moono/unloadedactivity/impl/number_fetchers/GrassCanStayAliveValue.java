package dev.moono.unloadedactivity.impl.number_fetchers;

#if MC_VER >= MC_26_1_2
import net.minecraft.world.level.block.SpreadingSnowyBlock;
#else
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
#endif

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.world.level.block.state.BlockState;

public class GrassCanStayAliveValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(LevelReader level, BlockState state, BlockPos pos) {
        #if MC_VER >= MC_26_1_2
        return SpreadingSnowyBlock.canStayAlive(state, level, pos) ? 1 : 0;
        #else
        return SpreadingSnowyDirtBlock.canBeGrass(state, level, pos) ? 1 : 0;
        #endif
    }
}
