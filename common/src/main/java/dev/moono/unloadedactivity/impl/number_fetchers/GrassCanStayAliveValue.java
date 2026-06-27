package dev.moono.unloadedactivity.impl.number_fetchers;

#if MC_VER >= MC_26_1_2
import net.minecraft.world.level.block.SpreadingSnowyBlock;
#else
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
#endif

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;

public class GrassCanStayAliveValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(FixedContext context) {
        #if MC_VER >= MC_26_1_2
        return SpreadingSnowyBlock
            .canStayAlive(context.getBlockState(), context.getLevel(), context.getBlockPos()) ? 1 : 0;
        #else
        return SpreadingSnowyDirtBlock
            .canBeGrass(context.getBlockState(), context.getLevel(), context.getBlockPos()) ? 1 : 0;
        #endif
    }
}
