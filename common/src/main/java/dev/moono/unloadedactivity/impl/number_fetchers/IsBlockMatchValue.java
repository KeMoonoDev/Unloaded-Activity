package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public class IsBlockMatchValue implements FixedNumberFetcher {
    private final Vec3i offset;
    private final Predicate<BlockState> isMatch;

    public IsBlockMatchValue(Predicate<BlockState> isMatch) {
        this(isMatch, Vec3i.ZERO);
    }

    public IsBlockMatchValue(Predicate<BlockState> isMatch, Vec3i offset) {
        this.isMatch = isMatch;
        this.offset = offset;
    }

    @Override
    public Number evaluate(FixedContext context) {
        BlockState targetState;
        if (offset.equals(Vec3i.ZERO)) {
            targetState = context.getBlockState();
        } else {
            targetState = context.getLevel().getBlockState(context.getBlockPos().offset(offset));
        }
        return this.isMatch.test(targetState) ? 1 : 0;
    }
}
