package dev.moono.unloadedactivity.api.number_fetchers;

import dev.moono.unloadedactivity.api.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class BlockBrightnessValue implements FixedNumberFetcher {
    Vec3i offset;

    public BlockBrightnessValue() {
        this(Vec3i.ZERO);
    }

    public BlockBrightnessValue(Vec3i offset) {
        this.offset = offset;
    }

    @Override
    public Number evaluate(LevelReader level, BlockState state, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos.offset(offset));
    }
}
