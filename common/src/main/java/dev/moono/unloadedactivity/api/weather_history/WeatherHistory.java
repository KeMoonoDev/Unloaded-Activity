package dev.moono.unloadedactivity.api.weather_history;

#if MC_VER >= MC_1_21_11
import com.mojang.serialization.Codec;
#endif

#if MC_VER <= MC_1_20_4
#elif MC_VER >= MC_1_21_5
#else
import net.minecraft.core.HolderLookup;
#endif

import net.minecraft.world.level.saveddata.SavedData;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.stream.LongStream;

import static java.lang.Math.max;
import static java.lang.Math.min;

public abstract class WeatherHistory extends SavedData {
    final int maxWeatherHistory = 3;
    private final ArrayList<Long> weatherList;

    public WeatherHistory() {
        this.weatherList = new ArrayList<>();
    }

    public WeatherHistory(ArrayList<Long> weatherList) {
        this.weatherList = weatherList;
    }

    public WeatherHistory(LongStream weatherList) {
        long[] longs = weatherList.toArray();
        Long[] longObjects = ArrayUtils.toObject(longs);
        this.weatherList = new ArrayList<>(Arrays.asList(longObjects));
    }

    public ArrayList<Long> getWeatherList() {
        return this.weatherList;
    }

    public boolean shouldCheckForRain() {
        return this.weatherList.size() % 2 == 0;
    }

    public void updateValues(long currentTime, boolean isRaining) {
        //if player decides to go back in time, this will try to reduce any weird behaviour.
        while (!this.weatherList.isEmpty()) {
            if (this.weatherList.get(0) > currentTime) {
                this.weatherList.remove(0);
                this.setDirty();
                continue;
            }
            break;
        }

        boolean checkForRain = this.shouldCheckForRain();

        if (isRaining && checkForRain) {
            this.weatherList.add(0, currentTime);
            this.setDirty();
        } else if (!isRaining && !checkForRain) {
            this.weatherList.add(0, currentTime);
            this.setDirty();
        }

        int weatherListSize = this.weatherList.size();

        if (weatherListSize > max(maxWeatherHistory, 1)*2) {
            this.weatherList.remove(weatherListSize-1);
            this.weatherList.remove(weatherListSize-2);
        }
    }

    public boolean getWeatherAtTime(long currentTime) {
        int indexOffset = shouldCheckForRain() ? 1 : 0;
        for (int i=this.weatherList.size() - 1;i>=0;i--) {
            if (this.weatherList.get(i) > currentTime) {
                // If this passes the first index, it should be false (no weather) because it's outside the history range.
                // After that, it alternates.
                return i % 2 != indexOffset;
            }
        }
        // If we need to check for rain, that means it's not raining.
        return !shouldCheckForRain();
    }

    public long getNextWeatherChangeDuration(long currentTime) {
        for (int i=this.weatherList.size() - 1;i>=0;i--) {
            long duration = this.weatherList.get(i) - currentTime;
            if (duration > 0) {
                return duration;
            }
        }
        return Long.MAX_VALUE;
    }
}
