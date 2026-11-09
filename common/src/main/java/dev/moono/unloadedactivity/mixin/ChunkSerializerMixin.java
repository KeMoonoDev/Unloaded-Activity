package dev.moono.unloadedactivity.mixin;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

#if MC_VER < MC_1_21_10
import net.minecraft.core.RegistryAccess;
#endif
import dev.moono.unloadedactivity.GroupChunkIndex;
import dev.moono.unloadedactivity.UnloadedActivity;
import net.minecraft.nbt.CompoundTag;
import dev.moono.unloadedactivity.GameUtils;
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
#endif
#if MC_VER >= MC_1_21_10
import net.minecraft.world.level.chunk.PalettedContainerFactory;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

import static dev.moono.unloadedactivity.UnloadedActivity.MOD_ID;
import static dev.moono.unloadedactivity.UnloadedActivity.OLD_MOD_ID;

#if MC_VER <= MC_1_21_1
@Mixin(value = ChunkSerializer.class, priority = 999)
public abstract class ChunkSerializerMixin {
    @Inject(method = "write", at = @At("RETURN"))
    private static void write(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag nbt = cir.getReturnValue();

        CompoundTag chunkData = new CompoundTag();

        long chunkLastDayTick = chunk.getLastDayTick();
        long chunkLastGameTick = chunk.getLastGameTick();
        long chunkLastMs = chunk.getLastMs();

        chunkData.putLong("last_day_tick", chunkLastDayTick);
        chunkData.putLong("last_game_tick", chunkLastGameTick);
        chunkData.putLong("last_ms", chunkLastMs);
        chunkData.putLong("ver", chunk.getSimulationVersion());
        chunkData.putLongArray("sim_blocks", chunk.getSimulationBlocks());

        CompoundTag groupsData = new CompoundTag();
        for (GroupChunkIndex groupChunkIndex : chunk.getGroupIndexes()) {
            var groupId = groupChunkIndex.groupId;
            CompoundTag groupData = new CompoundTag();

            groupData.putLongArray("positions", groupChunkIndex.getPositions());
            groupData.putLong("last_day_tick", groupChunkIndex.getLastDayTick(chunkLastDayTick));
            groupData.putLong("last_game_tick", groupChunkIndex.getLastGameTick(chunkLastGameTick));
            groupData.putLong("last_ms", groupChunkIndex.getLastMs(chunkLastMs));

            groupsData.put(groupId.toString(), groupData);
        }
        chunkData.put("groups", groupsData);

        nbt.put(MOD_ID, chunkData);
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

        CompoundTag chunkData = nbtCompound.getCompound(MOD_ID);
        if (chunkData.isEmpty()) {
            chunkData = nbtCompound.getCompound(OLD_MOD_ID);
        }

        boolean isEmpty = chunkData.isEmpty();

        if (isEmpty) {
            protoChunk.setUnsaved(true);
        } else {
            if (chunkData.contains("last_day_tick")) {
                protoChunk.setLastDayTick(chunkData.getLong("last_day_tick"));
            } else {
                protoChunk.setLastDayTick(chunkData.getLong("last_tick"));
            }
            protoChunk.setLastGameTick(chunkData.getLong("last_game_tick"));
            protoChunk.setLastMs(chunkData.getLong("last_ms"));
            protoChunk.setSimulationVersion(chunkData.getLong("ver"));
            protoChunk.setSimulationBlocks(chunkData.getLongArray("sim_blocks"));

            ArrayList<GroupChunkIndex> groupIndexes = new ArrayList<>();

            CompoundTag groupsData = chunkData.getCompound("groups");
            for (String groupKey : groupsData.getAllKeys()) {
                var maybeId = ResourceLocation.read(groupKey);
                if (maybeId.result().isEmpty())
                    continue;

                var groupId = maybeId.result().get();

                CompoundTag groupData = groupsData.getCompound(groupKey);

                long groupLastDayTick;
                if (groupData.contains("last_day_tick")) {
                    groupLastDayTick = groupData.getLong("last_day_tick");
                } else {
                    groupLastDayTick = groupData.getLong("last_tick");
                }
                long groupLastGameTick = groupData.getLong("last_game_tick");
                long groupLastMs = groupData.getLong("last_ms");

                if (groupLastDayTick == 0L) groupLastDayTick = protoChunk.getLastDayTick();
                if (groupLastGameTick == 0L) groupLastGameTick = protoChunk.getLastGameTick();
                if (groupLastMs == 0L) groupLastMs = protoChunk.getLastMs();

                GroupChunkIndex groupChunkIndex = new GroupChunkIndex(new ArrayList<>(), groupLastDayTick, groupLastGameTick, groupLastMs, groupId);
                groupChunkIndex.setPositions(groupData.getLongArray("positions"));

                groupIndexes.add(groupChunkIndex);
            }

            protoChunk.setGroupIndexes(groupIndexes);
        }


        if (UnloadedActivity.config.convertCCAData && isEmpty) {
            CompoundTag cardinalData = nbtCompound.getCompound("cardinal_components");

            if (!cardinalData.isEmpty()) {
                CompoundTag lastChunkTick = cardinalData.getCompound("unloadedactivity:last-chunk-tick");
                if (!lastChunkTick.isEmpty()) {
                    protoChunk.setLastDayTick(lastChunkTick.getLong("last-tick"));
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

        if (protoChunk.getLastDayTick() == 0) {
            protoChunk.setUnsaved(true);
            protoChunk.setLastDayTick(level.getDayTime());
            protoChunk.setLastGameTick(level.getGameTime());
            protoChunk.setLastMs(System.currentTimeMillis());
        }
    }
}

#else
@Mixin(value = SerializableChunkData.class, priority = 999)
public abstract class ChunkSerializerMixin {

    @Unique
    private ArrayList<GroupChunkIndex> groupIndexes = new ArrayList<>();
    @Unique
    private long lastDayTick = 0;
    @Unique
    private long lastGameTick = 0;
    @Unique
    private long lastMs = 0;
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

        chunkData.putLong("last_day_tick", lastDayTick);
        chunkData.putLong("last_game_tick", lastGameTick);
        chunkData.putLong("last_ms", lastMs);
        chunkData.putLong("ver", ver);
        chunkData.putLongArray("sim_blocks", simBlocks);

        CompoundTag groupsData = new CompoundTag();
        for (GroupChunkIndex groupChunkIndex : groupIndexes) {
            var groupId = groupChunkIndex.groupId;
            CompoundTag groupData = new CompoundTag();

            groupData.putLongArray("positions", groupChunkIndex.getPositions().stream().mapToLong(l -> l).toArray());
            groupData.putLong("last_day_tick", groupChunkIndex.getLastDayTick(lastDayTick));
            groupData.putLong("last_game_tick", groupChunkIndex.getLastGameTick(lastGameTick));
            groupData.putLong("last_ms", groupChunkIndex.getLastMs(lastMs));

            groupsData.put(groupId.toString(), groupData);
        }
        chunkData.put("groups", groupsData);

        nbt.put(MOD_ID, chunkData);
    }

    @Inject(method = "read", at = @At("RETURN"))
    public void read(ServerLevel level, PoiManager poiManager, RegionStorageInfo key, ChunkPos expectedPos, CallbackInfoReturnable<ProtoChunk> cir) {
        ProtoChunk protoChunkTemp = cir.getReturnValue();

        ChunkAccess chunk = (protoChunkTemp instanceof ImposterProtoChunk imposterChunk) ? imposterChunk.getWrapped() : protoChunkTemp;

        chunk.setLastDayTick(lastDayTick);
        chunk.setLastGameTick(lastGameTick);
        chunk.setLastMs(lastMs);
        chunk.setSimulationVersion(ver);
        chunk.setSimulationBlocks(simBlocks);
        chunk.setGroupIndexes(groupIndexes);
        if (lastDayTick == 0) {
            chunk.markUnsaved();
            chunk.setLastDayTick(GameUtils.getDayTime(level));
            chunk.setLastGameTick(level.getGameTime());
            chunk.setLastMs(System.currentTimeMillis());
        } else if (needsSaving) {
            chunk.markUnsaved();
        }
    }

    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void fromChunk(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<SerializableChunkData> cir) {
        ChunkSerializerMixin serializedChunk = (ChunkSerializerMixin) (Object) cir.getReturnValue();
        if (serializedChunk != null) {
            serializedChunk.lastDayTick = chunk.getLastDayTick();
            serializedChunk.lastGameTick = chunk.getLastGameTick();
            serializedChunk.lastMs = chunk.getLastMs();
            serializedChunk.ver = chunk.getSimulationVersion();
            serializedChunk.simBlocks = chunk.getSimulationBlocks().stream().mapToLong(l -> l).toArray();
            serializedChunk.groupIndexes = chunk.getGroupIndexes();
        }
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private static void fromNbt(
    #if MC_VER >= MC_1_21_10
        LevelHeightAccessor world, PalettedContainerFactory palettesFactory, CompoundTag nbt, CallbackInfoReturnable<SerializableChunkData> cir
    #else
        LevelHeightAccessor levelHeightAccessor, RegistryAccess registryAccess, CompoundTag nbt, CallbackInfoReturnable<SerializableChunkData> cir
    #endif
    ) {
        ChunkSerializerMixin serializedChunk = (ChunkSerializerMixin)(Object)cir.getReturnValue();

        if (serializedChunk == null) {
            return;
        }

        CompoundTag chunkData = nbt.getCompound(MOD_ID)#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
        if (chunkData.isEmpty()) {
            chunkData = nbt.getCompound(OLD_MOD_ID)#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
        }

        boolean isEmpty = chunkData.isEmpty();

        if (isEmpty) {
            serializedChunk.needsSaving = true;
        } else {
            if (chunkData.contains("last_day_tick")) {
                serializedChunk.lastDayTick = chunkData.getLong("last_day_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            } else {
                serializedChunk.lastDayTick = chunkData.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            }
            serializedChunk.lastGameTick = chunkData.getLong("last_game_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            serializedChunk.lastMs = chunkData.getLong("last_ms")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            serializedChunk.ver = chunkData.getLong("ver")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            serializedChunk.simBlocks = chunkData.getLongArray("sim_blocks")#if MC_VER >= MC_1_21_5 .orElse(new long[]{})#endif;

            ArrayList<GroupChunkIndex> groupIndexes = new ArrayList<>();

            CompoundTag groupsData = chunkData.getCompound("groups")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
            #if MC_VER >= MC_1_21_5
            var keySet = groupsData.keySet();
            #else
            var keySet = groupsData.getAllKeys();
            #endif
            for (String key : keySet) {
                var maybeId = #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif.read(key);
                if (maybeId.result().isEmpty())
                    continue;

                var groupId = maybeId.result().get();

                CompoundTag groupData = groupsData.getCompound(key)#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;

                long groupLastDayTick;
                if (groupData.contains("last_day_tick")) {
                    groupLastDayTick = groupData.getLong("last_day_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
                } else {
                    groupLastDayTick = groupData.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
                }
                long groupLastGameTick = groupData.getLong("last_game_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
                long groupLastMs = groupData.getLong("last_ms")#if MC_VER >= MC_1_21_5 .orElse(0L)#endif;

                if (groupLastDayTick == 0L) groupLastDayTick = serializedChunk.lastDayTick;
                if (groupLastGameTick == 0L) groupLastGameTick = serializedChunk.lastGameTick;
                if (groupLastMs == 0L) groupLastMs = serializedChunk.lastMs;

                GroupChunkIndex groupChunkIndex = new GroupChunkIndex(new ArrayList<>(), groupLastDayTick, groupLastGameTick, groupLastMs, groupId);
                groupChunkIndex.setPositions(groupData.getLongArray("positions")#if MC_VER >= MC_1_21_5 .orElse(new long[]{})#endif);

                groupIndexes.add(groupChunkIndex);
            }

            serializedChunk.groupIndexes = groupIndexes;
        }


        if (UnloadedActivity.config.convertCCAData && isEmpty) {
            CompoundTag cardinalData = nbt.getCompound("cardinal_components")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;

            if (!cardinalData.isEmpty()) {
                CompoundTag lastChunkTick = cardinalData.getCompound("unloadedactivity:last-chunk-tick")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
                if (!lastChunkTick.isEmpty()) {
                    serializedChunk.lastDayTick = lastChunkTick.getLong("last-tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
                }

                CompoundTag chunkSimVer = cardinalData.getCompound("unloadedactivity:chunk-sim-ver")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
                if (!chunkSimVer.isEmpty()) {
                    serializedChunk.ver = chunkSimVer.getLong("sim-ver")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
                }

                CompoundTag chunkSimBlocks = cardinalData.getCompound("unloadedactivity:chunk-sim-blocks")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
                if (!chunkSimBlocks.isEmpty()) {
                    serializedChunk.simBlocks = chunkSimBlocks.getLongArray("sim-blocks")#if MC_VER >= MC_1_21_5 .orElse(new long[]{})#endif;
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