package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class BlockBrightnessValue implements FixedNumberFetcher {
    final Vec3i offset;

    public BlockBrightnessValue() {
        this(Vec3i.ZERO);
    }

    public BlockBrightnessValue(Vec3i offset) {
        this.offset = offset;
    }

    @Override
    public Number evaluate(FixedContext context) {
        return context.getLevel().getBrightness(LightLayer.BLOCK, context.getBlockPos().offset(offset));
    }
}
