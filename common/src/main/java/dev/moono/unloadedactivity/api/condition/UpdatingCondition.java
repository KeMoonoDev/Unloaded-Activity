package dev.moono.unloadedactivity.api.condition;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
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

    public boolean isValidUpdating(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.isValid(ExpressionContext.updating(level, state, pos, currentTime, numberMap, activeGroupSimulateData));
    }

    public boolean isValidUpdating(ServerLevel level, BlockState state, BlockPos pos, long currentTime, Map<String, Number> numberMap) {
        return inner.isValid(ExpressionContext.updating(level, state, pos, currentTime, numberMap, null));
    }

    public boolean isValidUpdating(ServerLevel level, BlockState state, BlockPos pos, long currentTime, @Nullable ActiveGroupSimulateData activeGroupSimulateData) {
        return inner.isValid(ExpressionContext.updating(level, state, pos, currentTime, Map.of(), activeGroupSimulateData));
    }

    public boolean isValidUpdating(ServerLevel level, BlockState state, BlockPos pos, long currentTime) {
        return inner.isValid(ExpressionContext.updating(level, state, pos, currentTime, Map.of(), null));
    }
}
