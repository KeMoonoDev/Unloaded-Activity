package dev.moono.unloadedactivity.api.value_expression;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.api.context.RandomizedContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class RandomizedValueExpression<T> {
    public final ValueExpression<T> inner;

    public final boolean canBeAffectedByWeather;
    public final boolean canBeAffectedByTime;

    public RandomizedValueExpression(ValueExpression<T> value) {
        this.inner = value;
        this.canBeAffectedByWeather = value.canBeAffectedByWeather();
        this.canBeAffectedByTime = value.canBeAffectedByTime();
    }

    public T evaluate(RandomizedContext context) {
        return inner.evaluate((ExpressionContext) context);
    }

    public T evaluateRandomized(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.evaluate(ExpressionContext.randomized(level, state, pos, simulatedTime, numberMap, activeGroupSimulateData));
    }

    public T evaluateRandomized(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap) {
        return inner.evaluate(ExpressionContext.randomized(level, state, pos, simulatedTime, numberMap, null));
    }

    public T evaluateRandomized(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.evaluate(ExpressionContext.randomized(level, state, pos, simulatedTime, Map.of(), activeGroupSimulateData));
    }

    public T evaluateRandomized(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime) {
        return inner.evaluate(ExpressionContext.randomized(level, state, pos, simulatedTime, Map.of(), null));
    }
}
