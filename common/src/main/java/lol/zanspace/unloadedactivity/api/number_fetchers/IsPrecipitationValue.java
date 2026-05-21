package lol.zanspace.unloadedactivity.api.number_fetchers;

import lol.zanspace.unloadedactivity.api.NumberFetcher;
import lol.zanspace.unloadedactivity.datapack.ValueContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

public class IsPrecipitationValue implements NumberFetcher {
    private Biome.Precipitation precipitation;

    public IsPrecipitationValue(Biome.Precipitation precipitation) {
        this.precipitation = precipitation;
    }

    @Override
    public Number evaluate(ValueContext context) {
        // We get the biome from above if it's air because when Minecraft handles precipitation
        // it takes the biome from the top block and uses it for the bottom block.
        BlockPos samplePos = context.state.isAir() ? context.pos : context.pos.above();
        Biome biome = context.level.getBiome(samplePos).value();
        #if MC_VER >= MC_1_21_3
        Biome.Precipitation precipitation = biome.getPrecipitationAt(context.pos, context.level.getSeaLevel());
        #elif MC_VER >= MC_1_19_4
        Biome.Precipitation precipitation = biome.getPrecipitationAt(context.pos);
        #else
        Biome.Precipitation precipitation = biome.getPrecipitation()
        #endif;
        boolean isPrecipitation = precipitation == this.precipitation;
        return isPrecipitation ? 1 : 0;
    }

    @Override
    public boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    public boolean canBeAffectedByTime() {
        return false;
    }

    @Override
    public boolean isRandom() {
        return false;
    }

    @Override
    public long getNextValueSwitchDuration(ValueContext context) {
        return Long.MAX_VALUE;
    }
}
