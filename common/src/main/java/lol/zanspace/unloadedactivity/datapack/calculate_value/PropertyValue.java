package lol.zanspace.unloadedactivity.datapack.calculate_value;

import lol.zanspace.unloadedactivity.datapack.CalculateValue;
import lol.zanspace.unloadedactivity.datapack.CalculationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;
import java.util.function.Function;

import static lol.zanspace.unloadedactivity.interfaces.SimulateChunkBlocks.getProperty;

public class PropertyValue implements CalculateValue<Number> {
    private String propertyName;

    public PropertyValue(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Number calculateValue(CalculationData data) {
        Optional<Property<?>> maybeProperty = getProperty(data.state, propertyName);
        if (maybeProperty.isEmpty())
            return Float.NaN;

        Property<?> property = maybeProperty.get();

        if (property instanceof IntegerProperty integerProperty) {
            return data.state.getValue(integerProperty);
        }

        if (property instanceof BooleanProperty booleanProperty) {
            return data.state.getValue(booleanProperty) ? 1 : 0;
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
    public long getNextValueSwitchDuration(CalculationData data) {
        return Long.MAX_VALUE;
    }

    @Override
    public CalculateValue<Number> replicate() {
        return this;
    }

    @Override
    public void replaceSuper(CalculateValue<Number> superValue) {

    }

    @Override
    public <U> CalculateValue<U> map(Function<Number, U> mapFunction) {
        throw new RuntimeException("Map function not supported on this type.");
    }
}
