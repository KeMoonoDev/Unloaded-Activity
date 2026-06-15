package dev.moono.unloadedactivity.api.simulation_methods;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.value_expression_containers.FixedValueExpression;
import dev.moono.unloadedactivity.datapack.ExpressionContext;
import dev.moono.unloadedactivity.datapack.ValueExpression;
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
    public int updateType;
    public boolean updateNeighbors;

    @Nullable public FixedValueExpression<Number> maxValue;

    public PropertyMethod(SimulationConfig config) {
        super(config);
        this.updateType = config.getNumberOrDefault("update_type", Block.UPDATE_ALL).intValue();
        this.updateNeighbors = config.getBooleanOrDefault("update_neighbors", false);

        this.maxValue = config.getFixedNumberExpressionNullable("max_value");
    }

    @Override
    public boolean isDependable() {
        return true;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        Optional<Property<?>> maybeProperty = GameUtils.getProperty(state, target);

        if (maybeProperty.isEmpty())
            return 0;

        Property<?> property = maybeProperty.get();

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
            return 0;
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
    public DeferredBlockPlacer.SingleBlockPlacement getNewBlockState(BlockState state, ServerLevel level, BlockPos pos, int occurrences, long simulationDuration, long timePassed, @Nullable ActiveGroupSimulateData groupSimulateData) {
        Optional<Property<?>> maybeProperty = GameUtils.getProperty(state, this.target);

        if (maybeProperty.isEmpty())
            return DeferredBlockPlacer.SingleBlockPlacement.empty();

        Property<?> property = maybeProperty.get();

        int current;

        if (property instanceof IntegerProperty integerProperty) {
            current = state.getValue(integerProperty);

        } else if (property instanceof BooleanProperty booleanProperty) {
            current = state.getValue(booleanProperty) ? 1 : 0;

        } else {
            return DeferredBlockPlacer.SingleBlockPlacement.empty();
        }

        int newPropertyValue = current + occurrences;

        if (property instanceof IntegerProperty integerProperty) {
            state = state.setValue(integerProperty, newPropertyValue);
        } else if (property instanceof BooleanProperty booleanProperty) {
            state = state.setValue(booleanProperty, newPropertyValue > 0);
        }
        if (property instanceof IntegerProperty integerProperty) {
            state = state.setValue(integerProperty, newPropertyValue);
        } else if (property instanceof BooleanProperty booleanProperty) {
            state = state.setValue(booleanProperty, newPropertyValue > 0);
        }

        return new DeferredBlockPlacer.SingleBlockPlacement(state, updateType, simulationDuration);
    }
}
