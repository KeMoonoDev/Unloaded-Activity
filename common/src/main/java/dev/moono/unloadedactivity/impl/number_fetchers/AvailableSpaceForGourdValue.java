package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class AvailableSpaceForGourdValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(FixedContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getBlockPos();
        BlockState state = context.getBlockState();
        return (GameUtils.isValidGourdPosition(Direction.NORTH, pos, state, level) ? 1 : 0)
            + (GameUtils.isValidGourdPosition(Direction.EAST, pos, state, level) ? 1 : 0)
            + (GameUtils.isValidGourdPosition(Direction.SOUTH, pos, state, level) ? 1 : 0)
            + (GameUtils.isValidGourdPosition(Direction.WEST, pos, state, level) ? 1 : 0);
    }
}
