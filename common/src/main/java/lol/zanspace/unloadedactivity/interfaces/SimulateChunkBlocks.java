package lol.zanspace.unloadedactivity.interfaces;

import lol.zanspace.unloadedactivity.ActiveGroupSimulateData;
import lol.zanspace.unloadedactivity.OccurrencesAndDuration;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.Utils;
import lol.zanspace.unloadedactivity.datapack.*;
import lol.zanspace.unloadedactivity.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
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

    default Optional<SimulateProperty> getGroupSimulationProperty(ResourceLocation groupId) {
        return getSimulationData().propertyMap.values()
            .stream()
            .filter(property -> property.simulateWithGroup.equals(Optional.of(groupId)))
            .findFirst();
    };

    default boolean canSimulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty) {
        boolean isFinished = isPropertyFinished(state, level, pos, simulateProperty);
        if (isFinished)
            return false;

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
                        DirectionProperty property = (DirectionProperty)getProperty(dirBlockState, simulateProperty.buddingDirectionProperty.get()).get();

                        Direction blockDirection = dirBlockState.getValue(property);
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

    default @Nullable Triple<BlockState, OccurrencesAndDuration, BlockPos> simulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty, RandomSource random, long timePassed, float randomPickOdds, boolean calculateDuration, @Nullable ActiveGroupSimulateData groupSimulateData) {
        switch (simulateProperty.simulationType) {
            case PROPERTY -> {
                Optional<Property<?>> maybeProperty = getProperty(state, simulateProperty.target);

                if (maybeProperty.isEmpty())
                    return Triple.of(state, OccurrencesAndDuration.empty(), pos);

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
                    return Triple.of(state, OccurrencesAndDuration.empty(), pos);
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

                if (updateCount <= 0)
                    return Triple.of(state, OccurrencesAndDuration.empty(), pos);

                OccurrencesAndDuration result = Utils.getOccurrences(level, state, pos, level.getDayTime(), timePassed, simulateProperty, updateCount, randomPickOdds, calculateDuration, random, groupSimulateData);

                if (result.occurrences() == 0)
                    return Triple.of(state, result, pos);

                int newPropertyValue = current + result.occurrences();

                if (simulateProperty.increasePerHeight) {
                    if (simulateProperty.blockReplacement.isPresent()) {
                        Block newBlock = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));

                        BlockState newState = newBlock.defaultBlockState();
                        for (RandomProperty randomProperty : simulateProperty.randomProperties) {
                            Optional<Property<?>> maybeNewRandomProperty = getProperty(newState, randomProperty.propertyName);
                            if (maybeNewRandomProperty.isPresent()) {
                                switch (randomProperty.propertyType) {
                                    case BOOL -> {
                                        if (maybeNewRandomProperty.get() instanceof BooleanProperty newBooleanProperty) {
                                            Optional<Property<?>> maybeOldRandomProperty = getProperty(state, randomProperty.propertyName);
                                            if (maybeOldRandomProperty.isPresent() && maybeOldRandomProperty.get() instanceof BooleanProperty oldBooleanProperty) {
                                                boolean oldValue = state.getValue(oldBooleanProperty);
                                                newState = newState.setValue(newBooleanProperty, oldValue);
                                            } else {
                                                int value = randomProperty.getRandomValue(random);
                                                newState = newState.setValue(newBooleanProperty, value != 0);
                                            }
                                        }
                                    }
                                    case INT -> {
                                        if (maybeNewRandomProperty.get() instanceof IntegerProperty newIntegerProperty) {
                                            Optional<Property<?>> maybeOldRandomProperty = getProperty(state, randomProperty.propertyName);
                                            if (maybeOldRandomProperty.isPresent() && maybeOldRandomProperty.get() instanceof IntegerProperty oldIntegerProperty) {
                                                int oldValue = state.getValue(oldIntegerProperty);
                                                newState = newState.setValue(newIntegerProperty, oldValue);
                                            } else {
                                                int value = randomProperty.getRandomValue(random);
                                                newState = newState.setValue(newIntegerProperty, value);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        state = newState;
                        level.setBlock(pos, state, simulateProperty.updateType);
                    }


                    for (int i=0;i<result.occurrences();i++) {
                        if (simulateProperty.reverseHeightGrowthDirection) {
                            pos = pos.below();
                        } else {
                            pos = pos.above();
                        }

                        boolean isFinal = i+1 == result.occurrences();

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

                        for (RandomProperty randomProperty : simulateProperty.randomProperties) {
                            Optional<Property<?>> maybeNewRandomProperty = getProperty(state, randomProperty.propertyName);
                            if (maybeNewRandomProperty.isPresent()) {
                                switch (randomProperty.propertyType) {
                                    case BOOL -> {
                                        if (maybeNewRandomProperty.get() instanceof BooleanProperty newBooleanProperty) {
                                            int value = randomProperty.getRandomValue(random);
                                            state = state.setValue(newBooleanProperty, value != 0);
                                        }
                                    }
                                    case INT -> {
                                        if (maybeNewRandomProperty.get() instanceof IntegerProperty newIntegerProperty) {
                                            int value = randomProperty.getRandomValue(random);
                                            state = state.setValue(newIntegerProperty, value);
                                        }
                                    }
                                }
                            }
                        }

                        level.setBlockAndUpdate(pos, state);
                        boolean updateNeighbors = simulateProperty.updateNeighbors;
                        if (updateNeighbors) {
                            level.neighborChanged(state, pos, thisBlock, pos, false);
                            level.scheduleTick(pos, thisBlock, 1);
                        }
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
                        for (RandomProperty randomProperty : simulateProperty.randomProperties) {
                            Optional<Property<?>> maybeNewRandomProperty = getProperty(newState, randomProperty.propertyName);
                            if (maybeNewRandomProperty.isPresent()) {
                                switch (randomProperty.propertyType) {
                                    case BOOL -> {
                                        if (maybeNewRandomProperty.get() instanceof BooleanProperty newBooleanProperty) {
                                            Optional<Property<?>> maybeOldRandomProperty = getProperty(state, randomProperty.propertyName);
                                            if (maybeOldRandomProperty.isPresent() && maybeOldRandomProperty.get() instanceof BooleanProperty oldBooleanProperty) {
                                                boolean oldValue = state.getValue(oldBooleanProperty);
                                                newState = newState.setValue(newBooleanProperty, oldValue);
                                            } else {
                                                int value = randomProperty.getRandomValue(random);
                                                newState = newState.setValue(newBooleanProperty, value != 0);
                                            }
                                        }
                                    }
                                    case INT -> {
                                        if (maybeNewRandomProperty.get() instanceof IntegerProperty newIntegerProperty) {
                                            Optional<Property<?>> maybeOldRandomProperty = getProperty(state, randomProperty.propertyName);
                                            if (maybeOldRandomProperty.isPresent() && maybeOldRandomProperty.get() instanceof IntegerProperty oldIntegerProperty) {
                                                int oldValue = state.getValue(oldIntegerProperty);
                                                newState = newState.setValue(newIntegerProperty, oldValue);
                                            } else {
                                                int value = randomProperty.getRandomValue(random);
                                                newState = newState.setValue(newIntegerProperty, value);
                                            }
                                        }
                                    }
                                }
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

                    level.setBlock(pos, state, simulateProperty.updateType);

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
                            state = newBlock.defaultBlockState();
                        } else {
                            if (property instanceof IntegerProperty integerProperty) {
                                state = thisBlock.defaultBlockState().setValue(integerProperty, belowValue);
                            } else if (property instanceof BooleanProperty booleanProperty) {
                                state = thisBlock.defaultBlockState().setValue(booleanProperty, belowValue > 0);
                            }
                        }

                        for (RandomProperty randomProperty : simulateProperty.randomProperties) {
                            Optional<Property<?>> maybeNewRandomProperty = getProperty(state, randomProperty.propertyName);
                            if (maybeNewRandomProperty.isPresent()) {
                                switch (randomProperty.propertyType) {
                                    case BOOL -> {
                                        if (maybeNewRandomProperty.get() instanceof BooleanProperty newBooleanProperty) {
                                            int value = randomProperty.getRandomValue(random);
                                            state = state.setValue(newBooleanProperty, value != 0);
                                        }
                                    }
                                    case INT -> {
                                        if (maybeNewRandomProperty.get() instanceof IntegerProperty newIntegerProperty) {
                                            int value = randomProperty.getRandomValue(random);
                                            state = state.setValue(newIntegerProperty, value);
                                        }
                                    }
                                }
                            }
                        }

                        level.setBlockAndUpdate(pos, state);
                        boolean updateNeighbors = simulateProperty.updateNeighbors;
                        if (updateNeighbors) {
                            level.neighborChanged(state, pos, thisBlock, pos, false);
                            level.scheduleTick(pos, thisBlock, 1);
                        }
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
                    level.setBlock(pos, state, simulateProperty.updateType);
                }


                return Triple.of(state, result, pos);
            }
            case BUDDING -> {
                List<Direction> availableDirections = Arrays.stream(Direction.values()).filter(direction -> !simulateProperty.ignoreBuddingDirections.contains(direction)).toList();

                for(Direction direction : availableDirections) {
                    BlockPos budPos = pos.relative(direction);
                    BlockState budState = level.getBlockState(budPos);

                    int stage = 0;


                    boolean doContinue = false;

                    for (int i=0;i<simulateProperty.buddingBlocks.size();i++) {
                        Block buddingBlockStage = simulateProperty.buddingBlocks.get(i);

                        if (budState.is(buddingBlockStage)) {
                            DirectionProperty directionProperty = (DirectionProperty)getProperty(budState, simulateProperty.buddingDirectionProperty.get()).get();

                            if (budState.getValue(directionProperty) == direction) {
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

                    OccurrencesAndDuration result = Utils.getOccurrences(level, state, pos, level.getDayTime(), timePassed, simulateProperty, maxOccurrences, randomPickOdds, calculateDuration, random, groupSimulateData);

                    if (result.occurrences() == 0) {
                        continue;
                    }

                    int newStage = stage + result.occurrences();

                    Block newBudBlock = simulateProperty.buddingBlocks.get(newStage - 1);

                    BlockState newBudState = newBudBlock.defaultBlockState();

                    if (simulateProperty.buddingDirectionProperty.isPresent()) {
                        String buddingDirectionPropertyName = simulateProperty.buddingDirectionProperty.get();
                        DirectionProperty directionProperty = (DirectionProperty)getProperty(newBudState, buddingDirectionPropertyName).get();
                        newBudState = newBudState.setValue(directionProperty, direction);
                    }


                    if (simulateProperty.waterloggedProperty.isPresent()) {
                        BooleanProperty waterloggedProperty = (BooleanProperty)getProperty(newBudState, simulateProperty.waterloggedProperty.get()).get();
                        newBudState = newBudState.setValue(waterloggedProperty, budState.getFluidState().getType() == Fluids.WATER);
                    }

                    level.setBlock(budPos, newBudState, simulateProperty.updateType);
                }
            }
            case DECAY -> {
                long dayTime = level.getDayTime();

                boolean calculateDecayDuration = calculateDuration || simulateProperty.hatchEntity.isPresent();
                if (simulateProperty.blockReplacement.isPresent()) {
                    Block blockReplacement = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                    SimulationData simulationData = blockReplacement.getSimulationData();
                    if (simulationData.hasRandTicksWithoutGroup || simulationData.hasPrecTicksWithoutGroup) {
                        calculateDecayDuration = true;
                    }
                }

                OccurrencesAndDuration result = Utils.getOccurrences(level, state, pos, dayTime, timePassed, simulateProperty, 1, randomPickOdds, calculateDecayDuration, random, groupSimulateData);

                if (result.occurrences() == 0)
                    return Triple.of(state, result, pos);

                if (simulateProperty.dropsResources) {
                    Block.dropResources(state, level, pos);
                }

                BlockState oldState = state;

                if (simulateProperty.blockReplacement.isPresent()) {
                    Block blockReplacement = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                    state = blockReplacement.defaultBlockState();
                    level.setBlock(pos, state, simulateProperty.updateType);
                } else {
                    level.removeBlock(pos, false);
                    state = level.getBlockState(pos);
                }

                if (simulateProperty.hatchEntity.isPresent()) {
                    int hatchCount;
                    if (simulateProperty.hatchCount.isPresent()) {
                        long hatchTime = dayTime - timePassed + result.duration();
                        hatchCount = (int)simulateProperty.hatchCount.get().calculateValue(new CalculationData(level, oldState, pos, hatchTime));
                    } else {
                        hatchCount = 1;
                    }

                    if (hatchCount > 0) {
                        for(int i = 0; i < hatchCount; i++) {
                            Entity hatchedEntity = simulateProperty.hatchEntity.get().create(level);
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
                            hatchedEntity.simulateTime(timePassed - result.duration());
                        }

                    }
                }

                return Triple.of(state, result, pos);
            }
        }


        return Triple.of(state, OccurrencesAndDuration.empty(), pos);
    };

    default boolean implementsSimulatePrecTicks() {
        return false;
    }
    default boolean canSimulatePrecTicks(BlockState state, ServerLevel level, BlockPos pos, long timeInWeather, Biome.Precipitation precipitation) {
        return this.implementsSimulatePrecTicks();
    }
    default void simulatePrecTicks(BlockState state, ServerLevel level, BlockPos pos, long timeInWeather, long timePassed, Biome.Precipitation precipitation, float precipitationPickChance) {}


}
