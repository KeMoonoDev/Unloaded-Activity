package dev.moono.unloadedactivity.api.condition;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.api.context.UpdatingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class UpdatingCondition extends RandomizedCondition {
    public UpdatingCondition(Condition condition) {
        super(condition);

        if (condition.isRandom())
            throw new IllegalArgumentException("Provided Condition has a randomized result.");
    }

    public boolean isValid(UpdatingContext context) {
        return inner.isValid((ExpressionContext)context);
    }

    public boolean isValidUpdating(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.isValid(ExpressionContext.updating(level, state, pos, simulatedTime, numberMap, activeGroupSimulateData));
    }

    public boolean isValidUpdating(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, Map<String, Number> numberMap) {
        return inner.isValid(ExpressionContext.updating(level, state, pos, simulatedTime, numberMap, null));
    }

    public boolean isValidUpdating(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.isValid(ExpressionContext.updating(level, state, pos, simulatedTime, Map.of(), activeGroupSimulateData));
    }

    public boolean isValidUpdating(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime) {
        return inner.isValid(ExpressionContext.updating(level, state, pos, simulatedTime, Map.of(), null));
    }
}
