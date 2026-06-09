package dev.moono.unloadedactivity.api.number_fetchers;

import dev.moono.unloadedactivity.api.NumberFetcher;
import dev.moono.unloadedactivity.datapack.ValueContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;

public class ShouldFreezeValue implements NumberFetcher {
    @Override
    public Number evaluate(ValueContext context) {
        // Do the same checks as Biome.shouldFreeze, but do the cheap conditions first
        // and skip conditions which are supposed to be handled by the user. (Like checking if the state is water or if it's surrounded by water)
        #if MC_VER >= MC_1_21_3
        if (!context.level.isInsideBuildHeight(context.pos.getY())) {
            return 0;
        }
        #else
        if (context.level.isOutsideBuildHeight(context.pos.getY())) {
            return 0;
        }
        #endif

        if (context.level.getBrightness(LightLayer.BLOCK, context.pos) >= 10) {
            return 0;
        }
        // We get the biome from above if it's air because when Minecraft handles precipitation
        // it takes the biome from the top block and uses it for the bottom block.
        BlockPos samplePos = context.state.isAir() ? context.pos : context.pos.above();
        Biome biome = context.level.getBiome(samplePos).value();

        if (biome.warmEnoughToRain(context.pos, context.level.getSeaLevel())) {
            return 0;
        }

        return 1;
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
