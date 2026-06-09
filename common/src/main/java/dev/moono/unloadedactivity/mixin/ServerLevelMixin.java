package dev.moono.unloadedactivity.mixin;

#if MC_VER >= MC_1_21_5
import net.minecraft.world.level.saveddata.SavedDataType;
#endif

#if MC_VER >= MC_1_20_2
import net.minecraft.util.datafix.DataFixTypes;
#endif
#if MC_VER >= MC_1_19_4
import net.minecraft.core.RegistryAccess;
#endif
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import dev.moono.unloadedactivity.TimeMachine;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.WorldWeatherForecast;
import dev.moono.unloadedactivity.interfaces.WorldForecastGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;


@Mixin(value = ServerLevel.class, priority = 1001)
public abstract class ServerLevelMixin extends Level implements WorldGenLevel, WorldForecastGetter {
	#if MC_VER >= MC_1_21_3
	protected ServerLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
		super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
	}
	#elif MC_VER >= MC_1_19_4

	protected ServerLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
		super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
	}
    #else
    protected ServerLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, holder, supplier, bl, bl2, l, i);
    }
	#endif
	@Unique
	public int updateCount = 0;
	@Unique
	public int groupUpdateCount = 0;

	@Shadow public ServerLevel getLevel() {return null;}

	@Inject(method = "tickChunk", at = @At("HEAD"))
	#if MC_VER >= MC_26_1_2
	private void tickChunk(final LevelChunk chunk, final int randomTickSpeed, CallbackInfo info)
	#else
	private void tickChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo info)
	#endif
	{
		if (this.isClientSide())
			return;

		long lastTick = chunk.getLastTick();
		long currentTime = GameUtils.getTime(this);

		if (lastTick != 0) {
			if (!TimeMachine.isChunkIndexed(chunk)) {
				return;
			}

			long timeDifference = Math.max(currentTime - lastTick, 0);

			int differenceThreshold = UnloadedActivity.config.tickDifferenceThreshold;

			if (timeDifference > differenceThreshold) {
				if (updateCount < UnloadedActivity.config.maxChunkUpdatesPerTick*getMultiplier()) {
					++updateCount;
					int groupUpdateBudget = UnloadedActivity.config.maxGroupUpdatesPerTick - groupUpdateCount;
					Pair<Integer, Boolean> result = TimeMachine.simulateChunk(timeDifference, this.getLevel(), chunk, randomTickSpeed, groupUpdateBudget, currentTime);
					groupUpdateCount += result.getFirst();
					boolean simulatedAllGroups = result.getSecond();
					if (!simulatedAllGroups) {
						// Return early to not update the last tick.
						// This chunk has to be simulated again.
						return;
					}
				} else {
					return;
				}
			}
		}

		chunk.setLastTick(currentTime);
	}

	@Unique
	private int getMultiplier() {
		return UnloadedActivity.config.multiplyMaxChunkUpdatesPerPlayer ? Math.max(1, this.players().size()) : 1;
	}

	@Inject(method = "tick", at = @At(value = "TAIL"))
	private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		updateCount = 0;
		groupUpdateCount = 0;
	}

	@Inject(method = "tick", at = @At(value = "TAIL", target = "net/minecraft/server/level/ServerLevel.tickTime ()V"))
	private void finishTickTime(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		WorldWeatherForecast weatherInfo = this.getWeatherForecast();
		weatherInfo.updateValues(this);
	}

	#if MC_VER >= MC_1_21_5
	private static SavedDataType<WorldWeatherForecast> type = new SavedDataType<>(
	        #if MC_VER >= MC_26_1_2
			UnloadedActivity.id("weather_list"),
			#else
			MOD_ID,
			#endif
	        #if MC_VER >= MC_1_21_11
            WorldWeatherForecast::new,
            WorldWeatherForecast.CODEC,
            #else
			(ctx) -> new WorldWeatherForecast(),
			(ctx) -> {
				return CompoundTag.CODEC.xmap(
                        WorldWeatherForecast::load,
						weatherData -> weatherData.save(new CompoundTag())
				);
			},
            #endif
			DataFixTypes.LEVEL
	);
	#elif MC_VER >= MC_1_20_2
	@Unique
	private static SavedData.Factory<WorldWeatherForecast> type = new SavedData.Factory<>(
			WorldWeatherForecast::new,
			WorldWeatherForecast::load,
			net.minecraft.util.datafix.DataFixTypes.LEVEL
	);
	#endif

	@Override
	public WorldWeatherForecast getWeatherForecast() {
		return this.getLevel().getDataStorage().computeIfAbsent(
			#if MC_VER >= MC_1_20_2
			type
			#else
                WorldWeatherForecast::load,
                WorldWeatherForecast::new
			#endif
			#if MC_VER < MC_1_21_5
			, MOD_ID
			#endif
		);
	}

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void createState(CallbackInfo ci) {
		this.getWeatherForecast();
	}
}

