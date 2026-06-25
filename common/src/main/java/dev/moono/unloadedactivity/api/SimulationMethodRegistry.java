package dev.moono.unloadedactivity.api;

import dev.moono.unloadedactivity.api.simulation_method.SimulationMethod;
import net.minecraft.resources.*;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

public class SimulationMethodRegistry {
    private final HashMap<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, Function<SimulationConfig, SimulationMethod>> simulationMethods = new HashMap<>();

    public void register(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id, Function<SimulationConfig, SimulationMethod> construct) {
        simulationMethods.put(id, construct);
    }

    public Optional<Function<SimulationConfig, SimulationMethod>> get(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id) {
        return Optional.ofNullable(simulationMethods.get(id));
    };
}
