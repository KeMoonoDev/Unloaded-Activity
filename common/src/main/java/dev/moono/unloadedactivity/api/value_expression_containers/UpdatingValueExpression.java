package dev.moono.unloadedactivity.api.value_expression_containers;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.datapack.ExpressionContext;
import dev.moono.unloadedactivity.datapack.ValueExpression;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class UpdatingValueExpression<T> extends RandomizedValueExpression<T> {
    public UpdatingValueExpression(ValueExpression<T> value) {
        super(value);
        if (value.isRandom()) {
            throw new IllegalArgumentException("Provided ValueExpression has a randomized result.");
        }
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
}
