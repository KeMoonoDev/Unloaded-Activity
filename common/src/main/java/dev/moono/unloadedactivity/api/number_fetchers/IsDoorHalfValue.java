package dev.moono.unloadedactivity.api.number_fetchers;

import dev.moono.unloadedactivity.api.NumberFetcher;
import dev.moono.unloadedactivity.datapack.ValueContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class IsDoorHalfValue implements NumberFetcher {

    DoubleBlockHalf doorHalf;

    public IsDoorHalfValue(DoubleBlockHalf doorHalf) {
        this.doorHalf = doorHalf;
    }

    @Override
    public Number evaluate(ValueContext context) {
        boolean result = false;
        if (context.state.getBlock() instanceof DoorBlock) {
            result = context.state.getValue(DoorBlock.HALF) == this.doorHalf;
        }
        return result ? 1 : 0;
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
