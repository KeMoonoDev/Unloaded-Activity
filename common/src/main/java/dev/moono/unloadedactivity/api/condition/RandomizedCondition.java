package dev.moono.unloadedactivity.api.condition;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.api.context.RandomizedContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class RandomizedCondition {
    public final Condition inner;

    public final boolean canBeAffectedByWeather;
    public final boolean canBeAffectedByTime;

    public RandomizedCondition(Condition condition) {
        this.inner = condition;

        this.canBeAffectedByWeather = condition.canBeAffectedByWeather();
        this.canBeAffectedByTime = condition.canBeAffectedByTime();
    }

    public boolean isValid(RandomizedContext context) {
        return inner.isValid((ExpressionContext)context);
    }

    public boolean isValidRandomized(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.isValid(ExpressionContext.randomized(level, state, pos, simulatedTime, numberMap, activeGroupSimulateData));
    }

    public boolean isValidRandomized(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap) {
        return inner.isValid(ExpressionContext.randomized(level, state, pos, simulatedTime, numberMap, null));
    }

    public boolean isValidRandomized(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.isValid(ExpressionContext.randomized(level, state, pos, simulatedTime, Map.of(), activeGroupSimulateData));
    }

    public boolean isValidRandomized(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime) {
        return inner.isValid(ExpressionContext.randomized(level, state, pos, simulatedTime, Map.of(), null));
    }
}
