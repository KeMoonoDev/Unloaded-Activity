package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class IsDoorHalfValue implements FixedNumberFetcher {

    DoubleBlockHalf doorHalf;

    public IsDoorHalfValue(DoubleBlockHalf doorHalf) {
        this.doorHalf = doorHalf;
    }

    @Override
    public Number evaluate(LevelReader level, BlockState state, BlockPos pos) {
        boolean result = false;
        if (state.getBlock() instanceof DoorBlock) {
            result = state.getValue(DoorBlock.HALF) == this.doorHalf;
        }
        return result ? 1 : 0;
    }
}
