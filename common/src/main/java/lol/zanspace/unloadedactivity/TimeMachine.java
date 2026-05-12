package lol.zanspace.unloadedactivity;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import com.mojang.datafixers.util.Pair;
import lol.zanspace.unloadedactivity.datapack.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

public class TimeMachine {
    /// Returns how many groups were simulated and if the last ticked time should be updated/if normal ticks were simulated.
    public static Pair<Integer, Boolean> simulateChunk(long timeDifference, ServerLevel level, LevelChunk chunk, int randomTickSpeed, int groupUpdateBudget) {
        if (!UnloadedActivity.config.enableSimulatingRandomTicks
            && !UnloadedActivity.config.enableSimulatingPrecipitationTicks
            && !UnloadedActivity.config.enableSimulatingGroups) return Pair.of(0, true);

        int simulatedGroupCount = 0;

        if (UnloadedActivity.config.enableSimulatingGroups) {
            Pair<Integer, Boolean> result = TimeMachine.simulateGroupTicks(level, chunk, randomTickSpeed, groupUpdateBudget);
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
                SimulationData simulationData = block.getSimulationData();
                if (simulationData.hasRandTicksWithoutGroup) {
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
                        SimulationData simulationData = block.getSimulationData();
                        if (simulationData.hasRandTicksWithoutGroup) {
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
        chunk.setGroupIndexes(newGroupIndexes);
        chunk.setSimulationVersion(UnloadedActivity.chunkSimVer);
        #if MC_VER >= MC_1_21_3
        chunk.markUnsaved();
        #else
        chunk.setUnsaved(true);
        #endif
    }

    /// Returns positions of blocks that implement random ticks.
    public static ArrayList<BlockPos> getRandomTickableBlocks(LevelChunk chunk) {
        ArrayList<Long> currentSimulationBlocks = chunk.getSimulationBlocks();

        ArrayList<BlockPos> blockPosArray = new ArrayList<>(currentSimulationBlocks.size());

        if (UnloadedActivity.config.debugLogs)
            UnloadedActivity.LOGGER.info("Looping through "+currentSimulationBlocks.size()+" known positions.");

        int prevSize = currentSimulationBlocks.size();

        currentSimulationBlocks.removeIf((longPos) -> {
            BlockPos pos = BlockPos.of(longPos);
            BlockState state = chunk.getBlockState(pos);
            Block block = state.getBlock();
            SimulationData simulationData = block.getSimulationData();

            if (simulationData.hasRandTicksWithoutGroup)
                blockPosArray.add(pos);

            return !simulationData.hasRandTicksWithoutGroup;
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

                if (airPosBlock.getSimulationData().hasPrecTicksWithoutGroup)
                    precipitationBlocks.add(airPos);

                if (groundPosBlock.getSimulationData().hasPrecTicksWithoutGroup)
                    precipitationBlocks.add(groundPos);
            }

        return precipitationBlocks;
    }

    // This doesn't take a timeDifference parameter because that is supposed to be calculated in the function using the last group tick.
    public static Pair<Integer, Boolean> simulateGroupTicks(ServerLevel level, LevelChunk chunk, int randomTickSpeed, int groupUpdateBudget) {
        var groupIndexes = chunk.getGroupIndexes();

        long currentTime = GameUtils.getTime(level);

        int simulatedGroups = 0;

        boolean missedGroup = false;

        for (var entry : groupIndexes.entrySet()) {
            var groupId = entry.getKey();
            GroupInfo groupInfo = GroupInfoResource.GROUPS_MAP.get(groupId);

            if (groupInfo == null)
                continue;

            GroupChunkIndex groupChunkIndex = entry.getValue();

            if (groupChunkIndex == null)
                continue;

            long lastGroupTick = groupChunkIndex.getLastTick(chunk.getLastTick());
            long groupTimeDifference = Math.max(currentTime - lastGroupTick, 0);

            if (groupTimeDifference <= UnloadedActivity.config.groupTickDifferenceThreshold) {
                groupChunkIndex.setLastTick(currentTime);
                continue;
            }

            List<ActiveGroupSimulateData> checkingBlockPositions = groupChunkIndex.getAndFilterBlocks(chunk);

            boolean isAllInactive = true;

            for (var groupSimulateData : checkingBlockPositions) {
                if (groupSimulateData.isActive) {
                    isAllInactive = false;
                    break;
                }
            }

            if (isAllInactive) {
                groupChunkIndex.setLastTick(currentTime);
                continue;
            }

            if (simulatedGroups >= groupUpdateBudget) {
                // we want find all the groups where groupTimeDifference is not enough so we can update the last tick.
                missedGroup = true;
                continue;
            }

            simulatedGroups++;

            List<ActiveGroupSimulateData> pendingBlockPositions = new ArrayList<>();
            List<ActiveGroupSimulateData> toBeAddedToMap = checkingBlockPositions;

            Map<BlockPos, ActiveGroupSimulateData> activeGroupDataMap = new HashMap<>(UnloadedActivity.config.maxGroupTickSize);

            int forceLoadedChunks = 0;
            Set<ChunkPos> checkedChunks = new HashSet<>();
            checkedChunks.add(chunk.getPos());

            boolean chunksAreIndexed = true;

            // Populate activeGroupDataMap
            while (!checkingBlockPositions.isEmpty()) {
                if (!toBeAddedToMap.isEmpty()) {
                    for (var groupSimulateData : toBeAddedToMap) {
                        activeGroupDataMap.put(groupSimulateData.position, groupSimulateData);
                    }
                    toBeAddedToMap = new ArrayList<>();
                }

                Set<ChunkPos> newChunks = new HashSet<>();

                List<ActiveGroupSimulateData> finalizingBlockData = new ArrayList<>(checkingBlockPositions.size());

                // Loop through all blocks.
                // Separate blocks that wants info from another chunk and blocks that have everything they need.
                for (var groupSimulateData : checkingBlockPositions) {
                    boolean intersectsNewChunks = false;

                    if (groupSimulateData.isActive) {
                        for (var offset : groupInfo.iterateOffsets()) {
                            BlockPos checkPos = groupSimulateData.position.offset(offset);
                            ChunkPos chunkPos = GameUtils.chunkPosFromWorldPos(checkPos);
                            boolean isNewChunk = !checkedChunks.contains(chunkPos);
                            if (isNewChunk) {
                                newChunks.add(chunkPos);
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
                for (var data : finalizingBlockData) {
                    if (!data.isActive)
                        continue;

                    BlockPos blockPos = data.position;

                    ActiveGroupSimulateData activeGroupSimulateData = activeGroupDataMap.get(blockPos);

                    for (var offset : groupInfo.iterateOffsets()) {
                        if (offset.equals(Vec3i.ZERO)) {
                            continue;
                        }

                        BlockPos affectingBlockPos = blockPos.offset(offset);

                        ActiveGroupSimulateData affectingSimulateData = activeGroupDataMap.get(affectingBlockPos);

                        if (affectingSimulateData != null)
                            activeGroupSimulateData.surroundingData.add(affectingSimulateData);
                    }
                }

                finalizingBlockData.clear();

                // Get requested chunks and add their blocks to checkingBlockPositions
                for (var newChunkPos : newChunks) {
                    checkedChunks.add(newChunkPos);

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

                    GroupChunkIndex newGroupChunkIndex = newChunk.getGroupIndexes().get(groupId);

                    if (newGroupChunkIndex == null)
                        continue;

                    long newLastGroupTick = newGroupChunkIndex.getLastTick(newChunk.getLastTick());
                    long differenceFromMainChunk = lastGroupTick - newLastGroupTick;
                    long differencePercentage = Math.abs(differenceFromMainChunk / groupTimeDifference);

                    if (differencePercentage > UnloadedActivity.config.maxGroupTickDeviationScale)
                        continue;

                    List<ActiveGroupSimulateData> newData = newGroupChunkIndex.getAndFilterBlocks(newChunk);
                    int newTotalSize = activeGroupDataMap.size() + newData.size();

                    if (newTotalSize > UnloadedActivity.config.maxGroupTickSize)
                        continue;


                    newGroupChunkIndex.setLastTick(currentTime);
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
                break;
            }

            // Separate them into isolated groups.
            ArrayList<ArrayList<ActiveGroupSimulateData>> isolatedGroups = new ArrayList<>();

            int currentIndex = 0;

            for (var groupEntry : activeGroupDataMap.entrySet()) {
                ActiveGroupSimulateData activeGroupSimulateData = groupEntry.getValue();
                if (!activeGroupSimulateData.isActive)
                    continue;

                if (activeGroupSimulateData.groupIndex >= 0)
                    continue;

                ArrayList<ActiveGroupSimulateData> newGroup = new ArrayList<>();

                ArrayList<ActiveGroupSimulateData> pendingData = new ArrayList<>();
                pendingData.add(activeGroupSimulateData);

                while (!pendingData.isEmpty()) {
                    ArrayList<ActiveGroupSimulateData> listToLoop = pendingData;
                    pendingData = new ArrayList<>();

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

            RandomSource random = level.getRandom();
            float randomPickOdds = MathUtils.getRandomPickOdds(randomTickSpeed);
            float precipitationPickOdds = 1F/4096F;

            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Simulating " + isolatedGroups.size() + " isolated groups");

            // Data has been made. Time to actually do the simulation.
            for (ArrayList<ActiveGroupSimulateData> group : isolatedGroups) {
                int totalIterations = 0;
                long remainingCycles = groupTimeDifference;
                long simulationCurrentTime = currentTime - remainingCycles;

                WorldWeatherForecast weatherData = level.getWeatherForecast();

                if (UnloadedActivity.config.debugLogs)
                    UnloadedActivity.LOGGER.info("Simulating isolated group of " + group.size() + " members");

                while (remainingCycles > 0 && totalIterations < UnloadedActivity.config.maxGroupTickIterations) {
                    long minProbabilityStepDuration = remainingCycles / (UnloadedActivity.config.maxGroupTickIterations - totalIterations);
                    totalIterations++;

                    long minNextOddsSwitchDuration = Long.MAX_VALUE;
                    long nextWeatherSwitchDuration = Long.MAX_VALUE;
                    float maxProbability = 0F;

                    for (ActiveGroupSimulateData simulationData : group) {
                        if (!simulationData.isActive)
                            continue;

                        // For isActive to return true, there must be a simulateProperty present.
                        SimulateProperty simulateProperty = simulationData.getSimulateProperty().orElseThrow();

                        BlockState state = simulationData.blockState;
                        BlockPos pos = simulationData.position;
                        boolean isRaining = weatherData.getWeatherAtTime(simulationCurrentTime);

                        CalculationData calculationData = new CalculationData(level, state, pos, simulationCurrentTime, isRaining, false, simulationData);

                        long nextOddsSwitchDuration = simulateProperty.advanceProbability.getNextValueSwitchDuration(calculationData);
                        minNextOddsSwitchDuration = Math.min(minNextOddsSwitchDuration, nextOddsSwitchDuration);

                        if (nextWeatherSwitchDuration == Long.MAX_VALUE && simulateProperty.advanceProbability.isAffectedByWeather(calculationData)) {
                            nextWeatherSwitchDuration = weatherData.getNextWeatherChangeDuration(simulationCurrentTime);
                        }

                        float pickOdds;

                        if (simulateProperty.isPrecipitation) {
                            pickOdds = precipitationPickOdds;
                        } else {
                            pickOdds = randomPickOdds;
                        }

                        float probability = simulateProperty.advanceProbability.calculateValue(calculationData).floatValue() * pickOdds;

                        maxProbability = Math.max(probability, maxProbability);
                    }

                    if (maxProbability <= 0.0)
                        break;

                    long probabilityDuration = (long)Math.ceil((1.0 / maxProbability) * UnloadedActivity.config.groupTickUpdateStrength);
                    probabilityDuration = Math.max(minProbabilityStepDuration, probabilityDuration);

                    long simulationStepDuration = Math.min(Math.min(Math.min(minNextOddsSwitchDuration, nextWeatherSwitchDuration), probabilityDuration), remainingCycles);

                    ArrayList<ActiveGroupSimulateData> pendingRemoval = new ArrayList<>();
                    ArrayList<Pair<ActiveGroupSimulateData, GroupMemberInfo>> pendingUpdateMembership = new ArrayList<>();

                    for (ActiveGroupSimulateData simulationData : group) {
                        if (!simulationData.isActive)
                            continue;

                        // For isActive to return true, there must be a simulateProperty present.
                        SimulateProperty simulateProperty = simulationData.getSimulateProperty().orElseThrow();

                        Block block = simulationData.blockState.getBlock();

                        float pickOdds;

                        if (simulateProperty.isPrecipitation) {
                            pickOdds = precipitationPickOdds;
                        } else {
                            pickOdds = randomPickOdds;
                        }

                        var result = block.simulateProperty(simulationData.blockState, level, simulationData.position, simulateProperty, random, simulationStepDuration, pickOdds, false, simulationData);

                        if (result == null) {
                            pendingRemoval.add(simulationData);
                            continue;
                        }

                        if (!result.getRight().equals(simulationData.position)) {
                            pendingRemoval.add(simulationData);
                            continue;
                        }

                        simulationData.blockState = result.getLeft();

                        Block newBlock = simulationData.blockState.getBlock();
                        if (newBlock != block) {
                            Optional<GroupMemberInfo> maybeGroupMemberInfo = GroupInfoResource.getBlockMemberInfo(newBlock)
                                .stream()
                                .filter(info -> info.groupInfo == groupInfo)
                                .findFirst();

                            if (maybeGroupMemberInfo.isPresent()) {
                                var newSimulateProperty = newBlock.getSimulationData().propertyMap.values().stream().filter(property -> property.simulateWithGroup.equals(Optional.of(groupId))).findFirst();
                                simulationData.setSimulateProperty(newSimulateProperty);
                                pendingUpdateMembership.add(Pair.of(simulationData, maybeGroupMemberInfo.get()));
                            } else {
                                pendingRemoval.add(simulationData);
                            }
                            continue;
                        }

                        if (block.isPropertyFinished(simulationData.blockState, level, simulationData.position, simulateProperty)) {
                            simulationData.isActive = false;
                        }
                    }

                    for (var removingData : pendingRemoval) {
                        removingData.isActive = false;
                        for (var nearData : removingData.surroundingData) {
                            nearData.surroundingData.removeIf(data -> data == removingData);
                        }
                    }

                    for (var pair : pendingUpdateMembership) {
                        ActiveGroupSimulateData updatingData = pair.getFirst();
                        updatingData.groupMemberInfo = pair.getSecond();
                    }

                    group.removeIf(data -> !data.isActive);

                    remainingCycles -= simulationStepDuration;


                }
            }
        }

        return Pair.of(simulatedGroups, !missedGroup);
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

        float randomPickChance = MathUtils.getRandomPickOdds(randomTickSpeed);
        float precipitationPickChance = 1F/4096F; //1/(16*(16*16)). 16 for the chance of the chunk doing the tick and (16*16) for the chance of a block to be picked.

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

            SimulationData simulationData = block.getSimulationData();

            if (UnloadedActivity.config.debugLogs)
                if (!state.isAir())
                    UnloadedActivity.LOGGER.info("Simulating block " + block + " with " + simulationData.propertyMap.size() + " properties.");


            ArrayList<Pair<String, SimulateProperty>> pendingProperties = new ArrayList<>(simulationData.propertyMap.size());

            for (var entry : simulationData.propertyMap.entrySet()) {

                String propertyName = entry.getKey();
                var simulateProperty = entry.getValue();

                if (block.isPropertyFinished(state, level, pos, simulateProperty)) {
                    finishedProperties.add(Pair.of(propertyName, 0L));
                } else {
                    pendingProperties.add(Pair.of(propertyName, simulateProperty));
                }

                propertiesWithDependents.addAll(simulateProperty.dependencies);
            }

            boolean continueCheck = true;

            while (continueCheck) {
                continueCheck = false;

                var iterator = pendingProperties.iterator();

                while (iterator.hasNext()) {
                    var entry = iterator.next();

                    boolean validDependencies = true;
                    long maxDuration = 0;

                    var simulateProperty = entry.getSecond();

                    if (simulateProperty.isPrecipitation && (!allowPrecipitationTicks || !UnloadedActivity.config.enableSimulatingPrecipitationTicks)) {
                        continue;
                    }
                    if (!simulateProperty.isPrecipitation && !UnloadedActivity.config.enableSimulatingRandomTicks) {
                        continue;
                    }
                    if (simulateProperty.simulateWithGroup.isPresent()) {
                        continue;
                    }
                    var propertyName = entry.getFirst();
                    for (String dependency : simulateProperty.dependencies) {
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

                    if (!block.canSimulateProperty(state, level, pos, simulateProperty)) {
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

                    float pickChance = simulateProperty.isPrecipitation ? precipitationPickChance : randomPickChance;

                    var result = block.simulateProperty(state, level, pos, simulateProperty, GameUtils.getRand(level), simulateTime, pickChance, propertiesWithDependents.contains(propertyName), null);
                    if (result == null) {
                        continueCheck = false;
                        break;
                    }

                    state = result.getLeft();
                    pos = result.getRight();

                    long duration = result.getMiddle().duration();

                    assert (duration <= simulateTime);

                    long simulationDuration = result.getMiddle().duration() + maxDuration;

                    assert (simulationDuration <= timeLeft);

                    if (state.getBlock() != block) {
                        continueCheck = false;
                        timeLeft -= simulationDuration;
                        if (timeLeft > 0)
                            blockHasChanged = true;
                        break;
                    }

                    if (block.isPropertyFinished(state, level, pos, simulateProperty)) {
                        continueCheck = true;
                        finishedProperties.add(Pair.of(propertyName, simulationDuration));
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
