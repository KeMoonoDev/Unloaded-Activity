package dev.moono.unloadedactivity.registrations;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.NumberFetcherRegistry;
import dev.moono.unloadedactivity.api.SimulationMethodRegistry;
import dev.moono.unloadedactivity.api.UnloadedActivityApi;
import dev.moono.unloadedactivity.impl.simulation_methods.nomansland.TapMethod;
import dev.moono.unloadedactivity.impl.simulation_methods.supplementaries.GrowFlaxMethod;

public class NoMansLandRegistrations implements UnloadedActivityApi {
    @Override
    public void registerNumberFetchers(NumberFetcherRegistry registry) {

    }

    @Override
    public void registerSimulationMethods(SimulationMethodRegistry registry) {
        registry.register(GameUtils.createId("nomansland", "tap"), TapMethod::new);
    }
}
