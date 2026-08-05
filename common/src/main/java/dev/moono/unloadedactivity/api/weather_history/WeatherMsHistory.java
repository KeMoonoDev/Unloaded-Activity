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

public class WeatherMsHistory extends WeatherHistory {
    private static final String DATA_NAME = "weather_list_ms";

    #if MC_VER >= MC_1_21_11
    public static final Codec<WeatherMsHistory> CODEC = Codec.LONG_STREAM.xmap(
            WeatherMsHistory::new,
            WeatherMsHistory::getWeatherStream
    );

    public WeatherMsHistory() {
        super();
    }

    public WeatherMsHistory(ArrayList<Long> weatherList) {
        super(weatherList);
    }

    public WeatherMsHistory(LongStream weatherList) {
        super(weatherList);
    }

    public LongStream getWeatherStream() {
        return this.getWeatherList().stream().mapToLong((v) -> v);
    }
    #endif

    #if MC_VER >= MC_1_21_5
    public static final SavedDataType<WeatherMsHistory> type = new SavedDataType<>(
			#if MC_VER >= MC_26_1_2
            UnloadedActivity.id(DATA_NAME),
			#else
			MOD_ID,
			#endif
			#if MC_VER >= MC_1_21_11
            WeatherMsHistory::new,
            WeatherMsHistory.CODEC,
			#else
			(ctx) -> new WeatherMsHistory(),
			(ctx) -> {
				return CompoundTag.CODEC.xmap(
						WeatherMsHistory::load,
						weatherData -> weatherData.save(new CompoundTag())
				);
			},
			#endif
            DataFixTypes.LEVEL
    );
    #elif MC_VER >= MC_1_20_2
    @Unique
    private static final SavedData.Factory<WeatherMsHistory> type = new SavedData.Factory<>(
            WeatherMsHistory::new,
            WeatherMsHistory::load,
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
    public static WeatherMsHistory load(CompoundTag nbt)
    #elif MC_VER >= MC_1_21_5
    public static WeatherMsHistory load(CompoundTag nbt)
    #else
    public static WeatherMsHistory load(CompoundTag nbt, HolderLookup.Provider holderLookup)
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

        return new WeatherMsHistory(longArrayList);
    }
    #endif
}
