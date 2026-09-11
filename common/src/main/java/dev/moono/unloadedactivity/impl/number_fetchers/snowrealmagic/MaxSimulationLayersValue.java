package dev.moono.unloadedactivity.impl.number_fetchers.snowrealmagic;

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.tags.BlockTags;
import snownee.snow.SnowCommonConfig;

public class MaxSimulationLayersValue implements FixedNumberFetcher {
    public static int maxSimulationLayers() {
        if (SnowCommonConfig.snowAccumulationDuringSnowfall) {
            boolean naturalMelt = SnowCommonConfig.snowNaturalMelt && !SnowCommonConfig.snowNeverMelt;
            if (naturalMelt) {
                return Math.min(SnowCommonConfig.snowAccumulationMaxLayers, 1);
            } else {
                return Math.min(SnowCommonConfig.snowAccumulationMaxLayers, 8);
            }
        }
        return Math.min(SnowCommonConfig.snowAccumulationMaxLayers, 1);
    }

    @Override
    public Number evaluate(FixedContext context) {
        int layers = maxSimulationLayers();
        if (layers == 8 && SnowCommonConfig.snowAccumulationMaxLayers == 8) {
            if (!context.getBlockState().is(BlockTags.SNOW)) {
                layers = 7;
            }
        }
        return layers;
    }
}
