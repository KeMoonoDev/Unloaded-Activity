package dev.moono.unloadedactivity.api;

@FunctionalInterface
public interface EntitySimulation<T> {
    void consume(T entity, long ticksPassed, double multiplier);
}
