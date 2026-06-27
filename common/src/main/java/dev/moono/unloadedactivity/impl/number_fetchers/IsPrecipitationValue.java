package dev.moono.unloadedactivity.impl.number_fetchers;

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class IsPrecipitationValue implements FixedNumberFetcher {
    private final Biome.Precipitation precipitation;

    public IsPrecipitationValue(Biome.Precipitation precipitation) {
        this.precipitation = precipitation;
    }

    @Override
    public Number evaluate(FixedContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getBlockPos();
        // We get the biome from above if it's air because when Minecraft handles precipitation
        // it takes the biome from the top block and uses it for the bottom block.
        BlockPos samplePos = context.getBlockState().isAir() ? pos : pos.above();
        Biome biome = level.getBiome(samplePos).value();
        #if MC_VER >= MC_1_21_3
        Biome.Precipitation precipitation = biome.getPrecipitationAt(pos, level.getSeaLevel());
        #elif MC_VER >= MC_1_19_4
        Biome.Precipitation precipitation = biome.getPrecipitationAt(pos);
        #else
        Biome.Precipitation precipitation = biome.getPrecipitation();
        #endif
        boolean isPrecipitation = precipitation == this.precipitation;
        return isPrecipitation ? 1 : 0;
    }
}
