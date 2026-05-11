package lol.zanspace.unloadedactivity;

import net.minecraft.nbt.CompoundTag;

#if MC_VER > MC_1_20_4
import net.minecraft.core.HolderLookup;
#endif
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Optional;

#if MC_VER >= MC_1_21_11
import com.mojang.serialization.Codec;
import org.apache.commons.lang3.ArrayUtils;
import java.util.*;
import java.util.stream.LongStream;
#endif

import static java.lang.Math.max;
import static java.lang.Math.min;

public class WorldWeatherForecast extends SavedData {
    #if MC_VER >= MC_1_21_11
    public static final Codec<WorldWeatherForecast> CODEC = Codec.LONG_STREAM.fieldOf("weather_list").codec().xmap(
            WorldWeatherForecast::new,
            WorldWeatherForecast::getWeatherStream
    );

    public WorldWeatherForecast(LongStream weatherList) {
        long[] longs = weatherList.toArray();
        Long[] longObjects = ArrayUtils.toObject(longs);
        this.weatherList = new ArrayList<>(Arrays.asList(longObjects));
    }

    public LongStream getWeatherStream() {
        return this.getWeatherList().stream().mapToLong((v) -> v);
    }
    #endif

    final int maxWeatherHistory = 3;
    private ArrayList<Long> weatherList;

    public WorldWeatherForecast() {
        this.weatherList = new ArrayList<>();
    }

    #if MC_VER < MC_1_21_11
    public WorldWeatherForecast(ArrayList<Long> weatherList) {
        this.weatherList = weatherList;
    }

    #if MC_VER < MC_1_21_5
    @Override
    #endif
    #if MC_VER <= MC_1_20_4
    public CompoundTag save(CompoundTag nbt)
    #elif MC_VER >= MC_1_21_5
    public CompoundTag save(CompoundTag nbt)
    #else
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider holderLookup)
    #endif
    {
        #if MC_VER >= MC_1_21_5
        nbt.putLongArray("weather_list", this.weatherList.stream().mapToLong(l -> l).toArray());
        #else
        nbt.putLongArray("weather_list", this.weatherList);
        #endif
        return nbt;
    }

    #if MC_VER <= MC_1_20_4
    public static WorldWeatherForecast load(CompoundTag nbt)
    #elif MC_VER >= MC_1_21_5
    public static WorldWeatherForecast load(CompoundTag nbt)
    #else
    public static WorldWeatherForecast load(CompoundTag nbt, HolderLookup.Provider holderLookup)
    #endif
    {
        #if MC_VER >= MC_1_21_5
        long[] longArray = new long[]{};
        Optional<long[]> optionalLongs = nbt.getLongArray("weather_list");
        if (optionalLongs.isPresent()) {
            longArray = optionalLongs.get();
        }
        #else
        long[] longArray = nbt.getLongArray("weather_list");
        #endif
        ArrayList<Long> longArrayList = new ArrayList<>();

        for (long value : longArray) {
            longArrayList.add(value);
        }

        return new WorldWeatherForecast(longArrayList);
    }
    #endif

    public ArrayList<Long> getWeatherList() {
        return this.weatherList;
    }

    public boolean shouldCheckForRain() {
        return this.weatherList.size() % 2 == 0;
    }

    public void updateValues(Level level) {
        long currentTime = GameUtils.getTime(level);

        //if player decides to go back in time, this will try to reduce any weird behaviour.
        while (this.weatherList.size() > 0) {
            if (this.weatherList.get(0) > currentTime) {
                this.weatherList.remove(0);
                continue;
            }
            break;
        }

        boolean checkForRain = this.shouldCheckForRain();

        if (level.isRaining() && checkForRain) {
            this.weatherList.add(0, currentTime);
            this.setDirty();
        } else if (!level.isRaining() && !checkForRain) {
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

    public long getTimeInWeather(long timePassed, long currentTime) {
        int time = 0;

        long lastTicked = currentTime-timePassed;

        for (int i=0;i<min(max(maxWeatherHistory,1), (this.weatherList.size()+1)/2);i++) {
            int indexOffset = shouldCheckForRain() ? 0 : 1;
            long weatherStart = this.weatherList.get(i*2-indexOffset+1);
            long weatherEnd = (i-indexOffset) < 0 ? currentTime : this.weatherList.get(i*2-indexOffset);

            long difference = weatherEnd-weatherStart;

            long newWeatherStart = max(weatherStart,lastTicked);
            long newWeatherEnd = min(weatherEnd,currentTime);

            long newDifference = newWeatherEnd-newWeatherStart;

            time+=max(newDifference,0);

            if (newDifference != difference) {
                break;
            }
        }
        return time;
    }
}
