package dev.moono.unloadedactivity.api.number_fetchers;

import dev.moono.unloadedactivity.api.FixedNumberFetcher;
import dev.moono.unloadedactivity.api.NumberFetcher;
import dev.moono.unloadedactivity.datapack.ExpressionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public class IsBlockNeighborsMatchValue implements FixedNumberFetcher {
    private final Vec3i offset;
    private final Predicate<BlockState> isMatch;

    public IsBlockNeighborsMatchValue(Predicate<BlockState> isMatch) {
        this(isMatch, Vec3i.ZERO);
    }

    public IsBlockNeighborsMatchValue(Predicate<BlockState> isMatch, Vec3i offset) {
        this.isMatch = isMatch;
        this.offset = offset;
    }

    @Override
    public Number evaluate(LevelReader level, BlockState state, BlockPos pos) {
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState targetState = level.getBlockState(pos.offset(offset).relative(direction));
            if (this.isMatch.test(targetState)) return 1;
        }
        return 0;
    }
}
