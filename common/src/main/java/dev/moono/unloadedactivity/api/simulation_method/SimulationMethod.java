package dev.moono.unloadedactivity.api.simulation_method;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.condition.FixedCondition;
import dev.moono.unloadedactivity.api.value_expression.UpdatingValueExpression;
import dev.moono.unloadedactivity.api.condition.Condition;
import dev.moono.unloadedactivity.api.ExpressionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class SimulationMethod {
    public final boolean isPrecipitation;
    public final boolean requiresRain;

    public final UpdatingValueExpression<Number> advanceProbability;
    public final List<FixedCondition> conditions;
    public final List<String> dependencies;

    public SimulationMethod(SimulationConfig config) {
        this.isPrecipitation = config.getBooleanOrDefault("is_precipitation", false);
        this.requiresRain = config.getBooleanOrDefault("requires_rain", this.isPrecipitation);
        this.advanceProbability = config.getUpdatingNumberExpression("advance_probability");
        this.conditions = config.getFixedConditionList("conditions");
        this.dependencies = config.getStringList("dependencies");
    }

    public abstract boolean canDoMore(BlockState state, ServerLevel level, BlockPos pos);

    public abstract boolean isDependable();

    @Nullable
    public abstract DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, long timePassed, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData);

    public boolean simulatesWithGroup() {
        return false;
    }

    public boolean hasValidConditions(BlockState state, ServerLevel level, BlockPos pos) {
        ExpressionContext fixedContext = ExpressionContext.fixed(level, state, pos, Map.of(), null);
        for (FixedCondition condition : this.conditions) {
            if (!condition.inner.isValid(fixedContext)) {
                return false;
            }
        }

        return true;
    }

    public boolean canSimulate(BlockState state, ServerLevel level, BlockPos pos) {
        if (!canDoMore(state, level, pos))
            return false;

        return hasValidConditions(state, level, pos);
    }

    public boolean shouldCalculateDuration(BlockState state, ServerLevel level, BlockPos pos) {
        return false;
    }
}