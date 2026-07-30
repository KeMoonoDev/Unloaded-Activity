package dev.moono.unloadedactivity.datapack.group;

#if MC_VER >= MC_1_21_4
import net.minecraft.resources.FileToIdConverter;
#endif

import dev.moono.unloadedactivity.datapack.JsonResourcesCollector;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationDataResource;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.UnloadedActivity;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class GroupInfoResource extends JsonResourcesCollector {

    private static final String GROUPS_LOCATION = "simulate_info/groups";

    public static final List<GroupMemberInfo> EMPTY_MEMBER_INFO_LIST = List.of();

    public static final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif GROUPS_ID = UnloadedActivity.id("simulate_groups");

    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, GroupInfo> GROUPS_MAP = new HashMap<>();
    public static final Reference2ObjectOpenHashMap<Block, List<GroupMemberInfo>> BLOCK_MEMBERSHIPS = new Reference2ObjectOpenHashMap<>();
    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<JsonObject>> UNPARSED_GROUPS_MAP = new HashMap<>();
    //public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, ArrayList<Pair<GroupInfo, IncompleteGroupMemberInfo>>> BLOCKS_WITH_GROUPS_MAP = new HashMap<>();
    //public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, ArrayList<Pair<GroupInfo, IncompleteGroupMemberInfo>>> TAGS_WITH_GROUPS_MAP = new HashMap<>();

    public GroupInfoResource() {
        super(
            #if MC_VER >= MC_1_21_4
            FileToIdConverter.json(GROUPS_LOCATION)
            #else
            GROUPS_LOCATION
            #endif
        );
    }

    @Override
    protected void apply(
            Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<JsonObject>> objects,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        UNPARSED_GROUPS_MAP.clear();
        UNPARSED_GROUPS_MAP.putAll(objects);
    }

    public static void clearAllGroupInfos() {
        GROUPS_MAP.clear();
        BLOCK_MEMBERSHIPS.clear();
    }

    public static void clearAllRawGroupInfos() {
        UNPARSED_GROUPS_MAP.clear();
    }

    public static void buildAllGroupInfos() {
        clearAllGroupInfos();

        HashMap<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, ArrayList<GroupMemberInfo>> tagMemberInfos = new HashMap<>();
        HashMap<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, ArrayList<GroupMemberInfo>> blockMemberInfos = new HashMap<>();

        UNPARSED_GROUPS_MAP.forEach((key, list) -> {
            ArrayList<JsonObject> sortedGroupInfos = new ArrayList<>(list);
            sortedGroupInfos.sort(SimulationDataResource::compareJsonPriority);

            int startIndex = 0;

            for (int i = sortedGroupInfos.size() - 1; i >= 0; i--) {
                JsonObject object = sortedGroupInfos.get(i);
                JsonElement jsonReplace = object.get("replace");

                if (jsonReplace != null && jsonReplace.isJsonPrimitive() && jsonReplace.getAsBoolean()) {
                    startIndex = i;
                    break;
                }
            }

            List<JsonObject> mergingData = sortedGroupInfos.subList(startIndex, sortedGroupInfos.size());

            IncompleteGroupInfo incompleteGroupInfo = new IncompleteGroupInfo();

            for (JsonObject unparsedGroupInfo : mergingData) {
                IncompleteGroupInfo parsed = IncompleteGroupInfo.parse(unparsedGroupInfo);
                incompleteGroupInfo.merge(parsed);
            }

            GroupInfo groupInfo = new GroupInfo(key, incompleteGroupInfo);

            HashMap<Pair<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, Boolean>, GroupMemberInfo> memberInfos = new HashMap<>();

            for (var entry : incompleteGroupInfo.values.entrySet()) {
                memberInfos.put(entry.getKey(), new GroupMemberInfo(entry.getValue(), groupInfo));
            }

            GROUPS_MAP.put(key, groupInfo);

            for (var entry : memberInfos.entrySet()) {
                var pair = entry.getKey();
                boolean isTag = pair.getSecond();
                var id = pair.getFirst();

                if (isTag) {
                    tagMemberInfos.computeIfAbsent(id, unused -> new ArrayList<>())
                        .add(entry.getValue());
                } else {
                    blockMemberInfos.computeIfAbsent(id, unused -> new ArrayList<>())
                        .add(entry.getValue());
                }
            }
        });



        GameUtils.getBlockRegistry()
            #if MC_VER >= MC_1_21_11
            .listTags()
            #else
            .getTags() #if MC_VER < MC_1_21_3 .map(Pair::getSecond) #endif
            #endif
            .forEach(named -> {
                var tagKey = named.key().location();
                ArrayList<GroupMemberInfo> memberInfos = tagMemberInfos.get(tagKey);
                if (memberInfos == null) return;
                named.forEach(
                    blockHolder ->
                        BLOCK_MEMBERSHIPS
                            .computeIfAbsent(blockHolder.value(), unused -> new ArrayList<>())
                            .addAll(memberInfos)
                );
            }
        );

        for (var entry : blockMemberInfos.entrySet()) {
            var blockId = entry.getKey();
            ArrayList<GroupMemberInfo> memberInfos = entry.getValue();
            BLOCK_MEMBERSHIPS
                .computeIfAbsent(GameUtils.getBlock(blockId), unused -> new ArrayList<>())
                .addAll(memberInfos);

        }

        UnloadedActivity.LOGGER.info("Group entries: " + GROUPS_MAP.keySet());
    }

    public static List<GroupMemberInfo> getBlockMemberInfo(Block block) {
        return BLOCK_MEMBERSHIPS.getOrDefault(block, EMPTY_MEMBER_INFO_LIST);
    }
}
