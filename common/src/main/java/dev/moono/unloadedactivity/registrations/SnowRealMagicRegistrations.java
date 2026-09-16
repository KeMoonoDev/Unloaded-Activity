package dev.moono.unloadedactivity.registrations;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.NumberFetcherRegistry;
import dev.moono.unloadedactivity.api.SimulationMethodRegistry;
import dev.moono.unloadedactivity.api.UnloadedActivityApi;
import dev.moono.unloadedactivity.impl.number_fetchers.snowrealmagic.CanSnowSurviveValue;
import dev.moono.unloadedactivity.impl.number_fetchers.snowrealmagic.MaxSimulationLayersValue;
import dev.moono.unloadedactivity.impl.simulation_methods.snowrealmagic.SnowMethod;

public class SnowRealMagicRegistrations implements UnloadedActivityApi {
    @Override
    public void registerNumberFetchers(NumberFetcherRegistry registry) {
        #if MC_VER >= MC_1_20_1
        registry.register(GameUtils.createId("snowrealmagic", "max_simulation_layers"), new MaxSimulationLayersValue());
        registry.register(GameUtils.createId("snowrealmagic", "can_snow_survive"), new CanSnowSurviveValue());
        #endif
    }

    @Override
    public void registerSimulationMethods(SimulationMethodRegistry registry) {
        #if MC_VER >= MC_1_20_1
        registry.register(GameUtils.createId("snowrealmagic", "convert_to_snowy"), SnowMethod::new);
        #endif
    }
}
