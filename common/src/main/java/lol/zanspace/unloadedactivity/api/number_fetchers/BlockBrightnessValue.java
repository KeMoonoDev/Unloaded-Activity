package lol.zanspace.unloadedactivity.api.number_fetchers;

import lol.zanspace.unloadedactivity.api.NumberFetcher;
import lol.zanspace.unloadedactivity.datapack.ValueContext;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LightLayer;

public class BlockBrightnessValue implements NumberFetcher {
    Vec3i offset;

    public BlockBrightnessValue() {
        this(Vec3i.ZERO);
    }

    public BlockBrightnessValue(Vec3i offset) {
        this.offset = offset;
    }

    @Override
    public Number evaluate(ValueContext context) {
        return context.level.getBrightness(LightLayer.BLOCK, context.pos.offset(offset));
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
