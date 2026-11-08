package dev.moono.unloadedactivity.interfaces;

// This is just to add a simulation method that lives inside the class itself.
// For mods that have their own (Block)Entities, this interface should not be used for simulations.
public interface SimulateEntityHelper {
    void unloadedactivity$simulate(long ticksPassed, double multiplier);
}
