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

        TimeMachine.simulateRandomTicks(timeDifference, level, chunk, randomTickSpeed);

        long msTime = 0;

        if (UnloadedActivity.config.debugLogs) {
            msTime = Instant.now().toEpochMilli() - now;
            UnloadedActivity.LOGGER.info(msTime + "ms to simulate random ticks on chunk after " + timeDifference + " ticks.");
        };
        return msTime;
    }

    public static void simulateBlockPrecipitationTick(BlockPos pos, ServerLevel level, long timeDifference, double precipitationPickChance, long timeInWeather, Biome.Precipitation precipitation) {
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

        double randomPickChance = Utils.getRandomPickOdds(randomTickSpeed);
        double precipitationPickChance = 1.0/4096.0; //1/(16*(16*16)). 16 for the chance of the chunk doing the tick and (16*16) for the chance of a block to be picked.

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

                    double pickChance = simulateProperty.isPrecipitation ? precipitationPickChance : randomPickChance;

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
