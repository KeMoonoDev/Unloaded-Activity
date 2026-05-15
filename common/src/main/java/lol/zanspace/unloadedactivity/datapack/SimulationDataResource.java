package lol.zanspace.unloadedactivity.datapack;

#if MC_VER >= MC_1_21_11
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public class SimulationDataResource extends SimpleJsonResourceReloadListener #if MC_VER >= MC_1_21_3
<IncompleteSimulationData>
#endif {

    private static final String BLOCKS_LOCATION = "simulate_info/blocks";
    private static final String TAGS_LOCATION = "simulate_info/tags";

    public static final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif BLOCKS_ID = UnloadedActivity.id("simulate_blocks");
    public static final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif TAGS_ID = UnloadedActivity.id("simulate_tags");

    public final boolean isBlocks;

    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, IncompleteSimulationData> TAG_MAP = new HashMap<>();
    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, IncompleteSimulationData> BLOCK_MAP = new HashMap<>();
    public static final Int2ObjectOpenHashMap<SimulationData> COMPLETE_BLOCK_MAP = new Int2ObjectOpenHashMap<>();

    #if MC_VER >= MC_1_21_3
    public SimulationDataResource(boolean isBlocks) {
        super(
            IncompleteSimulationData.CODEC,
            #if MC_VER >= MC_1_21_4
            FileToIdConverter.json(isBlocks ? BLOCKS_LOCATION : TAGS_LOCATION)
            #else
            isBlocks ? BLOCKS_LOCATION : TAGS_LOCATION
            #endif
        );
        this.isBlocks = isBlocks;
    }
    #else
    public SimulationDataResource(boolean isBlocks) {
        super(new GsonBuilder().create(), isBlocks ? BLOCKS_LOCATION : TAGS_LOCATION);
        this.isBlocks = isBlocks;
    }
    #endif

    #if MC_VER >= MC_1_21_3
    @Override
    protected void apply(
            Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, IncompleteSimulationData> object,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    )
    #else
    @Override
    protected void apply(
        Map<
            #if MC_VER >= MC_1_21_11
            Identifier
            #else
            ResourceLocation
            #endif,
            JsonElement
        > object,
        ResourceManager resourceManager,
        ProfilerFiller profilerFiller
    )
    #endif
    {

        if (this.isBlocks) {
            BLOCK_MAP.clear();
        } else {
            TAG_MAP.clear();
        }

        COMPLETE_BLOCK_MAP.clear();

        Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<IncompleteSimulationData>> datas = new HashMap<>();

        object.forEach((key, input) -> {
            try {
                #if MC_VER >= MC_1_21_3
                IncompleteSimulationData incompleteSimulationData = input;
                #else
                var result = IncompleteSimulationData.CODEC.decode(com.mojang.serialization.JsonOps.INSTANCE, input);
                if (result.error().isPresent()) {
                    throw new RuntimeException(result.error().get().message());
                }
                IncompleteSimulationData incompleteSimulationData = result.result().get().getFirst();
                #endif

                var list = datas.computeIfAbsent(key, k -> new ArrayList<>());
                list.add(incompleteSimulationData);
            } catch(Exception e) {
                UnloadedActivity.LOGGER.error("{}\n{}\n{}", key, e, e.getStackTrace());
            }
        });

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

        if (this.isBlocks) {
            UnloadedActivity.LOGGER.info("Block entries: " + BLOCK_MAP.keySet());
        } else {
            UnloadedActivity.LOGGER.info("Tag entries: " + TAG_MAP.keySet());
        }
    }
}
