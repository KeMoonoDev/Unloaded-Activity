package dev.moono.unloadedactivity.api.weather_history;

#if MC_VER >= MC_1_21_5
import net.minecraft.world.level.saveddata.SavedDataType;
#elif MC_VER >= MC_1_20_2
import net.minecraft.world.level.saveddata.SavedData;
#endif

#if MC_VER >= MC_1_20_2
import net.minecraft.util.datafix.DataFixTypes;
#endif

import net.minecraft.core.HolderLookup;

import com.mojang.serialization.Codec;
import dev.moono.unloadedactivity.UnloadedActivity;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.LongStream;

import static dev.moono.unloadedactivity.UnloadedActivity.MOD_ID;

public class WeatherDayTickHistory extends WeatherHistory {
    public static final String DATA_NAME = "weather_list";

    #if MC_VER >= MC_1_21_11
    public static final Codec<WeatherDayTickHistory> CODEC = Codec.LONG_STREAM.xmap(
            WeatherDayTickHistory::new,
            WeatherDayTickHistory::getWeatherStream
    );
    #endif

    public WeatherDayTickHistory() {
        super();
    }

    public WeatherDayTickHistory(ArrayList<Long> weatherList) {
        super(weatherList);
    }

    public WeatherDayTickHistory(LongStream weatherList) {
        super(weatherList);
    }

    public LongStream getWeatherStream() {
        return this.getWeatherList().stream().mapToLong((v) -> v);
    }

    #if MC_VER >= MC_1_21_5
    public static final SavedDataType<WeatherDayTickHistory> type = new SavedDataType<>(
			#if MC_VER >= MC_26_1_2
            UnloadedActivity.id(DATA_NAME),
			#else
            MOD_ID + "-" + WeatherDayTickHistory.DATA_NAME,
			#endif
			#if MC_VER >= MC_1_21_11
            WeatherDayTickHistory::new,
            WeatherDayTickHistory.CODEC,
			#else
			(ctx) -> new WeatherDayTickHistory(),
			(ctx) -> {
				return CompoundTag.CODEC.xmap(
						WeatherDayTickHistory::load,
						weatherData -> weatherData.save(new CompoundTag())
				);
			},
			#endif
            DataFixTypes.LEVEL
    );
    #elif MC_VER >= MC_1_20_2
    public static final SavedData.Factory<WeatherDayTickHistory> type = new SavedData.Factory<>(
            WeatherDayTickHistory::new,
            WeatherDayTickHistory::load,
            net.minecraft.util.datafix.DataFixTypes.LEVEL
    );
    #endif

    #if MC_VER < MC_1_21_11

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
        nbt.putLongArray(DATA_NAME, this.getWeatherStream().toArray());
        #else
        nbt.putLongArray(DATA_NAME, this.getWeatherList());
        #endif
        return nbt;
    }

    #if MC_VER <= MC_1_20_4
    public static WeatherDayTickHistory load(CompoundTag nbt)
    #elif MC_VER >= MC_1_21_5
    public static WeatherDayTickHistory load(CompoundTag nbt)
    #else
    public static WeatherDayTickHistory load(CompoundTag nbt, HolderLookup.Provider holderLookup)
    #endif
    {
        #if MC_VER >= MC_1_21_5
        long[] longArray = new long[]{};
        Optional<long[]> optionalLongs = nbt.getLongArray(DATA_NAME);
        if (optionalLongs.isPresent()) {
            longArray = optionalLongs.get();
        }
        #else
        long[] longArray = nbt.getLongArray(DATA_NAME);
        #endif
        ArrayList<Long> longArrayList = new ArrayList<>();

        for (long value : longArray) {
            longArrayList.add(value);
        }

        return new WeatherDayTickHistory(longArrayList);
    }
    #endif
}
