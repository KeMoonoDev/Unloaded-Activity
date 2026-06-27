package dev.moono.unloadedactivity.impl.number_fetchers;

#if MC_VER >= MC_26_1_2
import net.minecraft.world.level.block.SpreadingSnowyBlock;
#else
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
#endif

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;

public class GrassCanGrowValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(FixedContext context) {
        return #if MC_VER >= MC_26_1_2 SpreadingSnowyBlock #else SpreadingSnowyDirtBlock #endif
            .canPropagate(context.getBlockState(), context.getLevel(), context.getBlockPos()) ? 1 : 0;
    }
}
