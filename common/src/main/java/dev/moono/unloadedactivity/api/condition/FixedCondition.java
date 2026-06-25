package dev.moono.unloadedactivity.api.condition;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.ExpressionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FixedCondition extends UpdatingCondition {
    public FixedCondition(Condition condition) {
        super(condition);

        if (this.canBeAffectedByWeather)
            throw new IllegalArgumentException("Provided ValueExpression can be affected by weather.");

        if (this.canBeAffectedByTime)
            throw new IllegalArgumentException("Provided ValueExpression can be affected by time.");
    }

    public boolean isValidFixed(ServerLevel level, BlockState state, BlockPos pos, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.isValid(ExpressionContext.fixed(level, state, pos, numberMap, activeGroupSimulateData));
    }

    public boolean isValidFixed(ServerLevel level, BlockState state, BlockPos pos, Map<String, Number> numberMap) {
        return inner.isValid(ExpressionContext.fixed(level, state, pos, numberMap, null));
    }

    public boolean isValidFixed(ServerLevel level, BlockState state, BlockPos pos, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.isValid(ExpressionContext.fixed(level, state, pos, Map.of(), activeGroupSimulateData));
    }

    public boolean isValidFixed(ServerLevel level, BlockState state, BlockPos pos) {
        return inner.isValid(ExpressionContext.fixed(level, state, pos, Map.of(), null));
    }
}
