package lol.zanspace.unloadedactivity.api.number_fetchers;

import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.api.NumberFetcher;
import lol.zanspace.unloadedactivity.datapack.ValueContext;

public class GrowthSpeedValue implements NumberFetcher {
    @Override
    public Number evaluate(ValueContext context) {
        #if MC_VER >= MC_1_21_1
        return UnloadedActivity.platform.getGrowthSpeed(context.state, context.level, context.pos);
        #else
        return CropBlockInvoker.invokeGetGrowthSpeed(context.state.getBlock(), context.level, context.pos);
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
