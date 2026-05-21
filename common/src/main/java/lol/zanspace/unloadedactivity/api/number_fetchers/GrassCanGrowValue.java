package lol.zanspace.unloadedactivity.api.number_fetchers;

#if MC_VER >= MC_26_1_2
import net.minecraft.world.level.block.SpreadingSnowyBlock;
#else
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
#endif

import lol.zanspace.unloadedactivity.api.NumberFetcher;
import lol.zanspace.unloadedactivity.datapack.ValueContext;

public class GrassCanGrowValue implements NumberFetcher {
    @Override
    public Number evaluate(ValueContext context) {
        #if MC_VER >= MC_26_1_2
        return SpreadingSnowyBlock.canPropagate(context.state, context.level, context.pos) ? 1 : 0;
        #else
        return SpreadingSnowyDirtBlock.canPropagate(context.state, context.level, context.pos) ? 1 : 0;
        #endif
    }

    @Override
    public boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    public boolean canBeAffectedByTime() {
        return false;
    }

    @Override
    public boolean isRandom() {
        return false;
    }

    @Override
    public long getNextValueSwitchDuration(ValueContext context) {
        return Long.MAX_VALUE;
    }
}
