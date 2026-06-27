package dev.moono.unloadedactivity.api.value_expression;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.api.context.UpdatingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class UpdatingValueExpression<T> extends RandomizedValueExpression<T> {
    public UpdatingValueExpression(ValueExpression<T> value) {
        super(value);
        if (value.isRandom())
            throw new IllegalArgumentException("Provided ValueExpression has a randomized result.");
    }

    public T evaluate(UpdatingContext context) {
        return inner.evaluate((ExpressionContext) context);
    }

    public T evaluateUpdating(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.evaluate(ExpressionContext.updating(level, state, pos, currentTime, numberMap, activeGroupSimulateData));
    }

    public T evaluateUpdating(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap) {
        return inner.evaluate(ExpressionContext.updating(level, state, pos, currentTime, numberMap, null));
    }

    public T evaluateUpdating(ServerLevel level, BlockState state, BlockPos pos, long currentTime, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.evaluate(ExpressionContext.updating(level, state, pos, currentTime, Map.of(), activeGroupSimulateData));
    }

    public T evaluateUpdating(ServerLevel level, BlockState state, BlockPos pos, long currentTime) {
        return inner.evaluate(ExpressionContext.updating(level, state, pos, currentTime, Map.of(), null));
    }

    public long getNextValueSwitchDuration(UpdatingContext context) {
        return inner.getNextValueSwitchDuration((ExpressionContext)context);
    }

    public long getNextValueSwitchDuration(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.getNextValueSwitchDuration(ExpressionContext.updating(level, state, pos, currentTime, numberMap, activeGroupSimulateData));
    }

    public long getNextValueSwitchDuration(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap) {
        return inner.getNextValueSwitchDuration(ExpressionContext.updating(level, state, pos, currentTime, numberMap, null));
    }

    public long getNextValueSwitchDuration(ServerLevel level, BlockState state, BlockPos pos, long currentTime, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.getNextValueSwitchDuration(ExpressionContext.updating(level, state, pos, currentTime, Map.of(), activeGroupSimulateData));
    }

    public long getNextValueSwitchDuration(ServerLevel level, BlockState state, BlockPos pos, long currentTime) {
        return inner.getNextValueSwitchDuration(ExpressionContext.updating(level, state, pos, currentTime, Map.of(), null));
    }
}
