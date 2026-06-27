package dev.moono.unloadedactivity.api.context;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RandomizedContext extends UpdatingContext {
    public abstract RandomSource getRandomSource();
}
