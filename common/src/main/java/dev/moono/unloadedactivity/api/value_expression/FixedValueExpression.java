package dev.moono.unloadedactivity.api.value_expression;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.api.context.FixedContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FixedValueExpression<T> extends UpdatingValueExpression<T> {
    public FixedValueExpression(ValueExpression<T> value) {
        super(value);
        if (this.canBeAffectedByWeather)
            throw new IllegalArgumentException("Provided ValueExpression can be affected by weather.");

        if (this.canBeAffectedByTime)
            throw new IllegalArgumentException("Provided ValueExpression can be affected by time.");
    }

    public T evaluate(FixedContext context) {
        return inner.evaluate((ExpressionContext) context);
    }

    public T evaluateFixed(ServerLevel level, BlockState state, BlockPos pos, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.evaluate(ExpressionContext.fixed(level, state, pos, numberMap, activeGroupSimulateData));
    }

    public T evaluateFixed(ServerLevel level, BlockState state, BlockPos pos, Map<String, Number> numberMap) {
        return inner.evaluate(ExpressionContext.fixed(level, state, pos, numberMap, null));
    }

    public T evaluateFixed(ServerLevel level, BlockState state, BlockPos pos, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.evaluate(ExpressionContext.fixed(level, state, pos, Map.of(), activeGroupSimulateData));
    }

    public T evaluateFixed(ServerLevel level, BlockState state, BlockPos pos) {
        return inner.evaluate(ExpressionContext.fixed(level, state, pos, Map.of(), null));
    }
}
