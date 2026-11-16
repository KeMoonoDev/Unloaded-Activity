package dev.moono.unloadedactivity;

import dev.moono.unloadedactivity.config.SimulationClock;

public class SimulationInfo {
    public final SimulationClock simulationClock;
    public final long simulatedTicks;
    public double simulationDuration = 0.0;

    public SimulationInfo(SimulationClock simulationClock, long simulatedTicks) {
        this.simulationClock = simulationClock;
        this.simulatedTicks = simulatedTicks;
    }

    public SimulationInfo(SimulationClock simulationClock, long simulatedTicks, double simulationDuration) {
        this.simulationClock = simulationClock;
        this.simulatedTicks = simulatedTicks;
        this.simulationDuration = simulationDuration;
    }
}
