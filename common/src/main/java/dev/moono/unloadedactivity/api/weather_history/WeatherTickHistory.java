package dev.moono.unloadedactivity.api.weather_history;

#if MC_VER >= MC_1_21_5
import net.minecraft.world.level.saveddata.SavedDataType;
#elif MC_VER >= MC_1_20_2
import net.minecraft.world.level.saveddata.SavedData;
#endif

#if MC_VER >= MC_1_20_2
import net.minecraft.util.datafix.DataFixTypes;
#endif

import com.mojang.serialization.Codec;
import dev.moono.unloadedactivity.UnloadedActivity;

import java.util.ArrayList;
import java.util.stream.LongStream;

public class WeatherTickHistory extends WeatherHistory {
    private static final String DATA_NAME = "weather_list";

    #if MC_VER >= MC_1_21_11
    public static final Codec<WeatherTickHistory> CODEC = Codec.LONG_STREAM.xmap(
            WeatherTickHistory::new,
            WeatherTickHistory::getWeatherStream
    );

    public WeatherTickHistory() {
        super();
    }

    public WeatherTickHistory(ArrayList<Long> weatherList) {
        super(weatherList);
    }

    public WeatherTickHistory(LongStream weatherList) {
        super(weatherList);
    }

    public LongStream getWeatherStream() {
        return this.getWeatherList().stream().mapToLong((v) -> v);
    }
    #endif

    #if MC_VER >= MC_1_21_5
    public static final SavedDataType<WeatherTickHistory> type = new SavedDataType<>(
			#if MC_VER >= MC_26_1_2
            UnloadedActivity.id(DATA_NAME),
			#else
			MOD_ID,
			#endif
			#if MC_VER >= MC_1_21_11
            WeatherTickHistory::new,
            CODEC,
			#else
			(ctx) -> new WeatherTickHistory(),
			(ctx) -> {
				return CompoundTag.CODEC.xmap(
						WeatherTickHistory::load,
						weatherData -> weatherData.save(new CompoundTag())
				);
			},
			#endif
            DataFixTypes.LEVEL
    );
    #elif MC_VER >= MC_1_20_2
    @Unique
    private static final SavedData.Factory<WeatherTickHistory> type = new SavedData.Factory<>(
            WeatherTickHistory::new,
            WeatherTickHistory::load,
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
        nbt.putLongArray(DATA_NAME, this.weatherList.stream().mapToLong(l -> l).toArray());
        #else
        nbt.putLongArray(DATA_NAME, this.weatherList);
        #endif
        return nbt;
    }

    #if MC_VER <= MC_1_20_4
    public static WeatherTickHistory load(CompoundTag nbt)
    #elif MC_VER >= MC_1_21_5
    public static WeatherTickHistory load(CompoundTag nbt)
    #else
    public static WeatherTickHistory load(CompoundTag nbt, HolderLookup.Provider holderLookup)
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

        return new WeatherTickHistory(longArrayList);
    }
    #endif
}
