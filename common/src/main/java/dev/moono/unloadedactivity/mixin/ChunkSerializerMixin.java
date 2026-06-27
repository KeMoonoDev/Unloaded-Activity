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

        long chunkLastTick = chunk.getLastTick();

        chunkData.putLong("last_tick", chunkLastTick);
        chunkData.putLong("ver", chunk.getSimulationVersion());
        chunkData.putLongArray("sim_blocks", chunk.getSimulationBlocks());

        CompoundTag groupsData = new CompoundTag();
        for (GroupChunkIndex groupChunkIndex : chunk.getGroupIndexes()) {
            var groupId = groupChunkIndex.groupId;
            CompoundTag groupData = new CompoundTag();

            groupData.putLongArray("positions", groupChunkIndex.getPositions());
            groupData.putLong("last_tick", groupChunkIndex.getLastTick(chunkLastTick));

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
            protoChunk.setLastTick(chunkData.getLong("last_tick"));
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

                long groupLastTicked = groupData.getLong("last_tick");

                GroupChunkIndex groupChunkIndex = new GroupChunkIndex(new ArrayList<>(), groupLastTicked, groupId);
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
    private ArrayList<GroupChunkIndex> groupIndexes = new ArrayList<>();
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

        CompoundTag groupsData = new CompoundTag();
        for (GroupChunkIndex groupChunkIndex : groupIndexes) {
            var groupId = groupChunkIndex.groupId;
            CompoundTag groupData = new CompoundTag();

            groupData.putLongArray("positions", groupChunkIndex.getPositions().stream().mapToLong(l -> l).toArray());
            groupData.putLong("last_tick", groupChunkIndex.getLastTick(lastTick));

            groupsData.put(groupId.toString(), groupData);
        }
        chunkData.put("groups", groupsData);

        nbt.put(MOD_ID, chunkData);
    }

    @Inject(method = "read", at = @At("RETURN"))
    public void read(ServerLevel level, PoiManager poiManager, RegionStorageInfo key, ChunkPos expectedPos, CallbackInfoReturnable<ProtoChunk> cir) {
        ProtoChunk protoChunkTemp = cir.getReturnValue();

        ChunkAccess chunk = (protoChunkTemp instanceof ImposterProtoChunk imposterChunk) ? imposterChunk.getWrapped() : protoChunkTemp;

        chunk.setLastTick(lastTick);
        chunk.setSimulationVersion(ver);
        chunk.setSimulationBlocks(simBlocks);
        chunk.setGroupIndexes(groupIndexes);
        if (lastTick == 0) {
            chunk.markUnsaved();
            chunk.setLastTick(GameUtils.getTime(level));
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
            serializedChunk.simBlocks = chunk.getSimulationBlocks().stream().mapToLong(l -> l).toArray();
            serializedChunk.groupIndexes = chunk.getGroupIndexes();
        }
    }

    @Inject(method = "parse", at = @At("RETURN"))
    #if MC_VER >= MC_1_21_10
    private static void fromNbt(LevelHeightAccessor world, PalettedContainerFactory palettesFactory, CompoundTag nbt, CallbackInfoReturnable<SerializableChunkData> cir) {
    #else
    private static void parse(LevelHeightAccessor levelHeightAccessor, RegistryAccess registryAccess, CompoundTag nbt, CallbackInfoReturnable<SerializableChunkData> cir) {
    #endif
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
            serializedChunk.lastTick = chunkData.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            serializedChunk.ver = chunkData.getLong("ver")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            serializedChunk.simBlocks = chunkData.getLongArray("sim_blocks")#if MC_VER >= MC_1_21_5 .orElse(new long[]{})#endif;

            ArrayList<GroupChunkIndex> groupIndexes = new ArrayList<>();

            CompoundTag groupsData = chunkData.getCompound("groups")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
            #if MC_VER >= MC_1_21_5
            for (String key : groupsData.keySet()) {
            #else
            for (String key : groupsData.getAllKeys()) {
            #endif
                var maybeId = #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif.read(key);
                if (maybeId.result().isEmpty())
                    continue;

                var groupId = maybeId.result().get();

                CompoundTag groupData = groupsData.getCompound(key)#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;

                long groupLastTicked = groupData.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(serializedChunk.lastTick)#endif;

                GroupChunkIndex groupChunkIndex = new GroupChunkIndex(new ArrayList<>(), groupLastTicked, groupId);
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
                    serializedChunk.lastTick = lastChunkTick.getLong("last-tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
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