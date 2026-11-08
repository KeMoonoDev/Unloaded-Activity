package dev.moono.unloadedactivity.api;

public interface UnloadedActivityApi {
    default void registerNumberFetchers(NumberFetcherRegistry registry) {}
    default void registerSimulationMethods(SimulationMethodRegistry registry) {}
    default void registerBlockEntitySimulations(BlockEntitySimulationRegistry registry) {}
    default void registerEntitySimulations(EntitySimulationRegistry registry) {}
}