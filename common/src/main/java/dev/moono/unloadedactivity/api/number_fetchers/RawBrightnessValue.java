package dev.moono.unloadedactivity.api.number_fetchers;

import dev.moono.unloadedactivity.api.FixedNumberFetcher;
import dev.moono.unloadedactivity.api.NumberFetcher;
import dev.moono.unloadedactivity.datapack.ExpressionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class RawBrightnessValue implements FixedNumberFetcher {
    Vec3i offset;

    public RawBrightnessValue() {
        this(Vec3i.ZERO);
    }

    public RawBrightnessValue(Vec3i offset) {
        this.offset = offset;
    }

    @Override
    public Number evaluate(LevelReader level, BlockState state, BlockPos pos) {
        return level.getRawBrightness(pos.offset(offset), 0);
    }
}
