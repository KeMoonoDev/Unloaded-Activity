package dev.moono.unloadedactivity.interfaces;

import dev.moono.unloadedactivity.api.WorldWeatherForecast;

public interface WorldForecastGetter {
    default WorldWeatherForecast getWeatherForecast() {
        return null;
    };
}
