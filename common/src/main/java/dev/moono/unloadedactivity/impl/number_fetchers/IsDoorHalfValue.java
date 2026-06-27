package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class IsDoorHalfValue implements FixedNumberFetcher {

    final DoubleBlockHalf doorHalf;

    public IsDoorHalfValue(DoubleBlockHalf doorHalf) {
        this.doorHalf = doorHalf;
    }

    @Override
    public Number evaluate(FixedContext context) {
        boolean result = false;
        BlockState state = context.getBlockState();
        if (state.getBlock() instanceof DoorBlock) {
            result = state.getValue(DoorBlock.HALF) == this.doorHalf;
        }
        return result ? 1 : 0;
    }
}
