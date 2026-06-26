package dev.moono.unloadedactivity.datapack.group;

#if MC_VER >= MC_1_21_11
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif
#if MC_VER >= MC_1_21_4
import net.minecraft.resources.FileToIdConverter;
#endif

import com.mojang.datafixers.util.Pair;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.UnloadedActivity;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class GroupInfoResource extends SimpleJsonResourceReloadListener #if MC_VER >= MC_1_21_3
<IncompleteGroupInfo>
#endif {

    private static final String GROUPS_LOCATION = "simulate_info/groups";

    public static final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif GROUPS_ID = UnloadedActivity.id("simulate_groups");

    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, GroupInfo> GROUPS_MAP = new HashMap<>();
    public static final Reference2ObjectOpenHashMap<Block, List<GroupMemberInfo>> BLOCK_MEMBERSHIPS = new Reference2ObjectOpenHashMap<>();
    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, ArrayList<Pair<GroupInfo, IncompleteGroupMemberInfo>>> BLOCKS_WITH_GROUPS_MAP = new HashMap<>();
    public static final Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, ArrayList<Pair<GroupInfo, IncompleteGroupMemberInfo>>> TAGS_WITH_GROUPS_MAP = new HashMap<>();

    #if MC_VER >= MC_1_21_3
    public GroupInfoResource() {
        super(
            IncompleteGroupInfo.CODEC,
            #if MC_VER >= MC_1_21_4
            FileToIdConverter.json(GROUPS_LOCATION)
            #else
            GROUPS_LOCATION
            #endif
        );
    }
    #else
    public GroupInfoResource() {
        super(new GsonBuilder().create(), GROUPS_LOCATION);
    }
    #endif

    #if MC_VER >= MC_1_21_3
    @Override
    protected void apply(
            Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, IncompleteGroupInfo> object,
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
        GROUPS_MAP.clear();
        BLOCK_MEMBERSHIPS.clear();
        BLOCKS_WITH_GROUPS_MAP.clear();
        TAGS_WITH_GROUPS_MAP.clear();

        Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<IncompleteGroupInfo>> datas = new HashMap<>();

        object.forEach((key, input) -> {
            try {
                #if MC_VER >= MC_1_21_3
                IncompleteGroupInfo incompleteGroupInfo = input;
                #else
                var result = IncompleteGroupInfo.parse(JsonOps.INSTANCE, input);
                if (result.error().isPresent()) {
                    throw new RuntimeException(result.error().get().message());
                }
                IncompleteGroupInfo incompleteGroupInfo = result.result().get();
                #endif

                var list = datas.computeIfAbsent(key, k -> new ArrayList<>());
                list.add(incompleteGroupInfo);
            } catch(Exception e) {
                UnloadedActivity.LOGGER.error("{}\n{}\n{}", key, e, e.getStackTrace());
            }
        });

        for (var entry : datas.entrySet()) {
            List<IncompleteGroupInfo> dataList = entry.getValue();

            if (dataList.isEmpty())
                continue;

            var groupId = entry.getKey();
            IncompleteGroupInfo finalGroupInfo = new IncompleteGroupInfo();

            for (IncompleteGroupInfo groupInfo : dataList) {
                finalGroupInfo.merge(groupInfo);
            }

            GroupInfo groupInfo = new GroupInfo(groupId, finalGroupInfo);

            GROUPS_MAP.put(groupId, groupInfo);

            for (var valueEntry : groupInfo.values.entrySet()) {
                var key = valueEntry.getKey();
                var id = key.getFirst();
                boolean isTag = key.getSecond();

                IncompleteGroupMemberInfo memberInfo = valueEntry.getValue();

                if (isTag) {
                    var groupInfos = TAGS_WITH_GROUPS_MAP.computeIfAbsent(id, (ignored) -> new ArrayList<>());
                    groupInfos.add(Pair.of(groupInfo, memberInfo));
                } else {
                    var groupInfos = BLOCKS_WITH_GROUPS_MAP.computeIfAbsent(id, (ignored) -> new ArrayList<>());
                    groupInfos.add(Pair.of(groupInfo, memberInfo));
                }
            }
        }

        UnloadedActivity.LOGGER.info("Group entries: " + GROUPS_MAP.keySet());
    }

    public static List<GroupMemberInfo> getBlockMemberInfo(Block block) {
        return BLOCK_MEMBERSHIPS.computeIfAbsent(block, (ignored) -> {
            HashMap<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, ArrayList<IncompleteGroupMemberInfo>> dataToBeCombined = new HashMap<>();
            block.builtInRegistryHolder().tags().forEach((tag) -> {
                for (var tagGroupData : TAGS_WITH_GROUPS_MAP.getOrDefault(tag.location(), new ArrayList<>())) {
                    ArrayList<IncompleteGroupMemberInfo> dataList = dataToBeCombined.computeIfAbsent(tagGroupData.getFirst().id, (ignored2) -> new ArrayList<>());
                    dataList.add(tagGroupData.getSecond());
                }
            });

            var blockId = GameUtils.getBlockId(block);

            for (var blockGroupData : BLOCKS_WITH_GROUPS_MAP.getOrDefault(blockId, new ArrayList<>())) {
                ArrayList<IncompleteGroupMemberInfo> dataList = dataToBeCombined.computeIfAbsent(blockGroupData.getFirst().id, (ignored2) -> new ArrayList<>());
                dataList.add(blockGroupData.getSecond());
            }

            ArrayList<GroupMemberInfo> memberships = new ArrayList<>();

            for (var entry : dataToBeCombined.entrySet()) {
                var groupId = entry.getKey();
                ArrayList<IncompleteGroupMemberInfo> dataList = entry.getValue();
                IncompleteGroupMemberInfo finalMemberInfo = new IncompleteGroupMemberInfo();
                for (var dataToMerge : dataList) {
                    finalMemberInfo.merge(dataToMerge);
                }

                GroupInfo groupInfo = GROUPS_MAP.get(groupId);

                memberships.add(new GroupMemberInfo(finalMemberInfo, groupInfo));
            }

            return memberships;
        });
    }
}
