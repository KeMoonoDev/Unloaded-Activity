package dev.moono.unloadedactivity.api.simulation_methods;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.datapack.SimulationData;
import dev.moono.unloadedactivity.datapack.SimulationDataResource;
import dev.moono.unloadedactivity.datapack.ValueContext;
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
    public final ValueExpression<Block> blockReplacement;
    public Map<String, ValueExpression<Number>> setProperties;
    public List<String> transferProperties;

    public ReplaceMethod(SimulationConfig config) {
        super(config);
        this.dropsResources = config.getBooleanOrDefault("drops_resources", false);
        this.blockReplacement = config.getRandomizedBlockExpression("block_replacement");
        this.setProperties = config.getRandomizedNumberExpressionMap("set_properties");
        this.transferProperties = config.getStringList("transfer_properties");
    }

    @Override
    public boolean isFinished(BlockState state, ServerLevel level, BlockPos pos) {
        return false;
    }

    @Override
    public boolean shouldCalculateDuration(BlockState state, ServerLevel level, BlockPos pos) {
        Block blockReplacement = this.blockReplacement.evaluate(new ValueContext(level, state, pos));

        Optional<SimulationData> maybeSimulationData = SimulationDataResource.getSimulationData(blockReplacement);
        if (maybeSimulationData.isEmpty()) return false;
        SimulationData simulationData = maybeSimulationData.get();

        return simulationData.hasRandTicksWithoutGroup || simulationData.hasPrecTicksWithoutGroup;
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

        boolean isRaining = level.getWeatherForecast().getWeatherAtTime(finishTime);

        Block blockReplacement = this.blockReplacement.evaluate(new ValueContext(level, state, pos, finishTime, isRaining, false, groupSimulateData));
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
            ValueExpression<Number> propertyValue = entry.getValue();
            Optional<Property<?>> maybeSetProperty = GameUtils.getProperty(newState, propertyName);
            if (maybeSetProperty.isPresent()) {
                Property<?> newSetProperty = maybeSetProperty.get();
                if (newSetProperty instanceof BooleanProperty booleanProperty) {
                    float value = propertyValue.evaluate(new ValueContext(level, newState, pos)).floatValue();
                    newState = newState.setValue(booleanProperty, value != 0);
                }
                if (newSetProperty instanceof IntegerProperty integerProperty) {
                    int value = propertyValue.evaluate(new ValueContext(level, newState, pos)).intValue();
                    newState = newState.setValue(integerProperty, value);
                }
            }
        }
        return new DeferredBlockPlacer.SingleBlockPlacement(newState, simulationDuration);
    }
}
