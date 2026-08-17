package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.context.RandomizedContext;
import dev.moono.unloadedactivity.api.simulation_method.GroupableSimulationMethod;
import dev.moono.unloadedactivity.api.value_expression.RandomizedValueExpression;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationData;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationDataResource;
import dev.moono.unloadedactivity.impl.SimulationUtils;
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
    public final Map<String, RandomizedValueExpression<Number>> setProperties;
    public final Map<String, RandomizedValueExpression<String>> setNamedProperties;
    public final List<String> transferProperties;

    public @Nullable Boolean cachedShouldCalculateDuration;

    public ReplaceMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);

        this.setProperties = config.getRandomizedNumberExpressionMap("set_properties");
        this.setNamedProperties = config.getRandomizedStringExpressionMap("set_named_properties");
        this.dropsResources = config.getBooleanOrDefault("drops_resources", false);
        this.blockReplacement = config.getRandomizedBlockExpression("block_replacement");
        this.transferProperties = config.getStringList("transfer_properties");
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public boolean shouldCalculateDuration(BlockState state, ServerLevel level, BlockPos pos) {
        if (this.cachedShouldCalculateDuration == null) {
            this.cachedShouldCalculateDuration =
                SimulationUtils.resultingBlocksMayNeedDuration(this.blockReplacement.inner) ||
                SimulationUtils.anyNeedsDuration(this.setProperties.values()) ||
                SimulationUtils.anyNeedsDuration(this.setNamedProperties.values());
        }
        return this.cachedShouldCalculateDuration || super.shouldCalculateDuration(state, level, pos);
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

        RandomizedContext context = RandomizedContext.of(level, state, pos, finishTime, groupSimulateData);

        Block blockReplacement = this.blockReplacement.evaluate(context);
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
            if (maybeNewProperty.get().getPossibleValues().contains(oldValue)) {
                newState = newState.setValue((Property) maybeNewProperty.get(), (Comparable)oldValue);
            }
        }

        newState = SimulationUtils.applySetProperties(newState, context, this.setProperties);
        newState = SimulationUtils.applySetNamedProperties(newState, context, this.setNamedProperties);

        return new DeferredBlockPlacer.SingleBlockPlacement(newState, finishTime);
    }
}
