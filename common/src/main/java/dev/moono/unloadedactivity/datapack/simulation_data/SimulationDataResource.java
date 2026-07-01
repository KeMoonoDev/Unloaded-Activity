package dev.moono.unloadedactivity.datapack.simulation_data;

#if MC_VER >= MC_1_21_4
import net.minecraft.resources.FileToIdConverter;
#endif

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import dev.moono.unloadedactivity.datapack.JsonResourcesCollector;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.UnloadedActivity;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class SimulationDataResource extends JsonResourcesCollector {

    private static final String BLOCKS_LOCATION = "simulate_info/blocks";
    private static final String TAGS_LOCATION = "simulate_info/tags";

    public static final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif BLOCKS_ID = UnloadedActivity.id("simulate_blocks");
    public static final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif TAGS_ID = UnloadedActivity.id("simulate_tags");

    public final boolean isBlocks;

    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<JsonObject>> TAG_MAP = new HashMap<>();
    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<JsonObject>> BLOCK_MAP = new HashMap<>();
    public static final Reference2ObjectOpenHashMap<Block, SimulationData> COMPLETE_BLOCK_MAP = new Reference2ObjectOpenHashMap<>();

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
    }

    public static void clearAllSimulationData() {
        COMPLETE_BLOCK_MAP.clear();
    }

    public static void clearAllRawSimulationData() {
        BLOCK_MAP.clear();
        TAG_MAP.clear();
    }

    public static void buildAllSimulationData() {
        clearAllSimulationData();

        HashSet<Block> blocksToBuild = new HashSet<>();

        for (var blockId : BLOCK_MAP.keySet()) {
            Block block = GameUtils.getBlock(blockId);
            if (block == null) continue;
            blocksToBuild.add(block);
        }

        GameUtils.getBlockRegistry()
            #if MC_VER >= MC_1_21_11
            .listTags()
            #else
            .getTags() #if MC_VER < MC_1_21_3 .map(Pair::getSecond) #endif
            #endif
            .forEach(named -> {
                if (!TAG_MAP.containsKey(named.key().location())) return;
                named.forEach(blockHolder -> blocksToBuild.add(blockHolder.value()));
            }
        );

        for (Block block : blocksToBuild) {
            var blockId = GameUtils.getBlockId(block);
            ArrayList<JsonObject> sortedBlockData = new ArrayList<>(BLOCK_MAP.getOrDefault(blockId, List.of()));
            sortedBlockData.sort(SimulationDataResource::compareJsonPriority);

            ArrayList<JsonObject> sortedTagData = new ArrayList<>();

            GameUtils.getBlockTags(block).forEach(tag -> {
                var tagId = tag.location();
                sortedTagData.addAll(TAG_MAP.getOrDefault(tagId, List.of()));
            });

            sortedTagData.sort(SimulationDataResource::compareJsonPriority);

            ArrayList<JsonObject> sortedData = new ArrayList<>(sortedTagData);
            sortedData.addAll(sortedBlockData);

            try {
                COMPLETE_BLOCK_MAP.put(block, new SimulationData(sortedData, block));
            } catch (Exception e) {
                UnloadedActivity.LOGGER.error("Failed to create SimulationData for " + blockId + ".\n" + e.getMessage());
            }
        }

        clearAllRawSimulationData();
    }

    public static Optional<SimulationData> getSimulationData(Block block) {
        return Optional.ofNullable(COMPLETE_BLOCK_MAP.get(block));
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
