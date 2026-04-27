package lol.zanspace.unloadedactivity;

import com.mojang.datafixers.util.Pair;
import lol.zanspace.unloadedactivity.datapack.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.*;

public class TimeMachine {
    public static long simulateChunk(long timeDifference, ServerLevel level, LevelChunk chunk, int randomTickSpeed) {
        if (!UnloadedActivity.config.enableRandomTicks || !UnloadedActivity.config.enablePrecipitationTicks) return 0;
        
        long now = 0;
        if (UnloadedActivity.config.debugLogs) now = Instant.now().toEpochMilli();

        TimeMachine.simulateGroupTicks(level, chunk, randomTickSpeed);
        TimeMachine.simulateTicks(timeDifference, level, chunk, randomTickSpeed);

        long msTime = 0;

        if (UnloadedActivity.config.debugLogs) {
            msTime = Instant.now().toEpochMilli() - now;
            UnloadedActivity.LOGGER.info(msTime + "ms to simulate random ticks on chunk after " + timeDifference + " ticks.");
        };
        return msTime;
    }

    public static void simulateBlockPrecipitationTick(BlockPos pos, ServerLevel level, long timeDifference, float precipitationPickChance, long timeInWeather, Biome.Precipitation precipitation) {
        if (!UnloadedActivity.config.enablePrecipitationTicks)
            return;

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (!block.canSimulatePrecTicks(state, level, pos, timeInWeather, precipitation))
            return;

        block.simulatePrecTicks(state, level, pos, timeInWeather, timeDifference, precipitation, precipitationPickChance);
    }

    public static List<BlockPos> getRandomTickableBlocks(LevelChunk chunk) {
        Level level = chunk.getLevel();

        #if MC_VER >= MC_1_21_3
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        #else
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        #endif

        ArrayList<BlockPos> blockPosArray = new ArrayList<>();

        ArrayList<Long> newSimulationBlocks = new ArrayList<>();

        if (UnloadedActivity.config.rememberBlockPositions && chunk.getSimulationVersion() == UnloadedActivity.chunkSimVer) {

            ArrayList<Long> currentSimulationBlocks = chunk.getSimulationBlocks();

            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Looping through "+currentSimulationBlocks.size()+" known positions.");

            boolean removedSomething = false;

            for (long longPos : currentSimulationBlocks) {
                BlockPos pos = BlockPos.of(longPos);
                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();
                if (block.hasRandTicks()) {
                    newSimulationBlocks.add(longPos);
                    blockPosArray.add(pos);
                } else {
                    removedSomething = true;
                }
            }
            if (removedSomething) {
                chunk.setSimulationBlocks(newSimulationBlocks);
                if (UnloadedActivity.config.debugLogs)
                    UnloadedActivity.LOGGER.info("Removed "+(currentSimulationBlocks.size()-newSimulationBlocks.size())+" positions.");
            }

        } else {
            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Looping through entire chunk.");

            for (int z=0; z<16;z++)
                for (int x=0; x<16;x++)
                    for (int y=minY; y<maxY;y++) {
                        BlockPos chunkBlockPos = new BlockPos(x,y,z);
                        ChunkPos chunkPos = chunk.getPos();
                        BlockPos worldBlockPos = chunkBlockPos.offset(chunkPos.x*16,0,chunkPos.z*16);
                        BlockState state = chunk.getBlockState(chunkBlockPos);
                        Block block = state.getBlock();
                        if (block.hasRandTicks()) {
                            blockPosArray.add(worldBlockPos);
                            if (UnloadedActivity.config.rememberBlockPositions)
                                newSimulationBlocks.add(worldBlockPos.asLong());
                        }
                    }
            if (UnloadedActivity.config.rememberBlockPositions) {
                chunk.setSimulationBlocks(newSimulationBlocks);
                chunk.setSimulationVersion(UnloadedActivity.chunkSimVer);
                #if MC_VER >= MC_1_21_3
                chunk.markUnsaved();
                #else
                chunk.setUnsaved(true);
                #endif
            }
        }

        return blockPosArray;
    }

    public static List<BlockPos> getPrecipitationTickableBlocks(LevelChunk chunk) {
        Level level = chunk.getLevel();

        ArrayList<BlockPos> precipitationBlocks = new ArrayList<>();

        for (int z=0; z<16;z++)
            for (int x=0; x<16;x++) {
                ChunkPos chunkPos = chunk.getPos();
                BlockPos airPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(chunkPos.x*16+x,0,chunkPos.z*16+z));
                BlockPos groundPos = airPos.below();
                BlockState airPosState = chunk.getBlockState(airPos);
                BlockState groundPosState = chunk.getBlockState(groundPos);
                Block airPosBlock = airPosState.getBlock();
                Block groundPosBlock = groundPosState.getBlock();

                if (airPosBlock.hasPrecTicks())
                    precipitationBlocks.add(airPos);

                if (groundPosBlock.hasPrecTicks())
                    precipitationBlocks.add(groundPos);
            }

        return precipitationBlocks;
    }

    public static Map<ResourceLocation, List<Triple<BlockPos, BlockState, SimulateProperty>>> getGroupSimulationsInChunk(LevelChunk chunk) {
        Map<ResourceLocation, List<Triple<BlockPos, BlockState, SimulateProperty>>> groupSimulations = new HashMap<>();

        List<BlockPos> randomTickBlocks = getRandomTickableBlocks(chunk);
        List<BlockPos> precipitationTickBlocks = getPrecipitationTickableBlocks(chunk);

        // Merge randomTickBlocks and precipitationTickBlocks without duplicates.
        List<BlockPos> potentialBlocks = new ArrayList<>(precipitationTickBlocks);
        potentialBlocks.removeAll(randomTickBlocks);
        potentialBlocks.addAll(randomTickBlocks);

        for (BlockPos blockPos : potentialBlocks) {
            BlockState blockState = chunk.getBlockState(blockPos);
            Block block = blockState.getBlock();
            List<SimulateProperty> simulateProperties = block.getGroupSimulationProperties();
            for (SimulateProperty simulateProperty : simulateProperties) {
                var groupId = simulateProperty.simulateWithGroup.orElseThrow();

                List<Triple<BlockPos, BlockState, SimulateProperty>> positions = groupSimulations.computeIfAbsent(groupId, (s) -> new ArrayList<>());
                positions.add(Triple.of(blockPos, blockState, simulateProperty));
            }
        }

        return groupSimulations;
    }

    // This doesn't take a timeDifference parameter because that is supposed to be calculated in the function using the last group tick.
    public static void simulateGroupTicks(ServerLevel level, LevelChunk chunk, int randomTickSpeed) {
        Map<ResourceLocation, List<Triple<BlockPos, BlockState, SimulateProperty>>> groups = getGroupSimulationsInChunk(chunk);

        long currentTime = level.getDayTime();
        for (var entry : groups.entrySet()) {
            ResourceLocation groupId = entry.getKey();
            GroupInfo groupInfo = GroupInfoResource.GROUPS_MAP.get(groupId);

            if (groupInfo == null)
                continue;

            long lastGroupTick = chunk.getLastGroupTick(groupId);
            long groupTimeDifference = Math.max(currentTime - lastGroupTick, 0);

            if (groupTimeDifference <= UnloadedActivity.config.groupTickDifferenceThreshold) {
                chunk.setLastGroupTick(groupId, level.getDayTime());
                continue;
            }

            List<Triple<BlockPos, BlockState, SimulateProperty>> pendingBlockPositions = new ArrayList<>();
            List<Triple<BlockPos, BlockState, SimulateProperty>> checkingBlockPositions = new ArrayList<>(entry.getValue());

            Map<BlockPos, ActiveGroupSimulateData> activeGroupDataMap = new HashMap<>(UnloadedActivity.config.maxGroupTickSize);

            int forceLoadedChunks = 0;
            Set<ChunkPos> checkedChunks = new HashSet<>();
            checkedChunks.add(chunk.getPos());

            // Populate activeGroupDataMap
            while (!checkingBlockPositions.isEmpty()) {
                Set<ChunkPos> newChunks = new HashSet<>();

                List<ActiveGroupSimulateData> finalizingBlockData = new ArrayList<>(checkingBlockPositions.size());

                // Loop through all blocks.
                // Separate blocks that wants info from another chunk and blocks that have everything they need.
                for (var info : checkingBlockPositions) {
                    BlockPos blockPos = info.getLeft();
                    BlockState state = info.getMiddle();
                    SimulateProperty simulateProperty = info.getRight();

                    Optional<GroupMemberInfo> maybeGroupMemberInfo = GroupInfoResource.getBlockMemberInfo(state.getBlock())
                        .stream()
                        .filter((groupMemberInfo -> groupMemberInfo.groupInfo == groupInfo))
                        .findFirst();

                    if (maybeGroupMemberInfo.isEmpty())
                        continue;

                    GroupMemberInfo groupMemberInfo = maybeGroupMemberInfo.get();

                    boolean isActive = state.getBlock().canSimulateProperty(state, level, blockPos, simulateProperty);

                    ActiveGroupSimulateData activeGroupSimulateData = activeGroupDataMap.computeIfAbsent(blockPos, (pos) -> new ActiveGroupSimulateData(blockPos, state, simulateProperty, groupMemberInfo, isActive));

                    boolean intersectsNewChunks = false;

                    if (isActive) {
                        for (var offset : groupInfo.iterateOffsets()) {
                            ChunkPos chunkPos = new ChunkPos(blockPos.offset(offset));
                            boolean isNewChunk = !checkedChunks.contains(chunkPos);
                            if (isNewChunk) {
                                newChunks.add(chunkPos);
                                intersectsNewChunks = true;
                            }
                        }
                    }

                    if (intersectsNewChunks) {
                        pendingBlockPositions.add(info);
                    } else {
                        finalizingBlockData.add(activeGroupSimulateData);
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

                    if (!level.hasChunk(newChunkPos.x, newChunkPos.z)) {
                        if (forceLoadedChunks >= UnloadedActivity.config.maxForcedChunkLoads)
                            continue;
                        forceLoadedChunks += 1;
                    }

                    LevelChunk newChunk = level.getChunk(newChunkPos.x, newChunkPos.z);
                    long newLastGroupTick = newChunk.getLastGroupTick(groupId);
                    long differenceFromMainChunk = lastGroupTick - newLastGroupTick;
                    long differencePercentage = Math.abs(differenceFromMainChunk / groupTimeDifference);

                    if (differencePercentage > UnloadedActivity.config.groupChunkDifferencePercentage)
                        continue;

                    var newGroups = getGroupSimulationsInChunk(newChunk);
                    List<Triple<BlockPos, BlockState, SimulateProperty>> newInfo = newGroups.getOrDefault(groupId, List.of());
                    int newTotalSize = activeGroupDataMap.size() + newInfo.size();

                    if (newTotalSize > UnloadedActivity.config.maxGroupTickSize)
                        continue;


                    newChunk.setLastGroupTick(groupId, level.getDayTime());

                    checkingBlockPositions.addAll(newInfo);
                }

                // Prepare for next loop.
                checkingBlockPositions.addAll(pendingBlockPositions);
                pendingBlockPositions.clear();
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
            float randomPickOdds = Utils.getRandomPickOdds(randomTickSpeed);
            float precipitationPickOdds = 1F/4096F;

            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Simulating " + isolatedGroups.size() + " isolated groups");

            // Data has been made. Time to actually do the simulation.
            for (ArrayList<ActiveGroupSimulateData> group : isolatedGroups) {
                int totalIterations = 0;
                long remainingCycles = groupTimeDifference;
                long simulationCurrentTime = currentTime - remainingCycles;

                WorldWeatherData weatherData = level.getWeatherData();

                if (UnloadedActivity.config.debugLogs)
                    UnloadedActivity.LOGGER.info("Simulating isolated group of " + group.size() + " members");

                while (remainingCycles > 0 && totalIterations < UnloadedActivity.config.maxGroupTickIterations) {
                    long minProbabilityStepDuration = remainingCycles / (UnloadedActivity.config.maxGroupTickIterations - totalIterations);
                    totalIterations++;

                    long minNextOddsSwitchDuration = Long.MAX_VALUE;
                    long nextWeatherSwitchDuration = Long.MAX_VALUE;
                    float maxProbability = 0F;

                    for (ActiveGroupSimulateData simulationData : group) {
                        BlockState state = simulationData.blockState;
                        BlockPos pos = simulationData.position;
                        boolean isRaining = weatherData.getWeatherAtTime(simulationCurrentTime);

                        CalculationData calculationData = new CalculationData(level, state, pos, simulationCurrentTime, isRaining, false, simulationData);

                        long nextOddsSwitchDuration = simulationData.simulateProperty.advanceProbability.getNextValueSwitchDuration(calculationData);
                        minNextOddsSwitchDuration = Math.min(minNextOddsSwitchDuration, nextOddsSwitchDuration);

                        if (nextWeatherSwitchDuration == Long.MAX_VALUE && simulationData.simulateProperty.advanceProbability.isAffectedByWeather(calculationData)) {
                            nextWeatherSwitchDuration = weatherData.getNextWeatherChangeDuration(simulationCurrentTime);
                        }

                        float pickOdds;

                        if (simulationData.simulateProperty.isPrecipitation) {
                            pickOdds = precipitationPickOdds;
                        } else {
                            pickOdds = randomPickOdds;
                        }

                        float probability = simulationData.simulateProperty.advanceProbability.calculateValue(calculationData).floatValue() * pickOdds;

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
                        Block block = simulationData.blockState.getBlock();

                        float pickOdds;

                        if (simulationData.simulateProperty.isPrecipitation) {
                            pickOdds = precipitationPickOdds;
                        } else {
                            pickOdds = randomPickOdds;
                        }

                        var result = block.simulateProperty(simulationData.blockState, level, simulationData.position, simulationData.simulateProperty, random, simulationStepDuration, pickOdds, false, simulationData);

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
                                pendingUpdateMembership.add(Pair.of(simulationData, maybeGroupMemberInfo.get()));
                            } else {
                                pendingRemoval.add(simulationData);
                            }
                            continue;
                        }

                        if (block.isPropertyFinished(simulationData.blockState, level, simulationData.position, simulationData.simulateProperty)) {
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
    }

    public static void simulateTicks(long timeDifference, ServerLevel level, LevelChunk chunk, int randomTickSpeed) {

        if (!UnloadedActivity.config.enableRandomTicks)
            return;

        List<BlockPos> blockPosArray = getRandomTickableBlocks(chunk);

        if (UnloadedActivity.config.randomizeBlockUpdates) {
            Collections.shuffle(blockPosArray);
        }

        List<BlockPos> precipitationBlocks = getPrecipitationTickableBlocks(chunk);

        for (BlockPos blockPos : precipitationBlocks) {
            simulateBlock(blockPos, level, timeDifference, randomTickSpeed, true);
        }

        for (BlockPos blockPos : blockPosArray) {
            if (precipitationBlocks.contains(blockPos)) continue;
            simulateBlock(blockPos, level, timeDifference, randomTickSpeed, false);
        }
    }

    public static void simulateBlock(BlockPos pos, ServerLevel level, long timeLeft, int randomTickSpeed, boolean allowPrecipitationTicks) {

        float randomPickChance = Utils.getRandomPickOdds(randomTickSpeed);
        float precipitationPickChance = 1F/4096F; //1/(16*(16*16)). 16 for the chance of the chunk doing the tick and (16*16) for the chance of a block to be picked.

        BlockState state = level.getBlockState(pos);

        boolean blockHasChanged = true;

        while (blockHasChanged) {
            blockHasChanged = false;

            Block block = state.getBlock();


            Map<String, SimulateProperty> pendingProperties = new HashMap<>(block.getSimulationData().propertyMap);

            Map<String, Long> finishedProperties = new HashMap<>();

            Set<String> propertiesWithDependents = new HashSet<>();


            if (UnloadedActivity.config.debugLogs)
                if (!state.isAir())
                    UnloadedActivity.LOGGER.info("Simulating block " + block + " with " + pendingProperties.size() + " properties.");

            var pendingPropertiesIterator = pendingProperties.entrySet().iterator();
            while (pendingPropertiesIterator.hasNext()) {
                var entry = pendingPropertiesIterator.next();

                String propertyName = entry.getKey();
                var simulateProperty = entry.getValue();

                if (block.isPropertyFinished(state, level, pos, simulateProperty)) {
                    finishedProperties.put(propertyName, 0L);
                    pendingPropertiesIterator.remove();
                }

                propertiesWithDependents.addAll(simulateProperty.dependencies);
            }

            boolean continueCheck = true;

            while (continueCheck) {
                continueCheck = false;

                var iterator = pendingProperties.entrySet().iterator();

                while (iterator.hasNext()) {
                    var entry = iterator.next();

                    boolean validDependencies = true;
                    long maxDuration = 0;

                    var simulateProperty = entry.getValue();
                    if (simulateProperty.isPrecipitation && !allowPrecipitationTicks) {
                        continue;
                    }
                    if (simulateProperty.simulateWithGroup.isPresent()) {
                        continue;
                    }
                    var propertyName = entry.getKey();
                    for (String dependency : simulateProperty.dependencies) {
                        Long dependencyDuration = finishedProperties.get(dependency);

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

                    var result = block.simulateProperty(state, level, pos, simulateProperty, level.random, simulateTime, pickChance, propertiesWithDependents.contains(propertyName), null);
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
                        finishedProperties.put(propertyName, simulationDuration);
                    }
                }
            }
        }
    }

    public static <T extends BlockEntity> void simulateBlockEntity(ServerLevel level, BlockPos pos, BlockState blockState, T blockEntity, long timeDifference) {
        if (!UnloadedActivity.config.enableBlockEntities) return;

        long now = 0;
        if (UnloadedActivity.config.debugLogs) now = Instant.now().toEpochMilli();
        if (!blockEntity.unloaded_activity$canSimulate()) return;
        blockEntity.unloaded_activity$simulateTime(level, pos, blockState, timeDifference);
        if (UnloadedActivity.config.debugLogs) UnloadedActivity.LOGGER.info((Instant.now().toEpochMilli() - now) + "ms to simulate ticks on blockEntity after " + timeDifference + " ticks.");
    }

    public static void simulateEntity(Entity entity, long timeDifference) {

        if (!UnloadedActivity.config.enableEntities) return;
        if (!entity.canSimulate()) return;

        long now = 0;
        if (UnloadedActivity.config.debugLogs) now = Instant.now().toEpochMilli();

        entity.simulateTime(timeDifference);
        if (UnloadedActivity.config.debugLogs) UnloadedActivity.LOGGER.info((Instant.now().toEpochMilli() - now) + "ms to simulate ticks on entity after " + timeDifference + " ticks.");
    }
}
