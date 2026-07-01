package dev.moono.unloadedactivity.api;

import dev.moono.unloadedactivity.api.simulation_method.SimulationMethod;
import net.minecraft.world.level.block.Block;

@FunctionalInterface
public interface SimulationMethodConstructor {
    public SimulationMethod apply(SimulationConfig config, Block block, boolean hasDependants);
}
