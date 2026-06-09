package lol.zanspace.unloadedactivity.api;

import com.mojang.datafixers.util.Pair;
import lol.zanspace.unloadedactivity.ActiveGroupSimulateData;
import lol.zanspace.unloadedactivity.DeferredBlockPlacer;
import lol.zanspace.unloadedactivity.datapack.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class SimulationMethod {
    public final String target;
    public final boolean isPrecipitation;
    public final boolean requiresRain;
    public final boolean canBeAffectedByWeather;
    public final boolean canBeAffectedByTime;

    public final ValueExpression<Number> advanceProbability;
    public final List<Condition> conditions;
    public final List<String> dependencies;

    public SimulationMethod(SimulationConfig config) {
        this.target = config.getString("target");
        this.isPrecipitation = config.getBooleanOrDefault("is_precipitation", false);
        this.requiresRain = config.getBooleanOrDefault("requires_rain", this.isPrecipitation);
        this.advanceProbability = config.getUpdatingNumberExpression("advance_probability");
        this.conditions = config.getFixedConditionList("conditions");
        this.dependencies = config.getStringList("dependencies");

        this.canBeAffectedByWeather = this.advanceProbability.canBeAffectedByWeather();
        this.canBeAffectedByTime = this.advanceProbability.canBeAffectedByTime();
    }

    public abstract boolean isFinished(BlockState state, ServerLevel level, BlockPos pos);

    @Nullable
    public abstract DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, long timePassed, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData);

    public boolean simulatesWithGroup() {
        return false;
    }

    public boolean hasValidConditions(BlockState state, ServerLevel level, BlockPos pos) {
        ValueContext calculationData = new ValueContext(level, state, pos);

        for (Condition condition : this.conditions) {
            if (!condition.isValid(calculationData)) {
                return false;
            }
        }

        return true;
    }

    public boolean canSimulate(BlockState state, ServerLevel level, BlockPos pos) {
        if (isFinished(state, level, pos))
            return false;

        return hasValidConditions(state, level, pos);
    }

    public boolean shouldCalculateDuration(BlockState state, ServerLevel level, BlockPos pos) {
        return false;
    }
}