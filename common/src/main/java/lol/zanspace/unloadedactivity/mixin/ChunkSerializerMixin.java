package lol.zanspace.unloadedactivity.mixin;

import lol.zanspace.unloadedactivity.UnloadedActivity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
#if MC_VER <= MC_1_21_1
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
#endif

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
#if MC_VER >= MC_1_21_1
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
#endif

#if MC_VER >= MC_1_21_3
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.core.RegistryAccess;
#endif
#if MC_VER >= MC_1_21_10
import net.minecraft.world.level.chunk.PalettedContainerFactory;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

#if MC_VER <= MC_1_21_1
@Mixin(value = ChunkSerializer.class, priority = 999)
public abstract class ChunkSerializerMixin {
    @Inject(method = "write", at = @At("RETURN"))
    private static void write(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag nbt = cir.getReturnValue();

        CompoundTag chunkData = new CompoundTag();

        chunkData.putLong("last_tick", chunk.getLastTick());
        chunkData.putLong("ver", chunk.getSimulationVersion());
        chunkData.putLongArray("sim_blocks", chunk.getSimulationBlocks());

        CompoundTag groupLastTickData = new CompoundTag();
        for (var entry: chunk.getLastGroupTicks().entrySet()) {
            groupLastTickData.putLong(entry.getKey().toString(), entry.getValue());
        }
        chunkData.put("last_group_ticks", groupLastTickData);

        nbt.put("unloaded_activity", chunkData);
    }

    @Inject(method = "read", at = @At("RETURN"))
    #if MC_VER >= MC_1_21_1
    private static void read(ServerLevel level, PoiManager poiStorage, RegionStorageInfo key, ChunkPos chunkPos, CompoundTag nbtCompound, CallbackInfoReturnable<ProtoChunk> cir)
    #else
    private static void read(ServerLevel level, PoiManager poiStorage, ChunkPos chunkPos, CompoundTag nbtCompound, CallbackInfoReturnable<ProtoChunk> cir)
    #endif
    {
        ProtoChunk protoChunkTemp = cir.getReturnValue();

        ChunkAccess protoChunk = (protoChunkTemp instanceof ImposterProtoChunk readOnlyChunk) ? readOnlyChunk.getWrapped() : protoChunkTemp;

        CompoundTag chunkData = nbtCompound.getCompound("unloaded_activity");

        boolean isEmpty = chunkData.isEmpty();

        if (isEmpty) {
            protoChunk.setUnsaved(true);
        } else {
            protoChunk.setLastTick(chunkData.getLong("last_tick"));
            protoChunk.setSimulationVersion(chunkData.getLong("ver"));
            protoChunk.setSimulationBlocks(chunkData.getLongArray("sim_blocks"));

            CompoundTag groupLastTickData = chunkData.getCompound("last_group_ticks");

            for (String key : groupLastTickData.getAllKeys()) {
                long lastGroupTick = nbtCompound.getLong(key);
                if (lastGroupTick != 0) {
                    var maybeId = ResourceLocation.read(key);
                    if (maybeId.result().isEmpty())
                        continue;

                    protoChunk.setLastGroupTick(maybeId.result().get(), lastGroupTick);
                }

            }
        }


        if (UnloadedActivity.config.convertCCAData && isEmpty) {
            CompoundTag cardinalData = nbtCompound.getCompound("cardinal_components");

            if (!cardinalData.isEmpty()) {
                CompoundTag lastChunkTick = cardinalData.getCompound("unloadedactivity:last-chunk-tick");
                if (!lastChunkTick.isEmpty()) {
                    protoChunk.setLastTick(lastChunkTick.getLong("last-tick"));
                }

                CompoundTag chunkSimVer = cardinalData.getCompound("unloadedactivity:chunk-sim-ver");
                if (!chunkSimVer.isEmpty()) {
                    protoChunk.setSimulationVersion(chunkSimVer.getLong("sim-ver"));
                }

                CompoundTag chunkSimBlocks = cardinalData.getCompound("unloadedactivity:chunk-sim-blocks");
                if (!chunkSimBlocks.isEmpty()) {
                    protoChunk.setSimulationBlocks(chunkSimBlocks.getLongArray("sim-blocks"));
                }

                // This is so that cardinal components doesn't start sending warnings.
                cardinalData.remove("unloadedactivity:last-chunk-tick");
                cardinalData.remove("unloadedactivity:chunk-sim-ver");
                cardinalData.remove("unloadedactivity:chunk-sim-blocks");
                cardinalData.remove("unloadedactivity:magik");
            }
        }

        if (protoChunk.getLastTick() == 0) {
            protoChunk.setUnsaved(true);
            protoChunk.setLastTick(level.getDayTime());
        }
    }
}

#else
@Mixin(value = SerializableChunkData.class, priority = 999)
public abstract class ChunkSerializerMixin {

    @Unique
    private long lastTick = 0;
    @Unique
    private long ver = 0;
    @Unique
    private long[] simBlocks = {};
    @Unique
    private boolean needsSaving = false;

    @Inject(method = "write", at = @At("RETURN"))
    public void write(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag nbt = cir.getReturnValue();

        CompoundTag chunkData = new CompoundTag();

        chunkData.putLong("last_tick", lastTick);
        chunkData.putLong("ver", ver);
        chunkData.putLongArray("sim_blocks", simBlocks);

        nbt.put("unloaded_activity", chunkData);
    }

    @Inject(method = "read", at = @At("RETURN"))
    public void read(ServerLevel level, PoiManager poiManager, RegionStorageInfo key, ChunkPos expectedPos, CallbackInfoReturnable<ProtoChunk> cir) {
        ProtoChunk protoChunkTemp = cir.getReturnValue();

        ChunkAccess chunk = (protoChunkTemp instanceof ImposterProtoChunk imposterChunk) ? imposterChunk.getWrapped() : protoChunkTemp;

        chunk.setLastTick(lastTick);
        chunk.setSimulationVersion(ver);
        chunk.setSimulationBlocks(simBlocks);
        if (lastTick == 0) {
            chunk.markUnsaved();
            chunk.setLastTick(level.getDayTime());
        } else if (needsSaving) {
            chunk.markUnsaved();
        }
    }

    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void fromChunk(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<SerializableChunkData> cir) {
        ChunkSerializerMixin serializedChunk = (ChunkSerializerMixin) (Object) cir.getReturnValue();
        if (serializedChunk != null) {
            serializedChunk.lastTick = chunk.getLastTick();
            serializedChunk.ver = chunk.getSimulationVersion();
            serializedChunk.simBlocks = chunk.getSimulationBlocks().stream().mapToLong(l -> l).toArray();;
        }
    }

    @Inject(method = "parse", at = @At("RETURN"))
    #if MC_VER >= MC_1_21_10
    private static void fromNbt(LevelHeightAccessor world, PalettedContainerFactory palettesFactory, CompoundTag nbt, CallbackInfoReturnable<SerializableChunkData> cir) {
    #else
    private static void parse(LevelHeightAccessor world, RegistryAccess registryAccess, CompoundTag nbt, CallbackInfoReturnable<SerializableChunkData> cir) {
    #endif
        ChunkSerializerMixin serializedChunk = (ChunkSerializerMixin)(Object)cir.getReturnValue();

        if (serializedChunk == null) {
            return;
        }

        CompoundTag chunkData = nbt.getCompound("unloaded_activity")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;

        boolean isEmpty = chunkData.isEmpty();

        if (isEmpty) {
            serializedChunk.needsSaving = true;
        } else {
            serializedChunk.lastTick = chunkData.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            serializedChunk.ver = chunkData.getLong("ver")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            serializedChunk.simBlocks = chunkData.getLongArray("sim_blocks")#if MC_VER >= MC_1_21_5 .orElse(new long[]{})#endif;
        }


        if (UnloadedActivity.config.convertCCAData && isEmpty) {
            CompoundTag cardinalData = nbt.getCompound("cardinal_components")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;

            if (!cardinalData.isEmpty()) {
                CompoundTag lastChunkTick = cardinalData.getCompound("unloadedactivity:last-chunk-tick")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
                if (!lastChunkTick.isEmpty()) {
                    serializedChunk.lastTick = lastChunkTick.getLong("last-tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
                }

                CompoundTag chunkSimVer = cardinalData.getCompound("unloadedactivity:chunk-sim-ver")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
                if (!chunkSimVer.isEmpty()) {
                    serializedChunk.ver = chunkSimVer.getLong("sim-ver")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
                }

                CompoundTag chunkSimBlocks = cardinalData.getCompound("unloadedactivity:chunk-sim-blocks")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
                if (!chunkSimBlocks.isEmpty()) {
                    serializedChunk.simBlocks = chunkSimBlocks.getLongArray("sim-blocks")#if MC_VER >= MC_1_21_5 .orElse(new long[]{})#endif;;
                }

                // This is so that cardinal components doesn't start sending warnings.
                cardinalData.remove("unloadedactivity:last-chunk-tick");
                cardinalData.remove("unloadedactivity:chunk-sim-ver");
                cardinalData.remove("unloadedactivity:chunk-sim-blocks");
                cardinalData.remove("unloadedactivity:magik");
            }
        }
    }
}
#endif