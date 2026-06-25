package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.api.number_fetcher.RandomizedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class RandomValue implements RandomizedNumberFetcher {
    @Override
    public Number evaluate(LevelReader level, BlockState state, BlockPos pos, long currentSimulatedTime, boolean isRaining, RandomSource random) {
        return random.nextFloat();
    }
}
