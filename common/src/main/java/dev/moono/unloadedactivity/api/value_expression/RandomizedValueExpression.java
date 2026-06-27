package dev.moono.unloadedactivity.api.value_expression;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
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

    public T evaluateRandomized(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.evaluate(ExpressionContext.randomized(level, state, pos, currentTime, numberMap, activeGroupSimulateData));
    }

    public T evaluateRandomized(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap) {
        return inner.evaluate(ExpressionContext.randomized(level, state, pos, currentTime, numberMap, null));
    }

    public T evaluateRandomized(ServerLevel level, BlockState state, BlockPos pos, long currentTime, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.evaluate(ExpressionContext.randomized(level, state, pos, currentTime, Map.of(), activeGroupSimulateData));
    }

    public T evaluateRandomized(ServerLevel level, BlockState state, BlockPos pos, long currentTime) {
        return inner.evaluate(ExpressionContext.randomized(level, state, pos, currentTime, Map.of(), null));
    }
}
