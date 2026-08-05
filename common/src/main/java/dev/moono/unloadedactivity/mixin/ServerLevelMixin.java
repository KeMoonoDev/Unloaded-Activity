package dev.moono.unloadedactivity.mixin;

#if MC_VER >= MC_1_19_4
import dev.moono.unloadedactivity.api.weather_history.WeatherMsHistory;
import dev.moono.unloadedactivity.api.weather_history.WeatherTickHistory;
import net.minecraft.core.RegistryAccess;
#endif

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import dev.moono.unloadedactivity.TimeMachine;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.SimulatedTime;
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

		long currentTick = GameUtils.getTime(this);
		long currentMs = System.currentTimeMillis();

		SimulatedTime simulatedTime = SimulatedTime.PLACEHOLDER;

		if (UnloadedActivity.config.useSystemTime) {
			long lastMs = chunk.getLastMs();
			if (lastMs > 0) {
				simulatedTime = SimulatedTime.fromLastMs(lastMs, currentTick, currentMs);
			}
		} else {
			long lastTick = chunk.getLastTick();
			if (lastTick > 0) {
				simulatedTime = SimulatedTime.fromLastTick(lastTick, currentTick, currentMs);
			}
		}

		if (simulatedTime.remainingTicks() > 0) {
			if (!TimeMachine.isChunkIndexed(chunk)) return;

			int tickDifferenceThreshold = UnloadedActivity.config.tickDifferenceThreshold;

			if (simulatedTime.remainingTicks() > tickDifferenceThreshold) {
				if (updateCount < UnloadedActivity.config.maxChunkUpdatesPerTick*getMultiplier()) {
					++updateCount;
					int groupUpdateBudget = UnloadedActivity.config.maxGroupUpdatesPerTick - groupUpdateCount;
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

		chunk.setLastTick(currentTick);
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
		WeatherTickHistory weatherTickHistory = GameUtils.getWeatherTickHistory(this.getLevel());
		weatherTickHistory.updateValues(GameUtils.getTime(this), this.isRaining());
		WeatherMsHistory weatherMsHistory = GameUtils.getWeatherMsHistory(this.getLevel());
		weatherMsHistory.updateValues(System.currentTimeMillis(), this.isRaining());
	}

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void createState(CallbackInfo ci) {
		GameUtils.getWeatherTickHistory(this.getLevel());
		GameUtils.getWeatherMsHistory(this.getLevel());
	}
}

