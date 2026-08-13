package dev.moono.unloadedactivity.impl.number_fetchers.farmersdelight;

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import vectorwing.farmersdelight.common.Configuration;
import vectorwing.farmersdelight.common.registry.ModBlocks;
import vectorwing.farmersdelight.common.tag.ModTags;

import java.util.function.Supplier;

@SuppressWarnings("deprecation")
public class CanClimbValue implements FixedNumberFetcher {
    final Vec3i offset;
    final Supplier<Boolean> useTagsSupplier;
    final Supplier<Block> ropeBlockSupplier;

    public CanClimbValue() {
        this(new Vec3i(0, 1, 0));
    }

    public CanClimbValue(Vec3i offset) {
        this.offset = offset;
        try {
            @SuppressWarnings("unchecked") // It's either a Supplier<Boolean> or ForgeConfigSpec.BooleanValue which implements Supplier<Boolean>
            Supplier<Boolean> booleanSupplier = (Supplier<Boolean>) Configuration.class.getField("ENABLE_TOMATO_VINE_CLIMBING_TAGGED_ROPES").get(null);
            this.useTagsSupplier = booleanSupplier;
            @SuppressWarnings("unchecked") // Same deal with booleanSupplier
            Supplier<Block> blockSupplier = (Supplier<Block>) ModBlocks.class.getField("ROPE").get(null);
            this.ropeBlockSupplier = blockSupplier;
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public Number evaluate(FixedContext context) {
        BlockState targetState;
        if (offset.equals(Vec3i.ZERO)) {
            targetState = context.getBlockState();
        } else {
            targetState = context.getLevel().getBlockState(context.getBlockPos().offset(offset));
        }

        try {
            boolean useTags = useTagsSupplier.get();

            if (useTags) return targetState.is(ModTags.ROPES) ? 1 : 0;

            return targetState.is(ropeBlockSupplier.get()) ? 1 : 0;
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
