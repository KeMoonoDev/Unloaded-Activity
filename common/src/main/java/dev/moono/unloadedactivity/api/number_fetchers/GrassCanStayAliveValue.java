package dev.moono.unloadedactivity.api.number_fetchers;

#if MC_VER >= MC_26_1_2
import net.minecraft.world.level.block.SpreadingSnowyBlock;
#else
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
#endif

import dev.moono.unloadedactivity.api.NumberFetcher;
import dev.moono.unloadedactivity.datapack.ValueContext;

public class GrassCanStayAliveValue implements NumberFetcher {
    @Override
    public Number evaluate(ValueContext context) {
        #if MC_VER >= MC_26_1_2
        return SpreadingSnowyBlock.canStayAlive(context.state, context.level, context.pos) ? 1 : 0;
        #else
        return SpreadingSnowyDirtBlock.canBeGrass(context.state, context.level, context.pos) ? 1 : 0;
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
