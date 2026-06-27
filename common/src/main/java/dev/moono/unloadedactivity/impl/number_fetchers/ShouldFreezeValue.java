package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class ShouldFreezeValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(FixedContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getBlockPos();
        // Do the same checks as Biome.shouldFreeze, but do the cheap conditions first
        // and skip conditions which are supposed to be handled by the user. (Like checking if the state is water or if it's surrounded by water)
        #if MC_VER >= MC_1_21_3
        if (!level.isInsideBuildHeight(pos.getY())) {
            return 0;
        }
        #else
        if (level.isOutsideBuildHeight(pos.getY())) {
            return 0;
        }
        #endif

        if (level.getBrightness(LightLayer.BLOCK, pos) >= 10) {
            return 0;
        }
        // We get the biome from above if it's air because when Minecraft handles precipitation
        // it takes the biome from the top block and uses it for the bottom block.
        BlockPos samplePos = context.getBlockState().isAir() ? pos : pos.above();
        Biome biome = level.getBiome(samplePos).value();

        if (biome.warmEnoughToRain(pos #if MC_VER >= MC_1_21_3 , level.getSeaLevel() #endif)) {
            return 0;
        }

        return 1;
    }
}
