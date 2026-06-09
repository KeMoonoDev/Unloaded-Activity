package dev.moono.unloadedactivity.interfaces;

import dev.moono.unloadedactivity.WorldWeatherForecast;

public interface WorldForecastGetter {
    default WorldWeatherForecast getWeatherForecast() {
        return null;
    };
}
