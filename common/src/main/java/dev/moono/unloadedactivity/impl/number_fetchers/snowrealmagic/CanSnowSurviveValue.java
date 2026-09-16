package dev.moono.unloadedactivity.impl.number_fetchers.snowrealmagic;

#if MC_VER >= MC_1_20_1
import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import snownee.snow.Hooks;
import snownee.snow.SnowCommonConfig;

public class CanSnowSurviveValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(FixedContext context) {
        return Hooks.canSnowSurvive(#if MC_VER <= MC_1_20_1 Blocks.SNOW.defaultBlockState(), #endif context.getLevel(), context.getBlockPos()) ? 1 : 0;
    }
}
#else
public class CanSnowSurviveValue {}
#endif