package dev.moono.unloadedactivity;


import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.context.UpdatingContext;
import dev.moono.unloadedactivity.api.simulation_method.SimulationMethod;
import dev.moono.unloadedactivity.api.simulation_method.GroupableSimulationMethod;
import dev.moono.unloadedactivity.api.weather_history.WeatherHistory;
import dev.moono.unloadedactivity.api.weather_history.WeatherMsHistory;
import dev.moono.unloadedactivity.api.weather_history.WeatherTickHistory;
import dev.moono.unloadedactivity.datapack.group.GroupInfo;
import dev.moono.unloadedactivity.datapack.group.GroupInfoResource;
import dev.moono.unloadedactivity.datapack.group.GroupMemberInfo;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationData;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationDataResource;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;

public class TimeMachine {
    /// Returns how many groups were simulated and if the last ticked time should be updated/if normal ticks were simulated.
    public static Pair<Integer, Boolean> simulateChunk(ServerLevel level, LevelChunk chunk, SimulatedTime simulatedTime, int randomTickSpeed, int groupUpdateBudget) {
        if (!UnloadedActivity.config.enableSimulatingRandomTicks
            && !UnloadedActivity.config.enableSimulatingPrecipitationTicks
            && !UnloadedActivity.config.enableSimulatingGroups) return Pair.of(0, true);

        int simulatedGroupCount = 0;

        if (UnloadedActivity.config.enableSimulatingGroups) {
            Pair<Integer, Boolean> result = TimeMachine.simulateGroupTicks(level, chunk, randomTickSpeed, groupUpdateBudget, simulatedTime);
            simulatedGroupCount = result.getFirst();
            boolean simulatedAllGroups = result.getSecond();
            if (!simulatedAllGroups) {
                return result;
            }
        }

        if (UnloadedActivity.config.enableSimulatingRandomTicks || UnloadedActivity.config.enableSimulatingPrecipitationTicks)
            TimeMachine.simulateTicks(level, chunk, simulatedTime, randomTickSpeed);

        return Pair.of(simulatedGroupCount, true);
    }

    public static boolean isChunkIndexed(LevelChunk chunk) {
        return chunk.getSimulationVersion() == UnloadedActivity.chunkSimVer;
    }

    public static void indexChunk(LevelChunk chunk) {
        MinecraftServer server = chunk.getLevel().getServer();

        if (server == null) {
            throw new RuntimeException("The method indexChunk got run on the client side.");
        }

        if (isChunkIndexed(chunk))
            return;

        ArrayList<Long> newSimulationBlocks = new ArrayList<>();
        HashMap<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, GroupChunkIndex> newGroupIndexes = new HashMap<>();

        if (UnloadedActivity.config.debugLogs)
            UnloadedActivity.LOGGER.info("Looping through entire chunk.");

        ChunkPos chunkPos = chunk.getPos();

        LevelChunkSection[] sections = chunk.getSections();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];

            boolean simulateChunk = section.maybeHas((state) -> {
                Block block = state.getBlock();
                Optional<SimulationData> maybeSimulationData = SimulationDataResource.getSimulationData(block);
                if (maybeSimulationData.isPresent() && maybeSimulationData.get().hasRandTicksWithoutGroup) {
                    return true;
                }

                List<GroupMemberInfo> memberInfoList = GroupInfoResource.getBlockMemberInfo(block);
                return !memberInfoList.isEmpty();
            });

            if (!simulateChunk) {
                continue;
            }

            int sectionBlockY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex));

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos levelBlockPos = chunkPos.getBlockAt(x, sectionBlockY + y, z);
                        BlockState state = section.getBlockState(x, y, z);
                        Block block = state.getBlock();
                        Optional<SimulationData> maybeSimulationData = SimulationDataResource.getSimulationData(block);
                        if (maybeSimulationData.isPresent() && maybeSimulationData.get().hasRandTicksWithoutGroup) {
                            newSimulationBlocks.add(levelBlockPos.asLong());
                        }

                        List<GroupMemberInfo> memberInfoList = GroupInfoResource.getBlockMemberInfo(block);

                        if (!memberInfoList.isEmpty()) {
                            for (var memberInfo : memberInfoList) {
                                var groupId = memberInfo.groupInfo.id;
                                if (UnloadedActivity.config.debugLogs)
                                    UnloadedActivity.LOGGER.info("Adding position to group list " + groupId + " " + levelBlockPos.asLong());

                                var positions = newGroupIndexes
                                        .computeIfAbsent(groupId, (id) -> new GroupChunkIndex(new ArrayList<>(), chunk.getLastTick(), chunk.getLastMs(), id))
                                        .getPositions();

                                positions.add(levelBlockPos.asLong());
                            }
                        }
                    }
                }
            }
        }

        chunk.setSimulationBlocks(newSimulationBlocks);
        chunk.setGroupIndexes(new ArrayList<>(newGroupIndexes.values()));
        chunk.setSimulationVersion(UnloadedActivity.chunkSimVer);
        #if MC_VER >= MC_1_21_3
        chunk.markUnsaved();
        #else
        chunk.setUnsaved(true);
        #endif
    }

    /// Returns positions of blocks that implement random ticks.
    public static ArrayList<BlockPos> getRandomTickableBlocks(LevelChunk chunk) {
        MinecraftServer server = chunk.getLevel().getServer();

        if (server == null) {
            throw new RuntimeException("The method getRandomTickableBlocks got run on the client side.");
        }

        ArrayList<Long> currentSimulationBlocks = chunk.getSimulationBlocks();

        ArrayList<BlockPos> blockPosArray = new ArrayList<>(currentSimulationBlocks.size());

        if (UnloadedActivity.config.debugLogs)
            UnloadedActivity.LOGGER.info("Looping through "+currentSimulationBlocks.size()+" known positions.");

        int prevSize = currentSimulationBlocks.size();

        currentSimulationBlocks.removeIf((longPos) -> {
            BlockPos pos = BlockPos.of(longPos);
            BlockState state = chunk.getBlockState(pos);
            Block block = state.getBlock();
            Optional<SimulationData> simulationData = SimulationDataResource.getSimulationData(block);

            if (simulationData.isEmpty()) {
                return true;
            }

            if (simulationData.get().hasRandTicksWithoutGroup) {
                blockPosArray.add(pos);
                return false;
            } else {
                return true;
            }
        });

        int removedCount = prevSize - currentSimulationBlocks.size();

        if (removedCount > 0) {
            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Removed "+ removedCount +" positions.");
        }

        return blockPosArray;
    }

    /// Returns positions of blocks that implement precipitation ticks.
    public static List<BlockPos> getPrecipitationTickableBlocks(LevelChunk chunk) {
        Level level = chunk.getLevel();
        MinecraftServer server = level.getServer();

        if (server == null) {
            throw new RuntimeException("The method getPrecipitationTickableBlocks got run on the client side.");
        }

        ArrayList<BlockPos> precipitationBlocks = new ArrayList<>();

        for (int z=0; z<16;z++)
            for (int x=0; x<16;x++) {
                ChunkPos chunkPos = chunk.getPos();
                BlockPos airPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(chunkPos.getMinBlockX()+x,0,chunkPos.getMinBlockZ()+z));
                BlockPos groundPos = airPos.below();
                BlockState airPosState = chunk.getBlockState(airPos);
                BlockState groundPosState = chunk.getBlockState(groundPos);
                Block airPosBlock = airPosState.getBlock();
                Block groundPosBlock = groundPosState.getBlock();

                if (SimulationDataResource.getSimulationData(airPosBlock).map(data -> data.hasPrecTicksWithoutGroup).orElse(false))
                    precipitationBlocks.add(airPos);

                if (SimulationDataResource.getSimulationData(groundPosBlock).map(data -> data.hasPrecTicksWithoutGroup).orElse(false))
                    precipitationBlocks.add(groundPos);
            }

        return precipitationBlocks;
    }

    // This doesn't take a timeDifference parameter because that is supposed to be calculated in the function using the last group tick.
    public static Pair<Integer, Boolean> simulateGroupTicks(ServerLevel level, LevelChunk chunk, int randomTickSpeed, int groupUpdateBudget, SimulatedTime simulatedTime) {
        MinecraftServer server = chunk.getLevel().getServer();

        if (server == null) {
            throw new RuntimeException("The method simulateGroupTicks got run on the client side.");
        }

        ArrayList<GroupChunkIndex> groupIndexes = chunk.getGroupIndexes();

        int simulatedGroups = 0;

        boolean missedGroup = false;

        for (GroupChunkIndex groupChunkIndex : groupIndexes) {
            var groupId = groupChunkIndex.groupId;
            GroupInfo groupInfo = GroupInfoResource.GROUPS_MAP.get(groupId);

            if (groupInfo == null)
                continue;

            long tickDifference = 0;

            if (UnloadedActivity.config.useSystemTime) {
                long lastMs = groupChunkIndex.getLastMs(chunk.getLastMs());
                if (lastMs > 0) {
                    // 50ms per tick
                    tickDifference = Math.max(simulatedTime.endMs() - lastMs, 0) / 50;
                }
            } else {
                long lastTick = groupChunkIndex.getLastTick(chunk.getLastTick());
                if (lastTick > 0) {
                    tickDifference = Math.max(simulatedTime.endTick() - lastTick, 0);
                }
            }

            if (tickDifference <= UnloadedActivity.config.groupTickDifferenceThreshold) {
                groupChunkIndex.setLastTick(simulatedTime.endTick());
                groupChunkIndex.setLastMs(simulatedTime.endMs());
                continue;
            }

            ArrayList<ActiveGroupSimulateData> checkingBlockPositions = groupChunkIndex.getAndFilterBlocks(chunk);

            boolean isAllInactive = true;

            for (var groupSimulateData : checkingBlockPositions) {
                if (groupSimulateData.isActive) {
                    isAllInactive = false;
                    break;
                }
            }

            if (isAllInactive) {
                groupChunkIndex.setLastTick(simulatedTime.endTick());
                groupChunkIndex.setLastMs(simulatedTime.endMs());
                #if MC_VER >= MC_1_21_3
                chunk.markUnsaved();
                #else
                chunk.setUnsaved(true);
                #endif
                continue;
            }

            if (simulatedGroups >= groupUpdateBudget) {
                // we want find all the groups where groupTimeDifference is not enough so we can update the last tick.
                missedGroup = true;
                continue;
            }

            simulatedGroups++;

            Optional<Collection<ActiveGroupSimulateData>> maybeActiveGroupDataMap = generateActiveGroupDataMap(level, chunk, checkingBlockPositions, groupInfo, simulatedTime);

            if (maybeActiveGroupDataMap.isEmpty()) {
                break;
            }

            Collection<ActiveGroupSimulateData> activeGroupDataMap = maybeActiveGroupDataMap.get();

            // Separate them into isolated groups.
            List<List<ActiveGroupSimulateData>> isolatedGroups = separateToIsolatedGroups(activeGroupDataMap);

            RandomSource random = level.getRandom();
            float randomPickProbability = MathUtils.getRandomPickProbability(randomTickSpeed);
            float precipitationPickProbability = MathUtils.getPrecipitationPickProbability(randomTickSpeed);

            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Simulating " + isolatedGroups.size() + " isolated groups");

            // Data has been made. Time to actually do the simulation.
            for (List<ActiveGroupSimulateData> group : isolatedGroups) {
                int totalIterations = 0;
                SimulatedTime activeSimulatedTime = simulatedTime;

                if (UnloadedActivity.config.debugLogs)
                    UnloadedActivity.LOGGER.info("Simulating isolated group of " + group.size() + " members");

                while (activeSimulatedTime.remainingTicks() > 0 && totalIterations < UnloadedActivity.config.maxGroupTickIterations) {
                    long remainingTicks = activeSimulatedTime.remainingTicks();

                    long minProbabilityStepTickDuration = remainingTicks / (UnloadedActivity.config.maxGroupTickIterations - totalIterations);
                    long maxProbabilityStepTickDuration = remainingTicks / Math.max(1, UnloadedActivity.config.minGroupTickIterations - totalIterations);
                    totalIterations++;

                    long minNextProbabilitySwitchTickDuration = Long.MAX_VALUE;
                    float maxProbability = 0F;

                    long nextWeatherSwitchTickDuration = GameUtils.nextWeatherSwitchTickDuration(level, simulatedTime);

                    for (ActiveGroupSimulateData simulationData : group) {
                        if (!simulationData.isActive)
                            continue;

                        // For isActive to return true, there must be a simulationMethod present.
                        SimulationMethod simulationMethod = simulationData.getSimulationMethod().orElseThrow();

                        float pickProbability;

                        if (simulationMethod.isPrecipitation) {
                            pickProbability = precipitationPickProbability;
                        } else {
                            pickProbability = randomPickProbability;
                        }

                        BlockState state = simulationData.getState();
                        BlockPos pos = simulationData.position;

                        UpdatingContext context = UpdatingContext.of(level, state, pos, activeSimulatedTime, simulationData);

                        Pair<Float, Long> oddsAndDuration = simulationData.updateAndGetProbability(nextWeatherSwitchTickDuration, context);

                        minNextProbabilitySwitchTickDuration = Math.min(minNextProbabilitySwitchTickDuration, oddsAndDuration.getSecond());

                        float probability = oddsAndDuration.getFirst() * pickProbability;

                        maxProbability = Math.max(probability, maxProbability);
                    }

                    if (maxProbability <= 0.0) {
                        if (minNextProbabilitySwitchTickDuration >= remainingTicks) {
                            break;
                        } else {
                            activeSimulatedTime = activeSimulatedTime.passTicks(minNextProbabilitySwitchTickDuration);
                            for (ActiveGroupSimulateData simulationData : group) {
                                if (!simulationData.isActive)
                                    continue;
                                simulationData.passTicks(minNextProbabilitySwitchTickDuration);
                            }
                            continue;
                        }
                    }

                    long probabilityTickDuration = (long)Math.ceil((1.0 / maxProbability) * UnloadedActivity.config.groupTickUpdateStrength);
                    probabilityTickDuration = Math.min(maxProbabilityStepTickDuration, probabilityTickDuration);
                    probabilityTickDuration = Math.max(minProbabilityStepTickDuration, probabilityTickDuration);

                    long simulationStepTickDuration = Math.min(Math.min(minNextProbabilitySwitchTickDuration, probabilityTickDuration), remainingTicks);

                    ArrayList<Triple<BlockState, ActiveGroupSimulateData, Optional<GroupMemberInfo>>> pendingUpdateBlockInfo = new ArrayList<>();

                    SimulatedTime subSimulatedTime = simulatedTime.subTicks(simulationStepTickDuration);

                    for (ActiveGroupSimulateData simulationData : group) {
                        if (!simulationData.isActive)
                            continue;

                        simulationData.passTicks(simulationStepTickDuration);

                        // For isActive to return true, there must be a simulationMethod present.
                        GroupableSimulationMethod simulationMethod = simulationData.getSimulationMethod().orElseThrow();

                        BlockState state = simulationData.getState();
                        Block block = state.getBlock();

                        float pickProbability;

                        if (simulationMethod.isPrecipitation) {
                            pickProbability = precipitationPickProbability;
                        } else {
                            pickProbability = randomPickProbability;
                        }

                        int remainingUpdates = simulationData.getRemainingUpdates();

                        if (remainingUpdates > 0) {
                            float totalProbability = simulationData.currentProbability * pickProbability;
                            int occurrences = MathUtils.getOccurrencesSimple(simulationStepTickDuration, totalProbability, remainingUpdates, random);
                            simulationData.addUpdateCount(occurrences);
                        }

                        remainingUpdates = simulationData.getRemainingUpdates();

                        if (remainingUpdates > 0) {
                            continue;
                        }

                        int updateCount = simulationData.getCurrentUpdateCount();

                        DeferredBlockPlacer.SingleBlockPlacement singleBlockPlacement = simulationMethod.getNewBlockState(state, level, simulationData.position, OccurrencesAndTimings.fastDuration(updateCount, subSimulatedTime), simulationData);

                        simulationData.placeBlock = true;

                        BlockState newBlockState = singleBlockPlacement.blockState();
                        Block newBlock = newBlockState.getBlock();
                        if (newBlock == block) {
                            simulationData.isActive = false;
                            pendingUpdateBlockInfo.add(Triple.of(newBlockState, simulationData, Optional.of(simulationData.getGroupMemberInfo())));
                            simulationData.updateType = singleBlockPlacement.updateType();
                            continue;
                        }

                        simulationData.blockIsReplaced = true;

                        Optional<GroupMemberInfo> maybeGroupMemberInfo = GroupInfoResource.getBlockMemberInfo(newBlock)
                            .stream()
                            .filter(info -> info.groupInfo == groupInfo)
                            .findFirst();

                        pendingUpdateBlockInfo.add(Triple.of(newBlockState, simulationData, maybeGroupMemberInfo));
                    }

                    for (var triple : pendingUpdateBlockInfo) {
                        ActiveGroupSimulateData updatingData = triple.getMiddle();
                        Optional<GroupMemberInfo> maybeGroupMemberInfo = triple.getRight();
                        BlockState state = triple.getLeft();

                        if (maybeGroupMemberInfo.isEmpty()) {
                            updatingData.updateBlockInfo(state, null, null);
                            // The line above already invalidates the surrounding data's caches and removes itself from them. No need to be worried.
                            for (ActiveGroupSimulateData extendedData : updatingData.extendingData) {
                                extendedData.updateBlockInfo(null, null, null);
                            }
                            continue;
                        }

                        if (!updatingData.isActive) {
                            updatingData.updateBlockInfo(state, null, maybeGroupMemberInfo.get());
                        } else {
                            Optional<GroupableSimulationMethod> newSimulateProperty = SimulationDataResource.getSimulationData(state.getBlock()).flatMap(simulationData -> simulationData.methodMap.values().stream().filter(method -> method instanceof GroupableSimulationMethod groupableMethod && groupId.equals(groupableMethod.simulateWithGroup)).findFirst()).map(method -> (GroupableSimulationMethod)method);
                            updatingData.updateBlockInfo(state, newSimulateProperty.orElse(null), maybeGroupMemberInfo.get());
                        }

                        for (ActiveGroupSimulateData extendedData : updatingData.extendingData) {
                            extendedData.updateBlockInfo(null, null, maybeGroupMemberInfo.get());
                        }
                    }

                    //group.removeIf(data -> !data.isActive);

                    activeSimulatedTime = activeSimulatedTime.passTicks(simulationStepTickDuration);
                }

                for (var data : group) {
                    if (!data.placeBlock)
                        continue;

                    BlockState state = data.getState();

                    if (state == null)
                        continue;

                    if (data.blockIsReplaced) {
                        level.setBlockAndUpdate(data.position, state);
                    } else {
                        level.setBlock(data.position, state, data.updateType);
                    }
                }
            }
        }

        return Pair.of(simulatedGroups, !missedGroup);
    }


    public static Optional<Collection<ActiveGroupSimulateData>> generateActiveGroupDataMap(ServerLevel level, LevelChunk chunk, ArrayList<ActiveGroupSimulateData> checkingBlockPositions, GroupInfo groupInfo, SimulatedTime simulatedTime) {
        int adjustedMaxGroupTickSize = Math.round(UnloadedActivity.config.maxGroupTickSize / groupInfo.groupSizePenalty);

        // This is assuming simulatedTime was just created where the remainingTicks has not been reduced
        long groupTickDifference = simulatedTime.remainingTicks();

        List<ActiveGroupSimulateData> pendingBlockPositions = new ArrayList<>();
        List<ActiveGroupSimulateData> toBeAddedToMap = new ArrayList<>(checkingBlockPositions);

        Long2ObjectOpenHashMap<ActiveGroupSimulateData> activeGroupDataMap = new Long2ObjectOpenHashMap<>();

        int forceLoadedChunks = 0;
        // Might be able to replace this with a list and call .contains() considering it's not going to be that big.
        LongOpenHashSet checkedChunks = new LongOpenHashSet();
        checkedChunks.add(GameUtils.toLong(chunk.getPos()));

        boolean chunksAreIndexed = true;

        // Define things up here to not have to reallocate it every loop.
        ArrayList<ActiveGroupSimulateData> finalizingBlockData = new ArrayList<>();
        LongOpenHashSet newChunks = new LongOpenHashSet();

        // Populate activeGroupDataMap
        while (!checkingBlockPositions.isEmpty()) {
            if (!toBeAddedToMap.isEmpty()) {
                for (var groupSimulateData : toBeAddedToMap) {
                    // Will only fail to add if something else is extending into that position.
                    // The extending data will always take priority.
                    boolean added = activeGroupDataMap.putIfAbsent(groupSimulateData.position.asLong(), groupSimulateData) == null;
                    if (added && groupSimulateData.getState().getBlock() instanceof DoorBlock) {
                        if (groupSimulateData.getState().getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                            BlockPos abovePos = groupSimulateData.position.above();
                            var newGroupSimulateData = new ActiveGroupSimulateData(abovePos, null, null, groupSimulateData.getGroupMemberInfo(), level);
                            groupSimulateData.extendingData.add(newGroupSimulateData);
                            activeGroupDataMap.put(abovePos.asLong(), newGroupSimulateData);
                        }
                    }
                }
                toBeAddedToMap.clear();
            }

            finalizingBlockData.clear();
            newChunks.clear();

            // Loop through all blocks.
            // Separate blocks that wants info from another chunk and blocks that have everything they need.
            for (var groupSimulateData : checkingBlockPositions) {
                boolean intersectsNewChunks = false;

                if (groupSimulateData.isActive) {
                    for (var offset : groupInfo.getOffsetsWithoutZero()) {
                        BlockPos checkPos = groupSimulateData.position.offset(offset);
                        ChunkPos chunkPos = GameUtils.chunkPosFromWorldPos(checkPos);
                        boolean isNewChunk = !checkedChunks.contains(GameUtils.toLong(chunkPos));
                        if (isNewChunk) {
                            newChunks.add(GameUtils.toLong(chunkPos));
                            intersectsNewChunks = true;
                        }
                    }
                }

                if (intersectsNewChunks) {
                    pendingBlockPositions.add(groupSimulateData);
                } else {
                    finalizingBlockData.add(groupSimulateData);
                }
            }

            checkingBlockPositions.clear();

            // Get surrounding data from the blocks that have everything they need.
            for (var currentActiveGroupSimulateData : finalizingBlockData) {
                if (!currentActiveGroupSimulateData.isActive)
                    continue;

                BlockPos blockPos = currentActiveGroupSimulateData.position;

                for (var offset : groupInfo.getOffsetsWithoutZero()) {
                    BlockPos affectingBlockPos = blockPos.offset(offset);

                    ActiveGroupSimulateData affectingSimulateData = activeGroupDataMap.get(affectingBlockPos.asLong());

                    if (affectingSimulateData != null)
                        currentActiveGroupSimulateData.surroundingData.add(affectingSimulateData);
                }
            }

            // Get requested chunks and add their blocks to checkingBlockPositions
            for (long newChunkPosLong : newChunks) {
                checkedChunks.add(newChunkPosLong);

                ChunkPos newChunkPos = GameUtils.chunkPosFromLong(newChunkPosLong);
                if (!GameUtils.isChunkLoaded(level, newChunkPos)) {
                    if (forceLoadedChunks >= UnloadedActivity.config.maxForcedChunkLoads)
                        continue;
                    forceLoadedChunks += 1;
                }

                LevelChunk newChunk = GameUtils.getChunk(level, newChunkPos);

                if (!isChunkIndexed(newChunk)) {
                    chunksAreIndexed = false;
                    level.getServer().addChunkToQueueFront(chunk);
                }

                if (!chunksAreIndexed) {
                    continue;
                }

                GroupChunkIndex newGroupChunkIndex = newChunk.getOrCreateGroupIndex(groupInfo.id);

                if (newGroupChunkIndex == null)
                    continue;

                long newGroupTickDifference;

                if (UnloadedActivity.config.useSystemTime) {
                    long lastMs = newGroupChunkIndex.getLastMs(newChunk.getLastMs());
                    newGroupTickDifference = Math.max(simulatedTime.endMs() - lastMs, 0) / 50;
                } else {
                    long lastTick = newGroupChunkIndex.getLastTick(newChunk.getLastTick());
                    newGroupTickDifference = Math.max(simulatedTime.endTick() - lastTick, 0);
                }

                float differencePercentage = Math.abs((float)(newGroupTickDifference - groupTickDifference) / (float)groupTickDifference);

                boolean forceInactive = false;

                if (differencePercentage > UnloadedActivity.config.maxGroupTickDeviationScale)
                    forceInactive = true;

                List<ActiveGroupSimulateData> newData = newGroupChunkIndex.getAndFilterBlocks(newChunk);
                int newTotalSize = activeGroupDataMap.size() + newData.size();

                if (newTotalSize > adjustedMaxGroupTickSize)
                    forceInactive = true;

                if (forceInactive) {
                    for (var groupSimData : newData) {
                        // They will still be considered during the simulation, but they themselves will not be simulated.
                        groupSimData.isActive = false;
                    }
                } else {
                    newGroupChunkIndex.setLastTick(simulatedTime.endTick());
                    newGroupChunkIndex.setLastMs(simulatedTime.endMs());
                    #if MC_VER >= MC_1_21_3
                    chunk.markUnsaved();
                    #else
                    chunk.setUnsaved(true);
                    #endif
                }

                toBeAddedToMap.addAll(newData);
                checkingBlockPositions.addAll(newData);
            }

            if (!chunksAreIndexed) {
                break;
            }

            // Prepare for next loop.
            checkingBlockPositions.addAll(pendingBlockPositions);
            pendingBlockPositions.clear();
        }

        if (!chunksAreIndexed) {
            return Optional.empty();
        }

        return Optional.of(activeGroupDataMap.values());
    }

    public static List<List<ActiveGroupSimulateData>> separateToIsolatedGroups(Collection<ActiveGroupSimulateData> activeGroupDataMap) {
        ArrayList<List<ActiveGroupSimulateData>> isolatedGroups = new ArrayList<>();

        int currentIndex = 0;

        // Define them up here to reduce allocations during the loop.
        ArrayList<ActiveGroupSimulateData> pendingData = new ArrayList<>();
        ArrayList<ActiveGroupSimulateData> listToLoop = new ArrayList<>();

        for (var activeGroupSimulateData : activeGroupDataMap) {
            if (!activeGroupSimulateData.isActive)
                continue;

            if (activeGroupSimulateData.groupIndex >= 0)
                continue;

            ArrayList<ActiveGroupSimulateData> newGroup = new ArrayList<>();
            pendingData.add(activeGroupSimulateData);


            while (!pendingData.isEmpty()) {
                var temp = listToLoop;
                listToLoop = pendingData;
                pendingData = temp;
                pendingData.clear();

                for (var updatingGroupData : listToLoop) {
                    if (updatingGroupData.groupIndex >= 0)
                        continue;

                    newGroup.add(updatingGroupData);

                    if (updatingGroupData.isActive) {
                        updatingGroupData.groupIndex = currentIndex;
                        pendingData.addAll(updatingGroupData.surroundingData);
                    }

                }
            }

            isolatedGroups.add(newGroup);
            currentIndex += 1;
        }

        return isolatedGroups;
    }

    public static void simulateTicks(ServerLevel level, LevelChunk chunk,  SimulatedTime simulatedTime, int randomTickSpeed) {
        List<BlockPos> precipitationBlocks = List.of();

        if (UnloadedActivity.config.enableSimulatingPrecipitationTicks) {
            precipitationBlocks = getPrecipitationTickableBlocks(chunk);

            if (UnloadedActivity.config.randomizeBlockUpdates)
                Collections.shuffle(precipitationBlocks);

            for (BlockPos blockPos : precipitationBlocks)
                simulateBlock(blockPos, level, simulatedTime, randomTickSpeed, true);
        }

        if (UnloadedActivity.config.enableSimulatingRandomTicks) {
            List<BlockPos> blockPosArray = getRandomTickableBlocks(chunk);

            if (UnloadedActivity.config.randomizeBlockUpdates)
                Collections.shuffle(blockPosArray);

            for (BlockPos blockPos : blockPosArray) {
                if (precipitationBlocks.contains(blockPos)) continue;
                simulateBlock(blockPos, level, simulatedTime, randomTickSpeed, false);
            }
        }
    }

    public static LongSet simulateBlock(BlockPos startingBlockPos, ServerLevel level, SimulatedTime startingSimulatedTime, int randomTickSpeed, boolean allowPrecipitationTicks) {
        float randomPickChance = MathUtils.getRandomPickProbability(randomTickSpeed);
        float precipitationPickChance = MathUtils.getPrecipitationPickProbability(randomTickSpeed);

        Long2IntOpenHashMap simulationIterations = new Long2IntOpenHashMap();
        int iterCount = 0;

        ArrayList<Triple<BlockPos, BlockState, SimulatedTime>> simulationQueue = new ArrayList<>();
        simulationQueue.add(Triple.of(startingBlockPos, level.getBlockState(startingBlockPos), startingSimulatedTime));

        while (!simulationQueue.isEmpty()) {
            Triple<BlockPos, BlockState, SimulatedTime> currentSimulation = simulationQueue.remove(0);

            iterCount++;
            if (iterCount > 100) {
                UnloadedActivity.LOGGER.warn("Reached max simulation iteration count.");
                break;
            }

            final BlockPos pos = currentSimulation.getLeft();

            if (simulationIterations.addTo(pos.asLong(), 1) > 5) {
                UnloadedActivity.LOGGER.warn("Reached max simulation iteration count for a singular block position.");
                break;
            }

            BlockState state = currentSimulation.getMiddle();

            final Block currentBlock = state.getBlock();

            if (UnloadedActivity.config.isBlockBlacklisted(currentBlock)) {
                break;
            }

            final SimulatedTime simulatedTime = currentSimulation.getRight();

            // This is not a HashMap because most of the time a block only has 1 or 2 properties.
            // It's probably not worth the overhead.
            ArrayList<Pair<String, SimulatedTime>> finishedProperties = new ArrayList<>();

            Optional<SimulationData> maybeSimulationData = SimulationDataResource.getSimulationData(currentBlock);
            if (maybeSimulationData.isEmpty()) break;
            SimulationData simulationData = maybeSimulationData.get();

            if (UnloadedActivity.config.debugLogs)
                if (!state.isAir())
                    UnloadedActivity.LOGGER.info("Simulating block " + currentBlock + " with " + simulationData.methodMap.size() + " properties.");


            ArrayList<Pair<String, SimulationMethod>> pendingProperties = new ArrayList<>(simulationData.methodMap.size());

            for (var entry : simulationData.methodMap.entrySet()) {

                String propertyName = entry.getKey();
                var simulationMethod = entry.getValue();

                if (!simulationMethod.canDoMore(state, level, pos)) {
                    finishedProperties.add(Pair.of(propertyName, simulatedTime));
                } else {
                    pendingProperties.add(Pair.of(propertyName, simulationMethod));
                }
            }

            boolean methodsGotFinished = true;

            // This loop is here to handle simulation methods that depend on other simulation methods.
            // It will keep doing passes until there is nothing more to simulate.
            while (methodsGotFinished) {
                methodsGotFinished = false;

                var iterator = pendingProperties.iterator();

                // This loop is to try to simulate all simulation methods.
                while (iterator.hasNext()) {
                    var entry = iterator.next();

                    boolean validDependencies = true;
                    SimulatedTime lastSimulatedTime = simulatedTime;

                    SimulationMethod simulationMethod = entry.getSecond();

                    if (simulationMethod.isPrecipitation && (!allowPrecipitationTicks || !UnloadedActivity.config.enableSimulatingPrecipitationTicks)) {
                        continue;
                    }
                    if (!simulationMethod.isPrecipitation && !UnloadedActivity.config.enableSimulatingRandomTicks) {
                        continue;
                    }
                    if (simulationMethod.simulatesWithGroup()) {
                        continue;
                    }
                    var propertyName = entry.getFirst();
                    for (String dependency : simulationMethod.dependencies) {
                        SimulatedTime dependencySimulatedTime = null;

                        for (var pair : finishedProperties) {
                            if (pair.getFirst().equals(dependency)) {
                                dependencySimulatedTime = pair.getSecond();
                                break;
                            }
                        }

                        if (dependencySimulatedTime == null) {
                            validDependencies = false;
                            break;
                        }

                        if (lastSimulatedTime.currentTick() < dependencySimulatedTime.currentTick()) {
                            lastSimulatedTime = dependencySimulatedTime;
                        }
                    }

                    if (!validDependencies) {
                        if (UnloadedActivity.config.debugLogs)
                            UnloadedActivity.LOGGER.info("Skipping simulating property " + propertyName + " due to invalid dependencies.");
                        continue;
                    }

                    iterator.remove();

                    // For the block to get to this point, isPropertyFinished must have returned false.
                    // We can use hasValidConditions instead of canSimulateProperty.
                    if (!simulationMethod.hasValidConditions(state, level, pos)) {
                        if (UnloadedActivity.config.debugLogs)
                            UnloadedActivity.LOGGER.info("Skipping simulating property " + propertyName + " due to invalid conditions.");
                        continue;
                    }

                    if (lastSimulatedTime.remainingTicks() <= 0) {
                        if (UnloadedActivity.config.debugLogs)
                            UnloadedActivity.LOGGER.info("Skipping simulating property " + propertyName + " due to no simulation time.");
                        continue;
                    }

                    if (UnloadedActivity.config.debugLogs)
                        UnloadedActivity.LOGGER.info("Simulating property " + propertyName + " on block " + currentBlock);

                    float pickChance = simulationMethod.isPrecipitation ? precipitationPickChance : randomPickChance;

                    DeferredBlockPlacer blockPlacer = simulationMethod.simulate(state, level, pos, GameUtils.getRand(level), lastSimulatedTime, pickChance);

                    if (blockPlacer == null) {
                        // We have no info about what happened. Abort entire simulation.
                        methodsGotFinished = false;
                        simulationQueue.clear();
                        break; // The reason it doesn't return immediately is that we need to return a LongSet.
                    }

                    if (blockPlacer.isEmpty()) continue;

                    boolean breakPropertyLoop = false;

                    for (DeferredBlockPlacer.BlockPlacementInfo placeInfo : blockPlacer) {
                        level.setBlock(placeInfo.blockPos(), placeInfo.blockState(), placeInfo.updateType());
                        if (placeInfo.updateNeighbors()) {
                            #if MC_VER >= MC_1_21_3
                                level.neighborChanged(placeInfo.blockState(), placeInfo.blockPos(), placeInfo.blockState().getBlock(), null, false);
                            #else
                                level.neighborChanged(placeInfo.blockState(), placeInfo.blockPos(), placeInfo.blockState().getBlock(), placeInfo.blockPos(), false);
                            #endif
                            level.scheduleTick(placeInfo.blockPos(), placeInfo.blockState().getBlock(), 1);
                        }

                        SimulatedTime placedAtTime = placeInfo.placedAtTime();

                        BlockState newState = placeInfo.blockState();


                        if (placeInfo.blockPos() != pos) {
                            if (placedAtTime.remainingTicks() > 0) {
                                simulationQueue.removeIf(t -> t.getLeft() == pos);
                                simulationQueue.add(Triple.of(placeInfo.blockPos(), newState, placedAtTime));
                            }
                        } else {
                            if (newState.getBlock() != currentBlock) {
                                // Block is entirely different. Stop simulating.
                                methodsGotFinished = false;
                                breakPropertyLoop = true;
                                if (placedAtTime.remainingTicks() > 0) {
                                    simulationQueue.removeIf(t -> t.getLeft() == pos);
                                    // Add to the front. Prioritize the current position.
                                    simulationQueue.add(0, Triple.of(placeInfo.blockPos(), newState, placedAtTime));
                                }
                            } else {
                                state = newState;
                                if (!simulationMethod.canDoMore(state, level, pos)) {
                                    methodsGotFinished = true;
                                    finishedProperties.add(Pair.of(propertyName, placedAtTime));
                                }
                            }

                        }
                    }

                    if (breakPropertyLoop) break;
                }
            }
        }
        return simulationIterations.keySet();
    }

    public static void simulateBlockEntity(BlockEntity blockEntity, long timeDifference) {
        if (!UnloadedActivity.config.enableSimulatingBlockEntities) return;
        blockEntity.unloadedactivity$simulateTime(timeDifference);
    }

    public static void simulateEntity(Entity entity, long timeDifference) {
        if (!UnloadedActivity.config.enableSimulatingEntities) return;
        entity.unloadedactivity$simulateTime(timeDifference);
    }
}
