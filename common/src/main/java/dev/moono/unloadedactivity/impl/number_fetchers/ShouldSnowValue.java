package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ShouldSnowValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(LevelReader level, BlockState state, BlockPos pos) {
        // Do the same checks as Biome.shouldSnow, but do the cheap checks first
        // and skip conditions which are supposed to be handled by the user. (Like checking if the state is air or snow)
        #if MC_VER >= MC_1_21_3
        if (!level.isInsideBuildHeight(pos.getY())) {
            return 0;
        }
        #else
        if (level.isOutsideBuildHeight(pos.getY())) {
            return 0;
        }
        #endif

        if (!Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
            return 0;
        }

        if (level.getBrightness(LightLayer.BLOCK, pos) >= 10) {
            return 0;
        }

        // We get the biome from above if it's air because when Minecraft handles precipitation
        // it takes the biome from the top block and uses it for the bottom block.
        BlockPos samplePos = state.isAir() ? pos : pos.above();
        Biome biome = level.getBiome(samplePos).value();
        #if MC_VER >= MC_1_21_3
        Biome.Precipitation precipitation = biome.getPrecipitationAt(pos, level.getSeaLevel());
        #elif MC_VER >= MC_1_19_4
        Biome.Precipitation precipitation = biome.getPrecipitationAt(pos);
        #else
        Biome.Precipitation precipitation = biome.getPrecipitation();
        #endif

        if (precipitation != Biome.Precipitation.SNOW) {
            return 0;
        }

        return 1;
    }
}
