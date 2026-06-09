package dev.moono.unloadedactivity.api.number_fetchers;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.NumberFetcher;
import dev.moono.unloadedactivity.datapack.ValueContext;
import net.minecraft.core.Direction;

public class AvailableSpaceForGourdValue implements NumberFetcher {
    @Override
    public Number evaluate(ValueContext context) {
        return (GameUtils.isValidGourdPosition(Direction.NORTH, context.pos, context.state, context.level) ? 1 : 0)
            + (GameUtils.isValidGourdPosition(Direction.EAST, context.pos, context.state, context.level) ? 1 : 0)
            + (GameUtils.isValidGourdPosition(Direction.SOUTH, context.pos, context.state, context.level) ? 1 : 0)
            + (GameUtils.isValidGourdPosition(Direction.WEST, context.pos, context.state, context.level) ? 1 : 0);
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
