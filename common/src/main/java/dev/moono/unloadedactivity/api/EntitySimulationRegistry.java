package dev.moono.unloadedactivity.api;

import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class EntitySimulationRegistry {
    private final ArrayList<RegistryForType<?>> registriesForTypes = new ArrayList<>();

    public <T extends Entity> void register(Class<T> clazz, EntitySimulation<T> simulation) {
        for (RegistryForType<?> registryForType : this.registriesForTypes) {
            if (registryForType.clazz == clazz) {
                @SuppressWarnings("unchecked") // This is safe because I already checked the clazz
                RegistryForType<T> registryForTypeT = (RegistryForType<T>) registryForType;
                registryForTypeT.simulations.add(simulation);
                return;
            }
        }

        RegistryForType<T> registryForType = new RegistryForType<>(clazz);
        registryForType.simulations.add(simulation);
        this.registriesForTypes.add(registryForType);
    }

    public List<EntitySimulation<Entity>> getSimulations(Class<? extends Entity> clazz) {
        ArrayList<EntitySimulation<Entity>> simulations = new ArrayList<>();
        for (RegistryForType<?> registryForType : this.registriesForTypes) {
            if (registryForType.clazz.isAssignableFrom(clazz)) {
                @SuppressWarnings("unchecked") // This is safe because I already checked the clazz
                RegistryForType<Entity> registryForTypeT = (RegistryForType<Entity>) registryForType;
                simulations.addAll(registryForTypeT.simulations);
            }
        }
        return simulations;
    }

    private static class RegistryForType<T extends Entity> {
        final Class<T> clazz;
        final ArrayList<EntitySimulation<T>> simulations = new ArrayList<>();

        RegistryForType(Class<T> clazz) {
            this.clazz = clazz;
        }
    }
}
