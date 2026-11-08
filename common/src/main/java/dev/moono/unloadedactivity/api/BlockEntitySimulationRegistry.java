package dev.moono.unloadedactivity.api;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class BlockEntitySimulationRegistry {
    private final ArrayList<RegistryForType<?>> registriesForTypes = new ArrayList<>();

    public <T extends BlockEntity> void register(Class<T> clazz, EntitySimulation<T> simulation) {
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

    public List<EntitySimulation<BlockEntity>> getSimulations(Class<? extends BlockEntity> clazz) {
        ArrayList<EntitySimulation<BlockEntity>> simulations = new ArrayList<>();
        for (RegistryForType<?> registryForType : this.registriesForTypes) {
            if (registryForType.clazz.isAssignableFrom(clazz)) {
                @SuppressWarnings("unchecked") // This is safe because I already checked the clazz
                RegistryForType<BlockEntity> registryForTypeT = (RegistryForType<BlockEntity>) registryForType;
                simulations.addAll(registryForTypeT.simulations);
            }
        }
        return simulations;
    }

    private static class RegistryForType<T extends BlockEntity> {
        final Class<T> clazz;
        final ArrayList<EntitySimulation<T>> simulations = new ArrayList<>();

        RegistryForType(Class<T> clazz) {
            this.clazz = clazz;
        }
    }
}
