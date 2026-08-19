package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.GroupableSimulationMethod;
import dev.moono.unloadedactivity.api.value_expression.FixedValueExpression;
import dev.moono.unloadedactivity.impl.SimulationUtils;
import dev.moono.unloadedactivity.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class PropertyMethod extends GroupableSimulationMethod {
    public final PropertyHolder<?> propertyHolder;
    public final int updateType;
    public final boolean updateNeighbors;

    static class PropertyHolder<T extends Comparable<T>> {
        public final Property<@NotNull T> property;
        public final List<T> progressValues;

        public PropertyHolder(Property<@NotNull T> property, List<String> progressValueStrings) {
            this.property = property;

            if (progressValueStrings.isEmpty()) {
                if (!(this.property instanceof IntegerProperty) && !(this.property instanceof BooleanProperty))
                    throw new RuntimeException("Block has the property named " + property.getName() + ", but the property isn't an IntegerProperty or BooleanProperty. To use this property, make sure \"progress_values\" is a non empty array.");
                this.progressValues = List.of();
            } else {
                if (progressValueStrings.size() != new HashSet<>(progressValueStrings).size()) {
                    throw new RuntimeException("Duplicate values found in \"progress_values\". Please make sure there are no duplicates.");
                }

                ArrayList<T> newList = new ArrayList<>();

                for (String valueName : progressValueStrings) {
                    T foundValue = null;
                    for (T possibleValue : this.property.getPossibleValues()) {
                        if (!possibleValue.toString().equals(valueName)) continue;

                        foundValue = possibleValue;
                        break;
                    }

                    if (foundValue == null) {
                        throw new RuntimeException("Value \""+valueName+"\" is not a valid value in property.");
                    }

                    newList.add(foundValue);
                }

                this.progressValues = newList;
            }
        }

        public boolean isInValidState(BlockState state) {
            if (progressValues.isEmpty()) return true;
            return progressValues.contains(state.getValue(property));
        }

        public int getMax() {
            if (!progressValues.isEmpty()) {
                return progressValues.size() - 1;
            } else {
                if (property instanceof IntegerProperty integerProperty) {
                    return ((IntegerPropertyAccessor)integerProperty).unloaded_activity$getMax();
                } else if (property instanceof BooleanProperty) {
                    return 1;
                } else {
                    throw new RuntimeException("Property should have been validated at this point.");
                }
            }
        }

        public int getCurrent(BlockState state) {
            if (!progressValues.isEmpty()) {
                return progressValues.indexOf(state.getValue(property));
            } else {
                if (property instanceof IntegerProperty integerProperty) {
                    return state.getValue(integerProperty);
                } else if (property instanceof BooleanProperty booleanProperty) {
                    return state.getValue(booleanProperty) ? 1 : 0;
                } else {
                    throw new RuntimeException("Property should have been validated at this point.");
                }
            }
        }

        public BlockState set(BlockState state, int value) {
            if (!progressValues.isEmpty()) {
                return state.setValue(property, progressValues.get(value));
            } else {
                if (property instanceof IntegerProperty integerProperty) {
                    return state.setValue(integerProperty, value);
                } else if (property instanceof BooleanProperty booleanProperty) {
                    return state.setValue(booleanProperty, value > 0);
                } else {
                    return state;
                }
            }
        }
    }

    @Override
    public boolean hasValidConditions(BlockState state, ServerLevel level, BlockPos pos) {
        return this.propertyHolder.isInValidState(state) && super.hasValidConditions(state, level, pos);
    }

    @Nullable public final FixedValueExpression<Number> maxValue;

    public PropertyMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);
        String propertyName = config.getString("property_name");

        this.updateType = config.getNumberOrDefault("update_type", Block.UPDATE_ALL).intValue();
        this.updateNeighbors = config.getBooleanOrDefault("update_neighbors", false);

        this.maxValue = config.getFixedNumberExpressionNullable("max_value");

        Optional<Property<?>> maybeProperty = GameUtils.getProperty(block.defaultBlockState(), propertyName);;

        if (maybeProperty.isEmpty())
            throw new RuntimeException("Block " + block + " does not have a property named " + propertyName);

        List<String> progressValues = config.getStringList("progress_values");

        this.propertyHolder = new PropertyHolder<>(maybeProperty.get(), progressValues);
    }

    @Override
    public boolean isDependable() {
        return true;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        int propertyMax = this.propertyHolder.getMax();
        int current = this.propertyHolder.getCurrent(state);

        int max;
        if (this.maxValue != null) {
            Number calculated = this.maxValue.evaluateFixed(level, state, pos);
            max = Math.min(propertyMax, calculated.intValue());
        } else {
             max = propertyMax;
        }

        return Math.max(0, max - current);
    }

    @Override
    public DeferredBlockPlacer.SingleBlockPlacement getNewBlockState(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings, @Nullable ActiveGroupSimulateData groupSimulateData) {
        int current = this.propertyHolder.getCurrent(state);

        int newPropertyValue = current + occurrencesAndTimings.occurrences();

        BlockState newState = this.propertyHolder.set(state, newPropertyValue);

        return new DeferredBlockPlacer.SingleBlockPlacement(newState, updateType, occurrencesAndTimings.getFinalTime());
    }
}
