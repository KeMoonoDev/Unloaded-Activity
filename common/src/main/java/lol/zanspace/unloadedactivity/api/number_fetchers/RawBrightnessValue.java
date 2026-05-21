package lol.zanspace.unloadedactivity.api.number_fetchers;

import lol.zanspace.unloadedactivity.api.NumberFetcher;
import lol.zanspace.unloadedactivity.datapack.ValueContext;
import net.minecraft.core.Vec3i;

public class RawBrightnessValue implements NumberFetcher {
    Vec3i offset;

    public RawBrightnessValue() {
        this(Vec3i.ZERO);
    }

    public RawBrightnessValue(Vec3i offset) {
        this.offset = offset;
    }

    @Override
    public Number evaluate(ValueContext context) {
        return context.level.getRawBrightness(context.pos.offset(offset), 0);
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
