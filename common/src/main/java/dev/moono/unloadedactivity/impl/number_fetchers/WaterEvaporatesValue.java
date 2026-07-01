package dev.moono.unloadedactivity.impl.number_fetchers;

#if MC_VER >= MC_1_21_11
import net.minecraft.world.attribute.EnvironmentAttributes;
#endif

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;

public class WaterEvaporatesValue implements FixedNumberFetcher {
    @Override
    public Number evaluate(FixedContext context) {
        boolean ultraWarm = #if MC_VER >= MC_1_21_11
            context.getLevel().environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, context.getBlockPos())
        #else
            context.getLevel().dimensionType().ultraWarm()
        #endif;

        return ultraWarm ? 1 : 0;
    }
}
