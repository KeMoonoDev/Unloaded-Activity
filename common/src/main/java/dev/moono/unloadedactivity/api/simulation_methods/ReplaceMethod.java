package dev.moono.unloadedactivity.api.simulation_methods;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.value_expression_containers.RandomizedValueExpression;
import dev.moono.unloadedactivity.datapack.SimulationData;
import dev.moono.unloadedactivity.datapack.SimulationDataResource;
import dev.moono.unloadedactivity.datapack.ExpressionContext;
import dev.moono.unloadedactivity.datapack.ValueExpression;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReplaceMethod extends GroupableSimulationMethod {
    public final boolean dropsResources;
    public final RandomizedValueExpression<Block> blockReplacement;
    public final Map<String, RandomizedValueExpression<Number>> setProperties;
    public final List<String> transferProperties;

    public final boolean cachedShouldCalculateDuration;

    public ReplaceMethod(SimulationConfig config) {
        super(config);
        this.dropsResources = config.getBooleanOrDefault("drops_resources", false);
        this.blockReplacement = config.getRandomizedBlockExpression("block_replacement");
        this.setProperties = config.getRandomizedNumberExpressionMap("set_properties");
        this.transferProperties = config.getStringList("transfer_properties");

        boolean replacementCanSimulate = this.blockReplacement.inner.getPossibleValues().anyMatch(block -> {
            Optional<SimulationData> maybeSimulationData = SimulationDataResource.getSimulationData(block);
            if (maybeSimulationData.isEmpty()) return false;
            SimulationData simulationData = maybeSimulationData.get();

            return simulationData.hasRandTicksWithoutGroup || simulationData.hasPrecTicksWithoutGroup;
        });

        if (replacementCanSimulate) {
            this.cachedShouldCalculateDuration = true;
        } else {
            this.cachedShouldCalculateDuration = this.setProperties.values().stream().anyMatch(valueExpression ->
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
    public DeferredBlockPlacer.SingleBlockPlacement getNewBlockState(BlockState state, ServerLevel level, BlockPos pos, int occurrences, long simulationDuration, long timePassed, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (this.dropsResources) {
            Block.dropResources(state, level, pos);
        }

        long currentTime = GameUtils.getTime(level);

        long finishTime = currentTime - timePassed + simulationDuration;

        Block blockReplacement = this.blockReplacement.evaluateRandomized(level, state, pos, finishTime, Map.of(), groupSimulateData);
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
        for (var entry : this.setProperties.entrySet()) {
            String propertyName = entry.getKey();
            RandomizedValueExpression<Number> propertyValue = entry.getValue();
            Optional<Property<?>> maybeSetProperty = GameUtils.getProperty(newState, propertyName);
            if (maybeSetProperty.isPresent()) {
                Property<?> newSetProperty = maybeSetProperty.get();
                if (newSetProperty instanceof BooleanProperty booleanProperty) {
                    float value = propertyValue.evaluateRandomized(level, newState, pos, finishTime).floatValue();
                    newState = newState.setValue(booleanProperty, value != 0);
                }
                if (newSetProperty instanceof IntegerProperty integerProperty) {
                    int value = propertyValue.evaluateRandomized(level, newState, pos, finishTime).intValue();
                    newState = newState.setValue(integerProperty, value);
                }
            }
        }
        return new DeferredBlockPlacer.SingleBlockPlacement(newState, simulationDuration);
    }
}
