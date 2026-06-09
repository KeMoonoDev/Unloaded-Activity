package lol.zanspace.unloadedactivity.api;

public interface UnloadedActivityApi {
    void registerNumberFetchers(NumberFetcherRegistry registry);
    void registerSimulationMethods(SimulationMethodRegistry registry);
}