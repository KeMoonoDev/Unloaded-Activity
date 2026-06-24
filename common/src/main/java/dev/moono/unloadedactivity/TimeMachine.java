package dev.moono.unloadedactivity;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import dev.moono.unloadedactivity.api.SimulationMethod;
import dev.moono.unloadedactivity.api.simulation_methods.GroupableSimulationMethod;
import dev.moono.unloadedactivity.datapack.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
    public static Pair<Integer, Boolean> simulateChunk(long timeDifference, ServerLevel level, LevelChunk chunk, int randomTickSpeed, int groupUpdateBudget, long currentTime) {
        if (!UnloadedActivity.config.enableSimulatingRandomTicks
            && !UnloadedActivity.config.enableSimulatingPrecipitationTicks
            && !UnloadedActivity.config.enableSimulatingGroups) return Pair.of(0, true);

        int simulatedGroupCount = 0;

        if (UnloadedActivity.config.enableSimulatingGroups) {
            Pair<Integer, Boolean> result = TimeMachine.simulateGroupTicks(level, chunk, randomTickSpeed, groupUpdateBudget, currentTime);
            simulatedGroupCount = result.getFirst();
            boolean simulatedAllGroups = result.getSecond();
            if (!simulatedAllGroups) {
                return result;
            }
        }

        if (UnloadedActivity.config.enableSimulatingRandomTicks || UnloadedActivity.config.enableSimulatingPrecipitationTicks)
            TimeMachine.simulateTicks(timeDifference, level, chunk, randomTickSpeed);

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
                                        .computeIfAbsent(groupId, (id) -> new GroupChunkIndex(new ArrayList<>(), chunk.getLastTick(), id))
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
    public static Pair<Integer, Boolean> simulateGroupTicks(ServerLevel level, LevelChunk chunk, int randomTickSpeed, int groupUpdateBudget, long currentTime) {
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

            long lastGroupTick = groupChunkIndex.getLastTick(chunk.getLastTick());
            long groupTimeDifference = Math.max(currentTime - lastGroupTick, 0);

            if (groupTimeDifference <= UnloadedActivity.config.groupTickDifferenceThreshold) {
                groupChunkIndex.setLastTick(currentTime);
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
                groupChunkIndex.setLastTick(currentTime);
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

            Optional<Collection<ActiveGroupSimulateData>> maybeActiveGroupDataMap = generateActiveGroupDataMap(level, chunk, checkingBlockPositions, groupInfo, lastGroupTick, currentTime);

            if (maybeActiveGroupDataMap.isEmpty()) {
                break;
            }

            Collection<ActiveGroupSimulateData> activeGroupDataMap = maybeActiveGroupDataMap.get();

            // Separate them into isolated groups.
            List<List<ActiveGroupSimulateData>> isolatedGroups = separateToIsolatedGroups(activeGroupDataMap);

            RandomSource random = level.getRandom();
            float randomPickOdds = MathUtils.getRandomPickOdds(randomTickSpeed);
            float precipitationPickOdds = MathUtils.getPrecipitationPickOdds(randomTickSpeed);

            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Simulating " + isolatedGroups.size() + " isolated groups");

            // Data has been made. Time to actually do the simulation.
            for (List<ActiveGroupSimulateData> group : isolatedGroups) {
                int totalIterations = 0;
                long remainingCycles = groupTimeDifference;

                WorldWeatherForecast weatherData = level.getWeatherForecast();

                if (UnloadedActivity.config.debugLogs)
                    UnloadedActivity.LOGGER.info("Simulating isolated group of " + group.size() + " members");

                while (remainingCycles > 0 && totalIterations < UnloadedActivity.config.maxGroupTickIterations) {
                    long simulationCurrentTime = currentTime - remainingCycles;

                    long minProbabilityStepDuration = remainingCycles / (UnloadedActivity.config.maxGroupTickIterations - totalIterations);
                    long maxProbabilityStepDuration = remainingCycles / Math.max(1, UnloadedActivity.config.minGroupTickIterations - totalIterations);
                    totalIterations++;

                    long minNextOddsSwitchDuration = Long.MAX_VALUE;
                    float maxProbability = 0F;

                    long nextWeatherSwitchDuration = weatherData.getNextWeatherChangeDuration(simulationCurrentTime);
                    boolean isRaining = weatherData.getWeatherAtTime(simulationCurrentTime);

                    for (ActiveGroupSimulateData simulationData : group) {
                        if (!simulationData.isActive)
                            continue;

                        // For isActive to return true, there must be a simulationMethod present.
                        SimulationMethod simulationMethod = simulationData.getSimulationMethod().orElseThrow();

                        float pickOdds;

                        if (simulationMethod.isPrecipitation) {
                            pickOdds = precipitationPickOdds;
                        } else {
                            pickOdds = randomPickOdds;
                        }

                        BlockState state = simulationData.getState();
                        BlockPos pos = simulationData.position;

                        ExpressionContext calculationData = ExpressionContext.updating(level, state, pos, simulationCurrentTime, Map.of(), simulationData);

                        Pair<Float, Long> oddsAndDuration = simulationData.updateAndGetOdds(nextWeatherSwitchDuration, calculationData);

                        minNextOddsSwitchDuration = Math.min(minNextOddsSwitchDuration, oddsAndDuration.getSecond());

                        float probability = oddsAndDuration.getFirst() * pickOdds;

                        maxProbability = Math.max(probability, maxProbability);
                    }

                    if (maxProbability <= 0.0) {
                        if (minNextOddsSwitchDuration >= remainingCycles) {
                            break;
                        } else {
                            remainingCycles -= minNextOddsSwitchDuration;
                            for (ActiveGroupSimulateData simulationData : group) {
                                if (!simulationData.isActive)
                                    continue;
                                simulationData.passTime(minNextOddsSwitchDuration);
                            }
                            continue;
                        }
                    }

                    long probabilityDuration = (long)Math.ceil((1.0 / maxProbability) * UnloadedActivity.config.groupTickUpdateStrength);
                    probabilityDuration = Math.min(maxProbabilityStepDuration, probabilityDuration);
                    probabilityDuration = Math.max(minProbabilityStepDuration, probabilityDuration);

                    long simulationStepDuration = Math.min(Math.min(minNextOddsSwitchDuration, probabilityDuration), remainingCycles);

                    ArrayList<Triple<BlockState, ActiveGroupSimulateData, Optional<GroupMemberInfo>>> pendingUpdateBlockInfo = new ArrayList<>();

                    for (ActiveGroupSimulateData simulationData : group) {
                        if (!simulationData.isActive)
                            continue;

                        simulationData.passTime(simulationStepDuration);

                        // For isActive to return true, there must be a simulationMethod present.
                        GroupableSimulationMethod simulationMethod = simulationData.getSimulationMethod().orElseThrow();

                        BlockState state = simulationData.getState();
                        Block block = state.getBlock();

                        float pickOdds;

                        if (simulationMethod.isPrecipitation) {
                            pickOdds = precipitationPickOdds;
                        } else {
                            pickOdds = randomPickOdds;
                        }

                        int remainingUpdates = simulationData.getRemainingUpdates();

                        if (remainingUpdates > 0) {
                            float totalOdds = simulationData.currentOdds * pickOdds;
                            int occurrences = MathUtils.getOccurrencesBinomial(simulationStepDuration, totalOdds, remainingUpdates, random);
                            simulationData.addUpdateCount(occurrences);
                        }

                        remainingUpdates = simulationData.getRemainingUpdates();

                        if (remainingUpdates > 0) {
                            continue;
                        }

                        int updateCount = simulationData.getCurrentUpdateCount();

                        DeferredBlockPlacer newBlockStates = simulationMethod.getNewBlockStates(state, level, simulationData.position, updateCount, simulationStepDuration, simulationStepDuration, simulationData);

                        if (newBlockStates.size() > 1) {
                            throw new RuntimeException("Group simulation type must only return 1 or 0 new blockstates.");
                        }

                        if (newBlockStates.isEmpty()) {
                            simulationData.isActive = false;
                            continue;
                        }

                        DeferredBlockPlacer.BlockPlacementInfo newBlockData = newBlockStates.get(0);

                        if (!newBlockData.blockPos().equals(simulationData.position)) {
                            throw new RuntimeException("Group simulation type must not change its position.");
                        }

                        simulationData.placeBlock = true;

                        BlockState newBlockState = newBlockData.blockState();
                        Block newBlock = newBlockState.getBlock();
                        if (newBlock == block) {
                            simulationData.isActive = false;
                            pendingUpdateBlockInfo.add(Triple.of(newBlockState, simulationData, Optional.of(simulationData.getGroupMemberInfo())));
                            simulationData.updateType = newBlockData.updateType();
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
                            updatingData.updateBlockInfo(state, Optional.empty(), null);
                            // The line above already invalidates the surrounding data's caches and removes itself from them. No need to be worried.
                            for (ActiveGroupSimulateData extendedData : updatingData.extendingData) {
                                extendedData.updateBlockInfo(null, Optional.empty(), null);
                            }
                            continue;
                        }

                        if (!updatingData.isActive) {
                            updatingData.updateBlockInfo(state, Optional.empty(), maybeGroupMemberInfo.get());
                        } else {
                            Optional<GroupableSimulationMethod> newSimulateProperty = SimulationDataResource.getSimulationData(state.getBlock()).flatMap(simulationData -> simulationData.methodMap.values().stream().filter(method -> method instanceof GroupableSimulationMethod groupableMethod && groupId.equals(groupableMethod.simulateWithGroup)).findFirst()).map(method -> (GroupableSimulationMethod)method);
                            updatingData.updateBlockInfo(state, newSimulateProperty, maybeGroupMemberInfo.get());
                        }

                        for (ActiveGroupSimulateData extendedData : updatingData.extendingData) {
                            extendedData.updateBlockInfo(null, Optional.empty(), maybeGroupMemberInfo.get());
                        }
                    }

                    //group.removeIf(data -> !data.isActive);

                    remainingCycles -= simulationStepDuration;


                }

                for (var data : group) {
                    if (!data.placeBlock)
                        continue;

                    if (data.blockIsReplaced) {
                        level.setBlockAndUpdate(data.position, data.getState());
                    } else {
                        level.setBlock(data.position, data.getState(), data.updateType);
                    }
                }
            }
        }

        return Pair.of(simulatedGroups, !missedGroup);
    }


    public static Optional<Collection<ActiveGroupSimulateData>> generateActiveGroupDataMap(ServerLevel level, LevelChunk chunk, ArrayList<ActiveGroupSimulateData> checkingBlockPositions, GroupInfo groupInfo, long lastMainChunkGroupTick, long currentTime) {
        long groupTimeDifference = Math.max(currentTime - lastMainChunkGroupTick, 0);

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
                            var newGroupSimulateData = new ActiveGroupSimulateData(abovePos, null, Optional.empty(), groupSimulateData.getGroupMemberInfo(), level);
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

                long newLastGroupTick = newGroupChunkIndex.getLastTick(newChunk.getLastTick());
                long differenceFromMainChunk = lastMainChunkGroupTick - newLastGroupTick;
                float differencePercentage = Math.abs((float)differenceFromMainChunk / (float)groupTimeDifference);

                boolean forceInactive = false;

                if (differencePercentage > UnloadedActivity.config.maxGroupTickDeviationScale)
                    forceInactive = true;

                List<ActiveGroupSimulateData> newData = newGroupChunkIndex.getAndFilterBlocks(newChunk);
                int newTotalSize = activeGroupDataMap.size() + newData.size();

                if (newTotalSize > UnloadedActivity.config.maxGroupTickSize)
                    forceInactive = true;

                if (forceInactive) {
                    for (var groupSimData : newData) {
                        // They will still be considered during the simulation, but they themselves will not be simulated.
                        groupSimData.isActive = false;
                    }
                } else {
                    newGroupChunkIndex.setLastTick(currentTime);
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

    public static void simulateTicks(long timeDifference, ServerLevel level, LevelChunk chunk, int randomTickSpeed) {
        List<BlockPos> precipitationBlocks = List.of();

        if (UnloadedActivity.config.enableSimulatingPrecipitationTicks) {
            precipitationBlocks = getPrecipitationTickableBlocks(chunk);

            if (UnloadedActivity.config.randomizeBlockUpdates)
                Collections.shuffle(precipitationBlocks);

            for (BlockPos blockPos : precipitationBlocks)
                simulateBlock(blockPos, level, timeDifference, randomTickSpeed, true);
        }

        if (UnloadedActivity.config.enableSimulatingRandomTicks) {
            List<BlockPos> blockPosArray = getRandomTickableBlocks(chunk);

            if (UnloadedActivity.config.randomizeBlockUpdates)
                Collections.shuffle(blockPosArray);

            for (BlockPos blockPos : blockPosArray) {
                if (precipitationBlocks.contains(blockPos)) continue;
                simulateBlock(blockPos, level, timeDifference, randomTickSpeed, false);
            }
        }
    }

    public static void simulateBlock(BlockPos pos, ServerLevel level, long timeLeft, int randomTickSpeed, boolean allowPrecipitationTicks) {
        MinecraftServer server = level.getServer();

        float randomPickChance = MathUtils.getRandomPickOdds(randomTickSpeed);
        float precipitationPickChance = MathUtils.getPrecipitationPickOdds(randomTickSpeed);

        BlockState state = level.getBlockState(pos);

        boolean blockHasChanged = true;

        while (blockHasChanged) {
            blockHasChanged = false;

            if (UnloadedActivity.config.isBlockBlacklisted(state)) {
                break;
            }

            Block block = state.getBlock();

            // This is not a HashMap because most of the time a block only has 1 or 2 properties.
            // It's not worth the overhead.
            ArrayList<Pair<String, Long>> finishedProperties = new ArrayList<>();

            // This is not a HashSet because of the same reason above.
            // Even if there's a duplicate, we only check if it contains, so it doesn't matter.
            ArrayList<String> propertiesWithDependents = new ArrayList<>();

            Optional<SimulationData> maybeSimulationData = SimulationDataResource.getSimulationData(block);
            if (maybeSimulationData.isEmpty()) break;
            SimulationData simulationData = maybeSimulationData.get();

            if (UnloadedActivity.config.debugLogs)
                if (!state.isAir())
                    UnloadedActivity.LOGGER.info("Simulating block " + block + " with " + simulationData.methodMap.size() + " properties.");


            ArrayList<Pair<String, SimulationMethod>> pendingProperties = new ArrayList<>(simulationData.methodMap.size());

            for (var entry : simulationData.methodMap.entrySet()) {

                String propertyName = entry.getKey();
                var simulationMethod = entry.getValue();

                if (!simulationMethod.canDoMore(state, level, pos)) {
                    finishedProperties.add(Pair.of(propertyName, 0L));
                } else {
                    pendingProperties.add(Pair.of(propertyName, simulationMethod));
                }

                propertiesWithDependents.addAll(simulationMethod.dependencies);
            }

            boolean continueCheck = true;

            while (continueCheck) {
                continueCheck = false;

                var iterator = pendingProperties.iterator();

                while (iterator.hasNext()) {
                    var entry = iterator.next();

                    boolean validDependencies = true;
                    long maxDuration = 0;

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
                        Long dependencyDuration = null;

                        for (var pair : finishedProperties) {
                            if (pair.getFirst().equals(dependency)) {
                                dependencyDuration = pair.getSecond();
                                break;
                            }
                        }

                        if (dependencyDuration == null) {
                            validDependencies = false;
                            break;
                        }

                        maxDuration = Math.max(maxDuration, dependencyDuration);
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

                    long simulateTime = timeLeft - maxDuration;

                    assert (simulateTime >= 0);

                    if (simulateTime == 0) {
                        if (UnloadedActivity.config.debugLogs)
                            UnloadedActivity.LOGGER.info("Skipping simulating property " + propertyName + " due to no simulation time.");
                        continue;
                    }

                    if (UnloadedActivity.config.debugLogs)
                        UnloadedActivity.LOGGER.info("Simulating property " + propertyName + " on block " + block);

                    float pickChance = simulationMethod.isPrecipitation ? precipitationPickChance : randomPickChance;

                    DeferredBlockPlacer blockPlacer = simulationMethod.simulate(state, level, pos, GameUtils.getRand(level), simulateTime, pickChance, propertiesWithDependents.contains(propertyName), null);

                    if (blockPlacer == null) {
                        continueCheck = false;
                        break;
                    }

                    if (blockPlacer.isEmpty()) continue;

                    blockPlacer.forEach(placeInfo -> {
                        level.setBlock(placeInfo.blockPos(), placeInfo.blockState(), placeInfo.updateType());
                        if (placeInfo.updateNeighbors()) {
                            #if MC_VER >= MC_1_21_3
                                level.neighborChanged(placeInfo.blockState(), placeInfo.blockPos(), placeInfo.blockState().getBlock(), null, false);
                            #else
                                level.neighborChanged(placeInfo.blockState(), placeInfo.blockPos(), placeInfo.blockState().getBlock(), placeInfo.blockPos(), false);
                            #endif
                            level.scheduleTick(placeInfo.blockPos(), placeInfo.blockState().getBlock(), 1);
                        }
                    });

                    DeferredBlockPlacer.BlockPlacementInfo lastBlockPlacement = blockPlacer.get(blockPlacer.size()-1);

                    state = lastBlockPlacement.blockState();
                    pos = lastBlockPlacement.blockPos();

                    long duration = lastBlockPlacement.duration();

                    assert (duration <= simulateTime);

                    long simulationDuration = duration + maxDuration;

                    assert (simulationDuration <= timeLeft);

                    if (state.getBlock() != block) {
                        continueCheck = false;
                        timeLeft -= simulationDuration;
                        if (timeLeft > 0)
                            blockHasChanged = true;
                        break;
                    }

                    if (!simulationMethod.canDoMore(state, level, pos)) {
                        continueCheck = true;
                        finishedProperties.add(Pair.of(propertyName, blockPlacer.maxDuration()));
                    }
                }
            }
        }
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
