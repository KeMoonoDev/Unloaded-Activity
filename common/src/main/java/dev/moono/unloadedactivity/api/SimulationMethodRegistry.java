package dev.moono.unloadedactivity.api;

import net.minecraft.resources.*;

import java.util.HashMap;
import java.util.Optional;

public class SimulationMethodRegistry {
    private final HashMap<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, SimulationMethodConstructor> simulationMethods = new HashMap<>();

    public void register(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id, SimulationMethodConstructor constructor) {
        simulationMethods.put(id, constructor);
    }

    public Optional<SimulationMethodConstructor> get(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id) {
        return Optional.ofNullable(simulationMethods.get(id));
    }
}
