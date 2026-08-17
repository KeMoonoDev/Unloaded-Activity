package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;


public class NamedPropertyValue implements FixedNumberFetcher {
    private final String propertyName;
    private final String valueEquals;

    public NamedPropertyValue(String propertyName, String valueEquals) {
        this.propertyName = propertyName;
        this.valueEquals = valueEquals;
    }

    @Override
    public Number evaluate(FixedContext context) {
        BlockState state = context.getBlockState();
        Optional<Property<?>> maybeProperty = GameUtils.getProperty(state, propertyName);
        if (maybeProperty.isEmpty())
            return Float.NaN;

        Property<?> property = maybeProperty.get();

        boolean isEqual = state.getValue(property).toString().equals(valueEquals);
        return isEqual ? 1 : 0;
    }
}
