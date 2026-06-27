package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;


public class PropertyValue implements FixedNumberFetcher {
    private final String propertyName;

    public PropertyValue(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Number evaluate(LevelReader level, BlockState state, BlockPos pos) {
        Optional<Property<?>> maybeProperty = GameUtils.getProperty(state, propertyName);
        if (maybeProperty.isEmpty())
            return Float.NaN;

        Property<?> property = maybeProperty.get();

        if (property instanceof IntegerProperty integerProperty) {
            return state.getValue(integerProperty);
        }

        if (property instanceof BooleanProperty booleanProperty) {
            return state.getValue(booleanProperty) ? 1 : 0;
        }

        return Float.NaN;
    }
}
