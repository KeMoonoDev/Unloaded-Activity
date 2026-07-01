package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.GroupableSimulationMethod;
import dev.moono.unloadedactivity.api.value_expression.RandomizedValueExpression;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationData;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationDataResource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReplaceMethod extends GroupableSimulationMethod {
    public final boolean dropsResources;
    public final RandomizedValueExpression<Block> blockReplacement;
    public final List<Pair<Property<?>, RandomizedValueExpression<Number>>> setProperties;
    public final List<String> transferProperties;

    public final boolean cachedShouldCalculateDuration;

    public ReplaceMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, hasDependants);
        Map<String, RandomizedValueExpression<Number>> setPropertiesNames = config.getRandomizedNumberExpressionMap("set_properties");

        this.dropsResources = config.getBooleanOrDefault("drops_resources", false);
        this.blockReplacement = config.getRandomizedBlockExpression("block_replacement");
        this.transferProperties = config.getStringList("transfer_properties");

        ArrayList<Pair<Property<?>, RandomizedValueExpression<Number>>> convertedSetProperties = new ArrayList<>();
        for (var entry : setPropertiesNames.entrySet()) {
            String setPropertyName = entry.getKey();
            Optional<Property<?>> maybeSetProperty = GameUtils.getProperty(block.defaultBlockState(), setPropertyName);;

            if (maybeSetProperty.isEmpty())
                throw new RuntimeException("Block " + block + " does not have a property named " + setPropertyName);

            Property<?> setProperty = maybeSetProperty.get();

            if (!(setProperty instanceof IntegerProperty) && !(setProperty instanceof BooleanProperty))
                throw new RuntimeException("Block " + block + " has the property named " + setPropertyName + ", but the property isn't an IntegerProperty or BooleanProperty.");

            convertedSetProperties.add(Pair.of(setProperty, entry.getValue()));
        }

        // Make it immutable
        this.setProperties = List.copyOf(convertedSetProperties);

        boolean replacementCanSimulate = this.blockReplacement.inner.getPossibleValues().anyMatch(possibleBlock -> {
            Optional<SimulationData> maybeSimulationData = SimulationDataResource.getSimulationData(possibleBlock);
            if (maybeSimulationData.isEmpty()) return false;
            SimulationData simulationData = maybeSimulationData.get();

            return simulationData.hasRandTicksWithoutGroup || simulationData.hasPrecTicksWithoutGroup;
        });

        if (replacementCanSimulate) {
            this.cachedShouldCalculateDuration = true;
        } else {
            this.cachedShouldCalculateDuration = this.setProperties.stream().map(Pair::getValue).anyMatch(valueExpression ->
                valueExpression.canBeAffectedByTime || valueExpression.canBeAffectedByWeather
            );
        }
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public boolean shouldCalculateDuration(BlockState state, ServerLevel level, BlockPos pos) {
        return this.cachedShouldCalculateDuration;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        return 1;
    }

    @Override
    public DeferredBlockPlacer.SingleBlockPlacement getNewBlockState(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (this.dropsResources) {
            Block.dropResources(state, level, pos);
        }

        SimulatedTime finishTime = occurrencesAndTimings.getFinalTime();

        Block blockReplacement = this.blockReplacement.evaluateRandomized(level, state, pos, finishTime.currentTime(), Map.of(), groupSimulateData);
        BlockState newState = blockReplacement.defaultBlockState();
        for (String propertyName : this.transferProperties) {
            Optional<Property<?>> maybeNewProperty = GameUtils.getProperty(newState, propertyName);

            if (maybeNewProperty.isEmpty()) {
                continue;
            }

            Optional<Property<?>> maybeOldProperty = GameUtils.getProperty(state, propertyName);

            if (maybeOldProperty.isEmpty()) {
                continue;
            }

            // Sick and twisted workaround.
            Object oldValue = state.getValue(maybeOldProperty.get());
            Object valueFromNew = newState.getValue(maybeNewProperty.get());
            if (oldValue.getClass().isInstance(valueFromNew)) {
                newState = newState.setValue((Property) maybeNewProperty.get(), (Comparable)oldValue);
            }
        }
        for (var pair : this.setProperties) {
            Property<?> setProperty = pair.getLeft();
            RandomizedValueExpression<Number> propertyValue = pair.getRight();
            if (setProperty instanceof BooleanProperty booleanProperty) {
                float value = propertyValue.evaluateRandomized(level, newState, pos, finishTime.currentTime()).floatValue();
                newState = newState.setValue(booleanProperty, value != 0);
            } else if (setProperty instanceof IntegerProperty integerProperty) {
                int value = propertyValue.evaluateRandomized(level, newState, pos, finishTime.currentTime()).intValue();
                newState = newState.setValue(integerProperty, value);
            } else {
                throw new RuntimeException("Property should have been validated at this point.");
            }
        }
        return new DeferredBlockPlacer.SingleBlockPlacement(newState, finishTime);
    }
}
