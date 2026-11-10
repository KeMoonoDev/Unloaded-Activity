package dev.moono.unloadedactivity.mixin;

#if MC_VER >= MC_1_19_4
import net.minecraft.core.RegistryAccess;
#endif

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import dev.moono.unloadedactivity.TimeMachine;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.weather_history.WeatherDayTickHistory;
import dev.moono.unloadedactivity.api.weather_history.WeatherGameTickHistory;
import dev.moono.unloadedactivity.api.weather_history.WeatherMsHistory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
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
import java.util.function.Supplier;


@Mixin(value = ServerLevel.class, priority = 1001)
public abstract class ServerLevelMixin extends Level implements WorldGenLevel {
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

		long currentDayTick = GameUtils.getDayTime(this);
		long currentGameTick = this.getGameTime();
		long currentMs = System.currentTimeMillis();

		long tickDifference = GameUtils.getTickDifference(chunk, currentDayTick, currentGameTick, currentMs);

		if (tickDifference > 0) {
			if (!TimeMachine.isChunkIndexed(chunk)) return;

			int tickDifferenceThreshold = UnloadedActivity.config.tickDifferenceThreshold;

			if (tickDifference > tickDifferenceThreshold) {
				if (updateCount < UnloadedActivity.config.maxChunkUpdatesPerTick*getMultiplier()) {
					++updateCount;
					int groupUpdateBudget = UnloadedActivity.config.maxGroupUpdatesPerTick - groupUpdateCount;
					SimulatedTime simulatedTime = SimulatedTime.fromRemainingTicks(tickDifference, currentDayTick,currentGameTick,currentMs);
					Pair<Integer, Boolean> result = TimeMachine.simulateChunk(this.getLevel(), chunk, simulatedTime, randomTickSpeed, groupUpdateBudget);
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

		chunk.setLastDayTick(currentDayTick);
		chunk.setLastGameTick(currentGameTick);
		chunk.setLastMs(currentMs);
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
		WeatherDayTickHistory weatherDayTickHistory = GameUtils.getWeatherDayTickHistory(this.getLevel());
		weatherDayTickHistory.updateValues(GameUtils.getDayTime(this), this.isRaining());
		WeatherGameTickHistory weatherGameTickHistory = GameUtils.getWeatherGameTickHistory(this.getLevel());
		weatherGameTickHistory.updateValues(this.getGameTime(), this.isRaining());
		WeatherMsHistory weatherMsHistory = GameUtils.getWeatherMsHistory(this.getLevel());
		weatherMsHistory.updateValues(System.currentTimeMillis(), this.isRaining());
	}

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void createState(CallbackInfo ci) {
		GameUtils.getWeatherDayTickHistory(this.getLevel());
		GameUtils.getWeatherGameTickHistory(this.getLevel());
		GameUtils.getWeatherMsHistory(this.getLevel());
	}
}

