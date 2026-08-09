package dev.moono.unloadedactivity.impl;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.context.RandomizedContext;
import dev.moono.unloadedactivity.api.value_expression.RandomizedValueExpression;
import dev.moono.unloadedactivity.api.value_expression.ValueExpression;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationData;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationDataResource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public abstract class SimulationUtils {
    public static BlockState applySetProperties(BlockState state, RandomizedContext context, Map<String, RandomizedValueExpression<Number>>  setProperties) {
        BlockState newState = state;
        for (var entry : setProperties.entrySet()) {
            String propertyName = entry.getKey();
            RandomizedValueExpression<Number> propertyValue = entry.getValue();

            Optional<Property<?>> maybeProperty = GameUtils.getProperty(newState, propertyName);
            if (maybeProperty.isEmpty()) continue;

            Property<?> property = maybeProperty.get();

            if (property instanceof BooleanProperty booleanProperty) {
                float value = propertyValue.evaluate(context).floatValue();
                newState = newState.setValue(booleanProperty, value != 0);
            } else if (property instanceof IntegerProperty integerProperty) {
                int value = propertyValue.evaluate(context).intValue();
                newState = newState.setValue(integerProperty, value);
            }
        }
        return newState;
    }

    public static BlockState applySetNamedProperties(BlockState state, RandomizedContext context, Map<String, RandomizedValueExpression<String>>  setProperties) {
        BlockState newState = state;
        for (var entry : setProperties.entrySet()) {
            String propertyName = entry.getKey();
            RandomizedValueExpression<String> propertyValue = entry.getValue();

            Optional<Property<?>> maybeProperty = GameUtils.getProperty(newState, propertyName);
            if (maybeProperty.isEmpty()) continue;

            Property<?> property = maybeProperty.get();

            String value = propertyValue.evaluate(context);

            newState = setNamedPropertyValue(newState, property, value);
        }
        return newState;
    }

    public static<T extends Comparable<T>> BlockState setNamedPropertyValue(BlockState state, Property<T> property, String value) {
        for (T possibleValue : property.getPossibleValues()) {
            if (!possibleValue.toString().equals(value)) continue;

            return state.setValue(property, possibleValue);
        }

        return state;
    }

    public static boolean resultingBlocksMayNeedDuration(ValueExpression<Block> valueExpression) {
        return valueExpression.getPossibleValues().anyMatch(possibleBlock -> {
            Optional<SimulationData> maybeSimulationData = SimulationDataResource.getSimulationData(possibleBlock);
            if (maybeSimulationData.isEmpty()) return false;
            SimulationData simulationData = maybeSimulationData.get();

            return simulationData.hasRandTicksWithoutGroup || simulationData.hasPrecTicksWithoutGroup;
        });
    }

    public static <T> boolean anyNeedsDuration(Collection<RandomizedValueExpression<T>> collection) {
        for (RandomizedValueExpression<?> valueExpression : collection) {
            if (needsDuration(valueExpression)) return true;
        }
        return false;
    }

    public static boolean needsDuration(RandomizedValueExpression<?> valueExpression) {
        return valueExpression.canBeAffectedByTime || valueExpression.canBeAffectedByWeather;
    }
}
