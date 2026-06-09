package lol.zanspace.unloadedactivity.api.number_fetchers;

import lol.zanspace.unloadedactivity.GameUtils;
import lol.zanspace.unloadedactivity.api.NumberFetcher;
import lol.zanspace.unloadedactivity.datapack.ValueContext;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;


public class PropertyValue implements NumberFetcher {
    private String propertyName;

    public PropertyValue(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Number evaluate(ValueContext context) {
        Optional<Property<?>> maybeProperty = GameUtils.getProperty(context.state, propertyName);
        if (maybeProperty.isEmpty())
            return Float.NaN;

        Property<?> property = maybeProperty.get();

        if (property instanceof IntegerProperty integerProperty) {
            return context.state.getValue(integerProperty);
        }

        if (property instanceof BooleanProperty booleanProperty) {
            return context.state.getValue(booleanProperty) ? 1 : 0;
        }

        return Float.NaN;
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
