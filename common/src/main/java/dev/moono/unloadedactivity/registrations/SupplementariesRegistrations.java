package dev.moono.unloadedactivity.registrations;

import com.google.gson.JsonElement;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.NumberFetcherRegistry;
import dev.moono.unloadedactivity.api.SimulationMethodRegistry;
import dev.moono.unloadedactivity.api.UnloadedActivityApi;
import dev.moono.unloadedactivity.impl.number_fetchers.farmersdelight.CanClimbValue;
import dev.moono.unloadedactivity.impl.simulation_methods.supplementaries.GrowFlaxMethod;
import vectorwing.farmersdelight.FarmersDelight;

public class SupplementariesRegistrations implements UnloadedActivityApi {
    @Override
    public void registerNumberFetchers(NumberFetcherRegistry registry) {

    }

    @Override
    public void registerSimulationMethods(SimulationMethodRegistry registry) {
        registry.register(GameUtils.createId("supplementaries", "grow_flax"), GrowFlaxMethod::new);
    }
}
