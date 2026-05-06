package lol.zanspace.unloadedactivity.mixin;

#if MC_VER >= MC_1_21_5
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedDataType;
#endif

#if MC_VER >= MC_1_20_2
import net.minecraft.util.datafix.DataFixTypes;
#endif
#if MC_VER >= MC_1_19_4
import net.minecraft.core.RegistryAccess;
#endif
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import lol.zanspace.unloadedactivity.TimeMachine;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.WorldWeatherData;
import lol.zanspace.unloadedactivity.interfaces.WorldTimeData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
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
public abstract class ServerLevelMixin extends Level implements WorldGenLevel, WorldTimeData {
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
	public int msTime = 0;

	@Shadow public ServerLevel getLevel() {return null;}

	@Inject(at = @At("HEAD"), method = "tickChunk")
	private void tickChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo info) {

		if (this.isClientSide())
			return;

		long lastTick = chunk.getLastTick();
		long currentTime = this.getDayTime();

		if (lastTick != 0) {
			if (!TimeMachine.isChunkIndexed(chunk)) {
				return;
			}

			long timeDifference = Math.max(currentTime - lastTick, 0);

			int differenceThreshold = UnloadedActivity.config.tickDifferenceThreshold;

			if (timeDifference > differenceThreshold) {
				if (updateCount < UnloadedActivity.config.maxChunkUpdatesPerTick*getMultiplier()) {
					++updateCount;
					msTime += TimeMachine.simulateChunk(timeDifference, this.getLevel(), chunk, randomTickSpeed);
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
		if (UnloadedActivity.config.debugLogs && (updateCount) != 0) {
			int averageMs = (int)((float) msTime / (updateCount) + 0.5);
			UnloadedActivity.LOGGER.info("Average chunk update time for "+updateCount+" chunks: "+averageMs+"ms");
			UnloadedActivity.LOGGER.info("Total chunk update time for "+updateCount+" chunks: "+msTime+"ms");
		}
		msTime = 0;
		updateCount = 0;
	}

	@Inject(method = "tick", at = @At(value = "TAIL", target = "net/minecraft/server/level/ServerLevel.tickTime ()V"))
	private void finishTickTime(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		WorldWeatherData weatherInfo = this.getWeatherData();
		weatherInfo.updateValues(this);
	}

	@Shadow public abstract DimensionDataStorage getDataStorage();

	#if MC_VER >= MC_1_21_5
	private static SavedDataType<WorldWeatherData> type = new SavedDataType<WorldWeatherData>(
			"unloaded_activity",
	        #if MC_VER >= MC_1_21_11
            WorldWeatherData::new,
            WorldWeatherData.CODEC,
            #else
			(ctx) -> new WorldWeatherData(),
			(ctx) -> {
				return CompoundTag.CODEC.xmap(
                        WorldWeatherData::load,
						weatherData -> weatherData.save(new CompoundTag())
				);
			},
            #endif
			DataFixTypes.LEVEL
	);
	#elif MC_VER >= MC_1_20_2
	@Unique
	private static SavedData.Factory<WorldWeatherData> type = new SavedData.Factory<>(
			WorldWeatherData::new,
			WorldWeatherData::load,
			net.minecraft.util.datafix.DataFixTypes.LEVEL
	);
	#endif

	@Override
	public WorldWeatherData getWeatherData() {
		return this.getDataStorage().computeIfAbsent(
			#if MC_VER >= MC_1_20_2
			type
			#else
                WorldWeatherData::load,
                WorldWeatherData::new
			#endif
			#if MC_VER < MC_1_21_5
			, "unloaded_activity"
			#endif
		);
	}

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void createState(CallbackInfo ci) {
		this.getWeatherData();
	}
}

