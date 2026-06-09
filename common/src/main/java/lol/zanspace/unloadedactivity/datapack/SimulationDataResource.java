package lol.zanspace.unloadedactivity.datapack;

#if MC_VER >= MC_1_21_11
import lol.zanspace.unloadedactivity.GameUtils;
import lol.zanspace.unloadedactivity.mixin.MinecraftServerMixin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif
#if MC_VER >= MC_1_21_4
import net.minecraft.resources.FileToIdConverter;
#endif

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.JsonResourcesCollector;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

public class SimulationDataResource extends JsonResourcesCollector {

    private static final String BLOCKS_LOCATION = "simulate_info/blocks";
    private static final String TAGS_LOCATION = "simulate_info/tags";

    public static final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif BLOCKS_ID = UnloadedActivity.id("simulate_blocks");
    public static final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif TAGS_ID = UnloadedActivity.id("simulate_tags");

    public final boolean isBlocks;

    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<JsonObject>> TAG_MAP = new HashMap<>();
    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<JsonObject>> BLOCK_MAP = new HashMap<>();
    public static final Int2ObjectOpenHashMap<SimulationData> COMPLETE_BLOCK_MAP = new Int2ObjectOpenHashMap<>();

    public SimulationDataResource(boolean isBlocks) {
        super(
            #if MC_VER >= MC_1_21_4
            FileToIdConverter.json(isBlocks ? BLOCKS_LOCATION : TAGS_LOCATION)
            #else
            isBlocks ? BLOCKS_LOCATION : TAGS_LOCATION
            #endif
        );
        this.isBlocks = isBlocks;
    }

    @Override
    protected void apply(
        Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<JsonObject>> objects,
        ResourceManager resourceManager,
        ProfilerFiller profilerFiller
    ) {
        if (this.isBlocks) {
            BLOCK_MAP.clear();
            BLOCK_MAP.putAll(objects);
            UnloadedActivity.LOGGER.info("Block entries: " + BLOCK_MAP.keySet());
        } else {
            TAG_MAP.clear();
            TAG_MAP.putAll(objects);
            UnloadedActivity.LOGGER.info("Tag entries: " + TAG_MAP.keySet());
        }

        COMPLETE_BLOCK_MAP.clear();

        /*
        for (Map.Entry<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<IncompleteSimulationData>> tagEntry : datas.entrySet()) {
            List<IncompleteSimulationData> dataList = tagEntry.getValue();

            if (dataList.isEmpty())
                continue;

            var id = tagEntry.getKey();
            IncompleteSimulationData finalSimulationData = new IncompleteSimulationData();

            for (IncompleteSimulationData simulationData : dataList) {
                finalSimulationData.merge(simulationData);
            }

            if (this.isBlocks) {
                BLOCK_MAP.put(id, finalSimulationData);
            } else {
                TAG_MAP.put(id, finalSimulationData);
            }
        }
         */
    }

    public static void clearSimulationDatas() {
        COMPLETE_BLOCK_MAP.clear();
    }

    public static void clearRawSimulationDatas() {
        BLOCK_MAP.clear();
        TAG_MAP.clear();
    }

    public static void buildSimulationDatas() {
        clearSimulationDatas();

        HashSet<Block> blocksToBuild = new HashSet<>();

        for (var blockId : BLOCK_MAP.keySet()) {
            var maybeBlockHolder = BuiltInRegistries.BLOCK.get(blockId);
            if (maybeBlockHolder.isEmpty()) continue;
            blocksToBuild.add(maybeBlockHolder.get().value());
        }

        BuiltInRegistries.BLOCK.listTags().forEach(named -> {
            if (!TAG_MAP.containsKey(named.key().location())) return;
            named.forEach(blockHolder -> blocksToBuild.add(blockHolder.value()));
        });

        for (Block block : blocksToBuild) {
            if (block == Blocks.BEETROOTS) {
                var uea = true;
            }
            var blockId = GameUtils.getBlockId(block);
            ArrayList<JsonObject> sortedBlockData = new ArrayList<>(BLOCK_MAP.getOrDefault(blockId, List.of()));
            sortedBlockData.sort(SimulationDataResource::compareJsonPriority);

            ArrayList<JsonObject> sortedTagData = new ArrayList<>();

            block.defaultBlockState().tags().forEach(tag -> {
                var tagId = tag.location();

                sortedTagData.addAll(TAG_MAP.getOrDefault(tagId, List.of()));
            });

            sortedTagData.sort(SimulationDataResource::compareJsonPriority);

            ArrayList<JsonObject> sortedData = new ArrayList<>(sortedTagData);
            sortedData.addAll(sortedBlockData);

            try {
                COMPLETE_BLOCK_MAP.put(GameUtils.getBlockIntId(block), new SimulationData(sortedData));
            } catch (Exception e) {
                UnloadedActivity.LOGGER.error("Failed to create SimulationData for " + blockId + ".\n" + e.getMessage());
            }
        }

        clearRawSimulationDatas();
    }

    public static Optional<SimulationData> getSimulationData(Block block) {
        return Optional.ofNullable(COMPLETE_BLOCK_MAP.get(GameUtils.getBlockIntId(block)));
    }

    public static int compareJsonPriority(JsonObject a, JsonObject b) {
        JsonElement aJsonPriority = a.getAsJsonObject().get("priority");
        JsonElement bJsonPriority = b.getAsJsonObject().get("priority");

        JsonPrimitive aPrimitive = (aJsonPriority != null && aJsonPriority.isJsonPrimitive()) ? aJsonPriority.getAsJsonPrimitive() : null;
        JsonPrimitive bPrimitive = (bJsonPriority != null && bJsonPriority.isJsonPrimitive()) ? bJsonPriority.getAsJsonPrimitive() : null;

        double aPriority = (aPrimitive != null && aPrimitive.isNumber()) ? aPrimitive.getAsDouble() : 1000;
        double bPriority = (bPrimitive != null && bPrimitive.isNumber()) ? bPrimitive.getAsDouble() : 1000;
        return Double.compare(aPriority, bPriority);
    }
}
