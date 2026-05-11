package lol.zanspace.unloadedactivity.interfaces;

import lol.zanspace.unloadedactivity.WorldWeatherForecast;

public interface WorldForecastGetter {
    default WorldWeatherForecast getWeatherForecast() {
        return null;
    };
}
