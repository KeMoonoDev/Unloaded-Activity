package dev.moono.unloadedactivity.registrations;

import com.google.gson.JsonElement;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.NumberFetcherRegistry;
import dev.moono.unloadedactivity.api.SimulationMethodRegistry;
import dev.moono.unloadedactivity.api.UnloadedActivityApi;
import dev.moono.unloadedactivity.impl.number_fetchers.LocalBrightnessValue;
import dev.moono.unloadedactivity.impl.number_fetchers.farmersdelight.CanClimbValue;
import vectorwing.farmersdelight.FarmersDelight;

public class FarmersDelightRegistrations implements UnloadedActivityApi {
    @Override
    public void registerNumberFetchers(NumberFetcherRegistry registry) {
        registry.register(GameUtils.createId(FarmersDelight.MODID, "can_climb"), data -> {
            JsonElement offsetUnparsed = data.get("offset");
            if (offsetUnparsed == null) return new CanClimbValue();
            return new CanClimbValue(GameUtils.parseOffset(offsetUnparsed));
        });
    }

    @Override
    public void registerSimulationMethods(SimulationMethodRegistry registry) {

    }
}
