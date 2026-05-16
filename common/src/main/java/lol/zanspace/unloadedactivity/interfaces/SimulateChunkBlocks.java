package lol.zanspace.unloadedactivity.interfaces;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.turtle.Turtle;
#else
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Turtle;
#endif

#if MC_VER >= MC_1_21_3
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.properties.EnumProperty;
#else
import net.minecraft.world.level.block.state.properties.DirectionProperty;
#endif

import lol.zanspace.unloadedactivity.ActiveGroupSimulateData;
import lol.zanspace.unloadedactivity.MathUtils;
import lol.zanspace.unloadedactivity.GameUtils;
import lol.zanspace.unloadedactivity.OccurrencesAndDuration;
import lol.zanspace.unloadedactivity.datapack.*;
import lol.zanspace.unloadedactivity.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface SimulateChunkBlocks {

    SimulationData getSimulationData();

    static Optional<Property<?>> getProperty(BlockState state, String propertyName) {
        for (var property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return Optional.of(property);
            }
        }
        return Optional.empty();
    }

    default int getCurrentAgeUA(BlockState state) {
        return 0;
    }

    default int getMaxAgeUA() {
        return 0;
    }

    default List<SimulateProperty> getGroupSimulationProperties() {
        return getSimulationData().propertyMap.values().stream().filter(property -> property.simulateWithGroup.isPresent()).toList();
    };

    default Optional<SimulateProperty> getGroupSimulationProperty(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif groupId) {
        return getSimulationData().propertyMap.values()
            .stream()
            .filter(property -> property.simulateWithGroup.equals(Optional.of(groupId)))
            .findFirst();
    };

    default boolean canSimulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty) {
        boolean isFinished = isPropertyFinished(state, level, pos, simulateProperty);
        if (isFinished)
            return false;

        return hasValidConditions(state, level, pos, simulateProperty);
    }

    default boolean hasValidConditions(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty) {
        CalculationData calculationData = new CalculationData(level, state, pos);

        for (Condition condition : simulateProperty.conditions) {
            if (!condition.isValid(calculationData)) {
                return false;
            }
        }

        return true;
    }

    default boolean isPropertyFinished(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty) {
        if (!simulateProperty.isBudding() && !simulateProperty.isDecay() && (simulateProperty.increasePerHeight || simulateProperty.maxHeight.isPresent())) {
            Block thisBlock = state.getBlock();

            BlockState blockStateAbove;

            if (simulateProperty.reverseHeightGrowthDirection) {
                blockStateAbove = level.getBlockState(pos.below());
            } else {
                blockStateAbove = level.getBlockState(pos.above());
            }

            boolean emptyAbove = blockStateAbove.isAir();
            boolean blockingAbove = !emptyAbove;
            boolean continuesAbove = blockStateAbove.is(thisBlock);
            boolean stopUpdatingAfterMaxHeight = !simulateProperty.keepUpdatingAfterMaxHeight;
            if (blockingAbove && (stopUpdatingAfterMaxHeight || continuesAbove)) {
                return true;
            }

            if (simulateProperty.maxHeight.isPresent()) {
                int maxHeight = simulateProperty.maxHeight.get();

                Block lowerBlock = simulateProperty.blockReplacement.map((blockCalculateValue -> blockCalculateValue.calculateValue(new CalculationData(level, state, pos)))).orElse(thisBlock);

                int height;
                if (simulateProperty.reverseHeightGrowthDirection) {
                    for(height = 1; level.getBlockState(pos.above(height)).is(lowerBlock) && height <= maxHeight; ++height) {}
                } else {
                    for(height = 1; level.getBlockState(pos.below(height)).is(lowerBlock) && height <= maxHeight; ++height) {}
                }

                if (height < maxHeight) {
                    return false;
                }
            }

            if (stopUpdatingAfterMaxHeight) {
                return true;
            }
        }

        switch (simulateProperty.simulationType) {
            case PROPERTY -> {
                Optional<Property<?>> maybeProperty = getProperty(state, simulateProperty.target);

                if (maybeProperty.isPresent()) {
                    Property<?> property = maybeProperty.get();

                    int propertyMax;
                    int max;
                    int current;

                    if (property instanceof IntegerProperty integerProperty) {
                        propertyMax = ((IntegerPropertyAccessor)integerProperty).unloaded_activity$getMax();
                        max = propertyMax;
                        current = state.getValue(integerProperty);
                    } else if (property instanceof BooleanProperty booleanProperty) {
                        propertyMax = 1;
                        max = 1;
                        current = state.getValue(booleanProperty) ? 1 : 0;
                    } else {
                        return true;
                    }

                    if (simulateProperty.maxValue.isPresent()) {
                        CalculateValue<Number> maxValue = simulateProperty.maxValue.get();
                        Number calculated = maxValue.calculateValue(new CalculationData(level, state, pos));
                        max = Math.min(propertyMax, calculated.intValue());
                    }

                    return current >= max;
                }
            }
            case BUDDING -> {
                var buddingBlocks = simulateProperty.buddingBlocks;
                Block finalBlock = buddingBlocks.get(buddingBlocks.size()-1);

                List<Direction> availableDirections = Arrays.stream(Direction.values()).filter(direction -> !simulateProperty.ignoreBuddingDirections.contains(direction)).toList();

                for (Direction direction : availableDirections) {
                    BlockPos dirPos = pos.relative(direction);
                    BlockState dirBlockState = level.getBlockState(dirPos);
                    if (!dirBlockState.is(finalBlock)) {
                        return false;
                    }

                    if (simulateProperty.buddingDirectionProperty.isPresent()) {
                        var property = (#if MC_VER >= MC_1_21_3 EnumProperty<?> #else DirectionProperty #endif) getProperty(dirBlockState, simulateProperty.buddingDirectionProperty.get()).get();

                        Direction blockDirection = (Direction) dirBlockState.getValue(property);
                        if (blockDirection != direction) {
                            return false;
                        }
                    }
                }
            }

            case DECAY -> {
                return false;
            }
        }

        return true;
    }

    default boolean shouldCalculateDuration(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty) {
        if (simulateProperty.hatchEntity.isPresent()) {
            return true;
        }

        if (simulateProperty.simulationType == SimulationType.DECAY) {
            if (simulateProperty.blockReplacement.isPresent()) {
                Block blockReplacement = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                SimulationData simulationData = blockReplacement.getSimulationData();
                if (simulationData.hasRandTicksWithoutGroup || simulationData.hasPrecTicksWithoutGroup) {
                    return true;
                }
            }
        }
        return false;
    }

    default int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty) {
        switch (simulateProperty.simulationType) {
            case PROPERTY -> {
                Optional<Property<?>> maybeProperty = getProperty(state, simulateProperty.target);

                if (maybeProperty.isEmpty())
                    return 0;

                Property<?> property = maybeProperty.get();

                Block thisBlock = state.getBlock();
                int propertyMax;
                int max;
                int current;
                int updateCount;

                if (property instanceof IntegerProperty integerProperty) {
                    propertyMax = ((IntegerPropertyAccessor)integerProperty).unloaded_activity$getMax();
                    max = propertyMax;
                    current = state.getValue(integerProperty);
                    updateCount = max - current;

                } else if (property instanceof BooleanProperty booleanProperty) {
                    propertyMax = 1;
                    max = 1;
                    current = state.getValue(booleanProperty) ? 1 : 0;
                    updateCount = 1 - current;

                } else {
                    return 0;
                }

                if (simulateProperty.maxValue.isPresent()) {
                    CalculateValue<Number> maxValue = simulateProperty.maxValue.get();
                    Number calculated = maxValue.calculateValue(new CalculationData(level, state, pos));
                    max = Math.min(propertyMax, calculated.intValue());
                }


                if (simulateProperty.maxHeight.isPresent() || simulateProperty.increasePerHeight) {
                    Block lowerBlock;
                    if (simulateProperty.blockReplacement.isPresent()) {
                        lowerBlock = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                    } else {
                        lowerBlock = thisBlock;
                    }

                    int freeSpaceLimit = Integer.MAX_VALUE;

                    if (simulateProperty.maxHeight.isPresent()) {
                        int maxHeight = simulateProperty.maxHeight.get();

                        int height;
                        if (simulateProperty.reverseHeightGrowthDirection) {
                            for(height = 1; level.getBlockState(pos.above(height)).is(lowerBlock) && height <= maxHeight; ++height) {}
                        } else {
                            for(height = 1; level.getBlockState(pos.below(height)).is(lowerBlock) && height <= maxHeight; ++height) {}
                        }

                        freeSpaceLimit = Math.min(freeSpaceLimit, maxHeight - height);
                    }

                    if (simulateProperty.increasePerHeight) {
                        freeSpaceLimit = Math.min(freeSpaceLimit, updateCount);
                    }



                    int freeSpace;
                    if (simulateProperty.onlyInWater) {
                        if (simulateProperty.reverseHeightGrowthDirection) {
                            for(freeSpace = 1; level.getBlockState(pos.below(freeSpace)).is(Blocks.WATER) && freeSpace <= freeSpaceLimit; ++freeSpace) {}
                        } else {
                            for(freeSpace = 1; level.getBlockState(pos.above(freeSpace)).is(Blocks.WATER) && freeSpace <= freeSpaceLimit; ++freeSpace) {}
                        }
                    } else {
                        if (simulateProperty.reverseHeightGrowthDirection) {
                            for(freeSpace = 1; level.isEmptyBlock(pos.below(freeSpace)) && freeSpace <= freeSpaceLimit; ++freeSpace) {}
                        } else {
                            for(freeSpace = 1; level.isEmptyBlock(pos.above(freeSpace)) && freeSpace <= freeSpaceLimit; ++freeSpace) {}
                        }
                    }
                    --freeSpace;

                    // Updates for growing in height
                    if (simulateProperty.increasePerHeight) {
                        updateCount = freeSpace;
                    } else {
                        updateCount += freeSpace;

                        boolean stopUpdatingAfterMaxHeight = !simulateProperty.keepUpdatingAfterMaxHeight;

                        if (stopUpdatingAfterMaxHeight) {
                            updateCount += max * Math.max(freeSpace - 1, 0);
                        } else {
                            updateCount += max * freeSpace;
                        }
                    }
                }

                return updateCount;
            }
            case DECAY -> {
                return 1;
            }
        }

        throw new RuntimeException("Simulation type " + simulateProperty.simulationType + " is not able to be separated.");
    }

    default List<Pair<BlockPos, BlockState>> getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty, int occurrences, long simulationDuration, long timePassed, @Nullable ActiveGroupSimulateData groupSimulateData) {
        ArrayList<Pair<BlockPos, BlockState>> updateList = new ArrayList<>();
        switch (simulateProperty.simulationType) {
            case PROPERTY -> {
                Optional<Property<?>> maybeProperty = getProperty(state, simulateProperty.target);

                if (maybeProperty.isEmpty())
                    return updateList;

                Property<?> property = maybeProperty.get();

                Block thisBlock = state.getBlock();
                int propertyMax;
                int max;
                int current;

                if (property instanceof IntegerProperty integerProperty) {
                    propertyMax = ((IntegerPropertyAccessor)integerProperty).unloaded_activity$getMax();
                    max = propertyMax;
                    current = state.getValue(integerProperty);

                } else if (property instanceof BooleanProperty booleanProperty) {
                    propertyMax = 1;
                    max = 1;
                    current = state.getValue(booleanProperty) ? 1 : 0;

                } else {
                    return updateList;
                }

                int newPropertyValue = current + occurrences;

                if (simulateProperty.increasePerHeight) {
                    if (simulateProperty.blockReplacement.isPresent()) {
                        Block newBlock = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                        BlockState newState = newBlock.defaultBlockState();

                        for (String propertyName : simulateProperty.transferProperties) {
                            Optional<Property<?>> maybeNewProperty = getProperty(newState, propertyName);

                            if (maybeNewProperty.isEmpty()) {
                                continue;
                            }

                            Optional<Property<?>> maybeOldProperty = getProperty(state, propertyName);

                            if (maybeOldProperty.isEmpty()) {
                                continue;
                            }

                            // Sick and twisted workaround.
                            Object oldValue = state.getValue(maybeOldProperty.get());
                            Object valueFromNew = newState.getValue(maybeNewProperty.get());
                            if (oldValue.getClass().isInstance(valueFromNew)) {
                                newState = newState.setValue((Property)maybeNewProperty.get(), (Comparable)oldValue);
                            }
                        }


                        state = newState;
                        updateList.add(Pair.of(pos, state));
                    }


                    for (int i=0;i<occurrences;i++) {
                        if (simulateProperty.reverseHeightGrowthDirection) {
                            pos = pos.below();
                        } else {
                            pos = pos.above();
                        }

                        boolean isFinal = i+1 == occurrences;

                        if (simulateProperty.blockReplacement.isPresent() && !isFinal) {
                            Block newBlock = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                            state = newBlock.defaultBlockState();
                        } else {
                            int newValue = current + (i + 1);
                            if (property instanceof IntegerProperty integerProperty) {
                                state = thisBlock.defaultBlockState().setValue(integerProperty, newValue);
                            } else if (property instanceof BooleanProperty booleanProperty) {
                                state = thisBlock.defaultBlockState().setValue(booleanProperty, newValue > 0);
                            }
                        }

                        for (var setProperty : simulateProperty.setProperties) {
                            String propertyName = setProperty.getFirst();
                            CalculateValue<Number> propertyValue = setProperty.getSecond();
                            Optional<Property<?>> maybeSetProperty = SimulateChunkBlocks.getProperty(state, propertyName);
                            if (maybeSetProperty.isPresent()) {
                                Property<?> newSetProperty = maybeSetProperty.get();
                                if (newSetProperty instanceof BooleanProperty booleanProperty) {
                                    float value = propertyValue.calculateValue(new CalculationData(level, state, pos)).floatValue();
                                    state = state.setValue(booleanProperty, value != 0);
                                }
                                if (newSetProperty instanceof IntegerProperty integerProperty) {
                                    int value = propertyValue.calculateValue(new CalculationData(level, state, pos)).intValue();
                                    state = state.setValue(integerProperty, value);
                                }
                            }
                        }

                        updateList.add(Pair.of(pos, state));
                    }
                } else if (simulateProperty.maxHeight.isPresent()) {
                    int growBlocks = newPropertyValue/(max + 1);
                    int valueRemainer = newPropertyValue % (max + 1);

                    boolean resetOnHeightChange = simulateProperty.resetOnHeightChange;

                    int belowValue = resetOnHeightChange ? 0 : max;

                    if (growBlocks == 0) {
                        if (property instanceof IntegerProperty integerProperty) {
                            state = state.setValue(integerProperty, valueRemainer);
                        } else if (property instanceof BooleanProperty booleanProperty) {
                            state = state.setValue(booleanProperty, valueRemainer > 0);
                        }
                    } else if (simulateProperty.blockReplacement.isPresent()) {
                        Block newBlock = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                        BlockState newState = newBlock.defaultBlockState();

                        for (String propertyName : simulateProperty.transferProperties) {
                            Optional<Property<?>> maybeNewProperty = getProperty(newState, propertyName);

                            if (maybeNewProperty.isEmpty()) {
                                continue;
                            }

                            Optional<Property<?>> maybeOldProperty = getProperty(state, propertyName);

                            if (maybeOldProperty.isEmpty()) {
                                continue;
                            }

                            // Sick and twisted workaround.
                            Object oldValue = state.getValue(maybeOldProperty.get());
                            Object valueFromNew = newState.getValue(maybeNewProperty.get());
                            if (oldValue.getClass().isInstance(valueFromNew)) {
                                newState = newState.setValue((Property)maybeNewProperty.get(), (Comparable)oldValue);
                            }
                        }

                        state = newState;
                    } else {
                        if (property instanceof IntegerProperty integerProperty) {
                            state = state.setValue(integerProperty, belowValue);
                        } else if (property instanceof BooleanProperty booleanProperty) {
                            state = state.setValue(booleanProperty, belowValue > 0);
                        }
                    }

                    updateList.add(Pair.of(pos, state));

                    for (int i=0;i<growBlocks;i++) {
                        if (simulateProperty.reverseHeightGrowthDirection) {
                            pos = pos.below();
                        } else {
                            pos = pos.above();
                        }

                        if (i+1==growBlocks) {
                            if (property instanceof IntegerProperty integerProperty) {
                                state = thisBlock.defaultBlockState().setValue(integerProperty, valueRemainer);
                            } else if (property instanceof BooleanProperty booleanProperty) {
                                state = thisBlock.defaultBlockState().setValue(booleanProperty, valueRemainer > 0);
                            }
                        } else if (simulateProperty.blockReplacement.isPresent()) {
                            Block newBlock = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                            BlockState newState = newBlock.defaultBlockState();

                            for (String propertyName : simulateProperty.transferProperties) {
                                Optional<Property<?>> maybeNewProperty = getProperty(newState, propertyName);

                                if (maybeNewProperty.isEmpty()) {
                                    continue;
                                }

                                Optional<Property<?>> maybeOldProperty = getProperty(state, propertyName);

                                if (maybeOldProperty.isEmpty()) {
                                    continue;
                                }

                                // Sick and twisted workaround.
                                Object oldValue = state.getValue(maybeOldProperty.get());
                                Object valueFromNew = newState.getValue(maybeNewProperty.get());
                                if (oldValue.getClass().isInstance(valueFromNew)) {
                                    newState = newState.setValue((Property)maybeNewProperty.get(), (Comparable)oldValue);
                                }
                            }

                            state = newState;
                        } else {
                            if (property instanceof IntegerProperty integerProperty) {
                                state = thisBlock.defaultBlockState().setValue(integerProperty, belowValue);
                            } else if (property instanceof BooleanProperty booleanProperty) {
                                state = thisBlock.defaultBlockState().setValue(booleanProperty, belowValue > 0);
                            }
                        }

                        for (var setProperty : simulateProperty.setProperties) {
                            String propertyName = setProperty.getFirst();
                            CalculateValue<Number> propertyValue = setProperty.getSecond();
                            Optional<Property<?>> maybeSetProperty = SimulateChunkBlocks.getProperty(state, propertyName);
                            if (maybeSetProperty.isPresent()) {
                                Property<?> newSetProperty = maybeSetProperty.get();
                                if (newSetProperty instanceof BooleanProperty booleanProperty) {
                                    float value = propertyValue.calculateValue(new CalculationData(level, state, pos)).floatValue();
                                    state = state.setValue(booleanProperty, value != 0);
                                }
                                if (newSetProperty instanceof IntegerProperty integerProperty) {
                                    int value = propertyValue.calculateValue(new CalculationData(level, state, pos)).intValue();
                                    state = state.setValue(integerProperty, value);
                                }
                            }
                        }
                        updateList.add(Pair.of(pos, state));
                    }
                } else {
                    if (property instanceof IntegerProperty integerProperty) {
                        state = state.setValue(integerProperty, newPropertyValue);
                    } else if (property instanceof BooleanProperty booleanProperty) {
                        state = state.setValue(booleanProperty, newPropertyValue > 0);
                    }
                    if (property instanceof IntegerProperty integerProperty) {
                        state = state.setValue(integerProperty, newPropertyValue);
                    } else if (property instanceof BooleanProperty booleanProperty) {
                        state = state.setValue(booleanProperty, newPropertyValue > 0);
                    }
                    updateList.add(Pair.of(pos, state));
                }

                return updateList;
            }
            case DECAY -> {
                if (simulateProperty.dropsResources) {
                    Block.dropResources(state, level, pos);
                }

                BlockState oldState = state;

                if (simulateProperty.blockReplacement.isPresent()) {
                    Block blockReplacement = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos, -1, false, false, groupSimulateData));
                    BlockState newState = blockReplacement.defaultBlockState();
                    for (String propertyName : simulateProperty.transferProperties) {
                        Optional<Property<?>> maybeNewProperty = getProperty(newState, propertyName);

                        if (maybeNewProperty.isEmpty()) {
                            continue;
                        }

                        Optional<Property<?>> maybeOldProperty = getProperty(state, propertyName);

                        if (maybeOldProperty.isEmpty()) {
                            continue;
                        }

                        // Sick and twisted workaround.
                        Object oldValue = state.getValue(maybeOldProperty.get());
                        Object valueFromNew = newState.getValue(maybeNewProperty.get());
                        if (oldValue.getClass().isInstance(valueFromNew)) {
                            newState = newState.setValue((Property)maybeNewProperty.get(), (Comparable)oldValue);
                        }
                    }
                    for (var setProperty : simulateProperty.setProperties) {
                        String propertyName = setProperty.getFirst();
                        CalculateValue<Number> propertyValue = setProperty.getSecond();
                        Optional<Property<?>> maybeSetProperty = SimulateChunkBlocks.getProperty(newState, propertyName);
                        if (maybeSetProperty.isPresent()) {
                            Property<?> newSetProperty = maybeSetProperty.get();
                            if (newSetProperty instanceof BooleanProperty booleanProperty) {
                                float value = propertyValue.calculateValue(new CalculationData(level, newState, pos)).floatValue();
                                newState = newState.setValue(booleanProperty, value != 0);
                            }
                            if (newSetProperty instanceof IntegerProperty integerProperty) {
                                int value = propertyValue.calculateValue(new CalculationData(level, newState, pos)).intValue();
                                newState = newState.setValue(integerProperty, value);
                            }
                        }
                    }
                    state = newState;
                    updateList.add(Pair.of(pos, state));
                } else {
                    updateList.add(Pair.of(pos, state.getFluidState().createLegacyBlock()));
                }

                if (simulateProperty.hatchEntity.isPresent()) {
                    int hatchCount;
                    if (simulateProperty.hatchCount.isPresent()) {
                        long hatchTime = GameUtils.getTime(level) - timePassed + simulationDuration;
                        hatchCount = (int)simulateProperty.hatchCount.get().calculateValue(new CalculationData(level, oldState, pos, hatchTime));
                    } else {
                        hatchCount = 1;
                    }

                    if (hatchCount > 0) {
                        for(int i = 0; i < hatchCount; i++) {
                            Entity hatchedEntity = simulateProperty.hatchEntity.get().create(
                                level
                                #if MC_VER >= MC_1_21_3 ,EntitySpawnReason.BREEDING #endif
                            );
                            if (hatchedEntity == null)
                                continue;

                            #if MC_VER >= MC_1_21_5
                            hatchedEntity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                            #else
                            hatchedEntity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                            #endif

                            if (simulateProperty.startingAge.isPresent() && hatchedEntity instanceof AgeableMob ageableMob) {
                                ageableMob.setAge(simulateProperty.startingAge.get());
                            }

                            if (hatchedEntity instanceof Turtle turtle) {
                                turtle.setHomePos(pos);
                            }

                            level.addFreshEntity(hatchedEntity);
                            hatchedEntity.unloadedactivity$simulateTime(timePassed - simulationDuration);
                        }
                    }
                }
                return updateList;
            }
        }
        return updateList;
    }

    default @Nullable Triple<BlockState, OccurrencesAndDuration, BlockPos> simulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty, RandomSource random, long timePassed, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData) {
        long currentTime = GameUtils.getTime(level);

        if (simulateProperty.simulationType == SimulationType.ACTION)
            return Triple.of(state, OccurrencesAndDuration.empty(), pos);

        if (simulateProperty.simulationType == SimulationType.BUDDING) {
            List<Direction> availableDirections = Arrays.stream(Direction.values()).filter(direction -> !simulateProperty.ignoreBuddingDirections.contains(direction)).toList();

            for(Direction direction : availableDirections) {
                BlockPos budPos = pos.relative(direction);
                BlockState budState = level.getBlockState(budPos);

                int stage = 0;


                boolean doContinue = false;

                for (int i=0;i<simulateProperty.buddingBlocks.size();i++) {
                    Block buddingBlockStage = simulateProperty.buddingBlocks.get(i);

                    if (budState.is(buddingBlockStage)) {
                        var property = (#if MC_VER >= MC_1_21_3 EnumProperty<?> #else DirectionProperty #endif) getProperty(budState, simulateProperty.buddingDirectionProperty.get()).get();
                        Direction budDirection = (Direction) budState.getValue(property);

                        if (budDirection == direction) {
                            stage = i+1;
                        } else {
                            doContinue = true;
                        }

                        break;
                    }
                }

                if (doContinue)
                    continue;


                if (stage == simulateProperty.buddingBlocks.size())
                    continue;


                if (stage == 0) {
                    if (!budState.isAir()) {
                        if (simulateProperty.waterloggedProperty.isEmpty()) {
                            continue;
                        }

                        if (!budState.is(Blocks.WATER))
                            continue;

                        if (budState.getFluidState().getAmount() < simulateProperty.minWaterValue)
                            continue;
                    }
                    // It's either air or water.
                }

                int maxOccurrences = simulateProperty.buddingBlocks.size() - stage;

                OccurrencesAndDuration result = MathUtils.getOccurrences(level, state, pos, currentTime, timePassed, simulateProperty, maxOccurrences, randomPickOdds, hasDependents, random, groupSimulateData);

                if (result.occurrences() == 0) {
                    continue;
                }

                int newStage = stage + result.occurrences();

                Block newBudBlock = simulateProperty.buddingBlocks.get(newStage - 1);

                BlockState newBudState = newBudBlock.defaultBlockState();

                if (simulateProperty.buddingDirectionProperty.isPresent()) {
                    String buddingDirectionPropertyName = simulateProperty.buddingDirectionProperty.get();
                    @SuppressWarnings("unchecked")
                    var property = (#if MC_VER >= MC_1_21_3 EnumProperty<Direction> #else DirectionProperty #endif) getProperty(newBudState, buddingDirectionPropertyName).get();
                    newBudState = newBudState.setValue(property, direction);
                }


                if (simulateProperty.waterloggedProperty.isPresent()) {
                    BooleanProperty waterloggedProperty = (BooleanProperty)getProperty(newBudState, simulateProperty.waterloggedProperty.get()).get();
                    newBudState = newBudState.setValue(waterloggedProperty, budState.getFluidState().getType() == Fluids.WATER);
                }

                level.setBlock(budPos, newBudState, simulateProperty.updateType);
            }

            return Triple.of(state, OccurrencesAndDuration.empty(), pos);
        }

        int updateCount = getMaxUpdateCount(state, level, pos, simulateProperty);
        boolean calculateDuration = hasDependents || shouldCalculateDuration(state, level, pos, simulateProperty);

        if (updateCount <= 0)
            return Triple.of(state, OccurrencesAndDuration.empty(), pos);

        OccurrencesAndDuration result = MathUtils.getOccurrences(level, state, pos, currentTime, timePassed, simulateProperty, updateCount, randomPickOdds, calculateDuration, random, groupSimulateData);

        if (result.occurrences() == 0)
            return Triple.of(state, result, pos);


        List<Pair<BlockPos, BlockState>> newBlockStates = getNewBlockStates(state, level, pos, simulateProperty, result.occurrences(), result.duration(), timePassed, groupSimulateData);

        Block thisBlock = state.getBlock();

        for (var pair : newBlockStates) {
            BlockPos newPos = pair.getFirst();
            BlockState newState = pair.getSecond();
            if (newPos.equals(pos) && newState.getBlock().equals(thisBlock)) {
                level.setBlock(newPos, newState, simulateProperty.updateType);
            } else {
                level.setBlockAndUpdate(newPos, newState);
                boolean updateNeighbors = simulateProperty.updateNeighbors;
                if (updateNeighbors) {
                    #if MC_VER >= MC_1_21_3
                    level.neighborChanged(newState, newPos, newState.getBlock(), null, false);
                    #else
                    level.neighborChanged(state, pos, thisBlock, pos, false);
                    #endif
                    level.scheduleTick(newPos, newState.getBlock(), 1);
                }
            }
        }

        if (newBlockStates.isEmpty()) {
            return Triple.of(state, result, pos);
        }

        Pair<BlockPos, BlockState> mainState = newBlockStates.get(newBlockStates.size() - 1);

        return Triple.of(mainState.getSecond(), result, mainState.getFirst());
    };

    default boolean implementsSimulatePrecTicks() {
        return false;
    }
    default boolean canSimulatePrecTicks(BlockState state, ServerLevel level, BlockPos pos, long timeInWeather, Biome.Precipitation precipitation) {
        return this.implementsSimulatePrecTicks();
    }
    default void simulatePrecTicks(BlockState state, ServerLevel level, BlockPos pos, long timeInWeather, long timePassed, Biome.Precipitation precipitation, float precipitationPickChance) {}
}
