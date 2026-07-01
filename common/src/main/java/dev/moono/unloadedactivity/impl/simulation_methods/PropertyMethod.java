package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.GroupableSimulationMethod;
import dev.moono.unloadedactivity.api.value_expression.FixedValueExpression;
import dev.moono.unloadedactivity.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PropertyMethod extends GroupableSimulationMethod {
    public final Property<?> property;
    public final int updateType;
    public final boolean updateNeighbors;

    @Nullable public final FixedValueExpression<Number> maxValue;

    public PropertyMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, hasDependants);
        String propertyName = config.getString("property_name");

        this.updateType = config.getNumberOrDefault("update_type", Block.UPDATE_ALL).intValue();
        this.updateNeighbors = config.getBooleanOrDefault("update_neighbors", false);

        this.maxValue = config.getFixedNumberExpressionNullable("max_value");

        Optional<Property<?>> maybeProperty = GameUtils.getProperty(block.defaultBlockState(), propertyName);;

        if (maybeProperty.isEmpty())
            throw new RuntimeException("Block " + block + " does not have a property named " + propertyName);

        this.property = maybeProperty.get();

        if (!(property instanceof IntegerProperty) && !(property instanceof BooleanProperty))
            throw new RuntimeException("Block " + block + " has the property named " + propertyName + ", but the property isn't an IntegerProperty or BooleanProperty.");
    }

    @Override
    public boolean isDependable() {
        return true;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        int propertyMax;
        int current;
        int updateCount;

        if (property instanceof IntegerProperty integerProperty) {
            propertyMax = ((IntegerPropertyAccessor)integerProperty).unloaded_activity$getMax();
            current = state.getValue(integerProperty);
        } else if (property instanceof BooleanProperty booleanProperty) {
            propertyMax = 1;
            current = state.getValue(booleanProperty) ? 1 : 0;
        } else {
            throw new RuntimeException("Property should have been validated at this point.");
        }

        int max;

        if (this.maxValue != null) {
            Number calculated = this.maxValue.evaluateFixed(level, state, pos);
            max = Math.min(propertyMax, calculated.intValue());
        } else {
             max = propertyMax;
        }

        updateCount = Math.max(0, max - current);

        return updateCount;
    }

    @Override
    public DeferredBlockPlacer.SingleBlockPlacement getNewBlockState(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings, @Nullable ActiveGroupSimulateData groupSimulateData) {
        int current;

        if (property instanceof IntegerProperty integerProperty) {
            current = state.getValue(integerProperty);
        } else if (property instanceof BooleanProperty booleanProperty) {
            current = state.getValue(booleanProperty) ? 1 : 0;
        } else {
            throw new RuntimeException("Property should have been validated at this point.");
        }

        int newPropertyValue = current + occurrencesAndTimings.occurrences();

        if (property instanceof IntegerProperty integerProperty) {
            state = state.setValue(integerProperty, newPropertyValue);
        } else if (property instanceof BooleanProperty booleanProperty) {
            state = state.setValue(booleanProperty, newPropertyValue > 0);
        }

        return new DeferredBlockPlacer.SingleBlockPlacement(state, updateType, occurrencesAndTimings.getFinalTime());
    }
}
