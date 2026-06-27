package dev.moono.unloadedactivity.api.context;

public interface WeatherDependantContext extends FixedContext {
    boolean isRaining();
    boolean isThundering();
}
