package dev.moono.unloadedactivity.registrations;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.NumberFetcherRegistry;
import dev.moono.unloadedactivity.api.SimulationMethodRegistry;
import dev.moono.unloadedactivity.api.UnloadedActivityApi;
import dev.moono.unloadedactivity.impl.number_fetchers.snowrealmagic.CanSnowSurviveValue;
import dev.moono.unloadedactivity.impl.number_fetchers.snowrealmagic.MaxSimulationLayersValue;
import dev.moono.unloadedactivity.impl.simulation_methods.snowrealmagic.SnowMethod;
import snownee.snow.SnowRealMagic;

public class SnowRealMagicRegistrations implements UnloadedActivityApi {
    @Override
    public void registerNumberFetchers(NumberFetcherRegistry registry) {
        registry.register(GameUtils.createId(SnowRealMagic.ID, "max_simulation_layers"), new MaxSimulationLayersValue());
        registry.register(GameUtils.createId(SnowRealMagic.ID, "can_snow_survive"), new CanSnowSurviveValue());
    }

    @Override
    public void registerSimulationMethods(SimulationMethodRegistry registry) {
        registry.register(GameUtils.createId(SnowRealMagic.ID, "convert_to_snowy"), SnowMethod::new);
    }
}
