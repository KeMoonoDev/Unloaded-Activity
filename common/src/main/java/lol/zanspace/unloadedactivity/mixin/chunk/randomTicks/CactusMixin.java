package lol.zanspace.unloadedactivity.mixin.chunk.randomTicks;

import lol.zanspace.unloadedactivity.ActiveGroupSimulateData;
import lol.zanspace.unloadedactivity.MathUtils;
import lol.zanspace.unloadedactivity.GameUtils;
import lol.zanspace.unloadedactivity.OccurrencesAndDuration;
import lol.zanspace.unloadedactivity.datapack.*;
import lol.zanspace.unloadedactivity.interfaces.SimulateChunkBlocks;
import lol.zanspace.unloadedactivity.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CactusBlock.class)
public abstract class CactusMixin extends Block {
    public CactusMixin(Properties properties) {
        super(properties);
    }


    #if MC_VER >= MC_1_21_5
    @Shadow @Final
    private static double ATTEMPT_GROW_CACTUS_FLOWER_SMALL_CACTUS_CHANCE;

    @Shadow @Final
    private static double ATTEMPT_GROW_CACTUS_FLOWER_TALL_CACTUS_CHANCE;

    @Shadow @Final
    private static int ATTEMPT_GROW_CACTUS_FLOWER_AGE;
    #endif

    @Override
    public boolean isPropertyFinished(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty) {
        if (!simulateProperty.isAction("grow_cactus")) {
            return super.isPropertyFinished(state, level, pos, simulateProperty);
        }
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
            boolean stopUpdatingAfterBlockage = true; // TODO add this to the main simulator
            if (blockingAbove && (stopUpdatingAfterBlockage || continuesAbove)) {
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

        int propertyMax = ((IntegerPropertyAccessor)CactusBlock.AGE).unloaded_activity$getMax();
        int max = propertyMax;
        int current = state.getValue(CactusBlock.AGE);


        if (simulateProperty.maxValue.isPresent()) {
            CalculateValue<Number> maxValue = simulateProperty.maxValue.get();
            Number calculated = maxValue.calculateValue(new CalculationData(level, state, pos));
            max = Math.min(propertyMax, calculated.intValue());
        }

        return current >= max;
    }

    @Override
    public @Nullable Triple<BlockState, OccurrencesAndDuration, BlockPos> simulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty, RandomSource random, long timePassed, float randomPickOdds, boolean calculateDuration, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (!simulateProperty.isAction("grow_cactus")) {
            return super.simulateProperty(state, level, pos, simulateProperty, random, timePassed, randomPickOdds, calculateDuration, groupSimulateData);
        }

        long currentTime = GameUtils.getTime(level);

        int propertyMax = ((IntegerPropertyAccessor)CactusBlock.AGE).unloaded_activity$getMax();
        int max = propertyMax;
        int current = state.getValue(CactusBlock.AGE);
        int updateCount = max - current;


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
                lowerBlock = this;
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

        OccurrencesAndDuration result = MathUtils.getOccurrences(level, state, pos, currentTime, timePassed, simulateProperty, updateCount, randomPickOdds, calculateDuration, random, groupSimulateData);

        if (result.occurrences() == 0)
            return Triple.of(state, result, pos);

        int newPropertyValue = current + result.occurrences();

        #if MC_VER >= MC_1_21_5
        boolean growFlowerAtEnd = false;
        { // Cactus flower logic
            int flowerOpportunities = ((current > ATTEMPT_GROW_CACTUS_FLOWER_AGE ? current - (propertyMax + 1) : current) + ATTEMPT_GROW_CACTUS_FLOWER_AGE-1 + result.occurrences()) / (propertyMax + 1);

            if (flowerOpportunities > 0) {
                int growNewBlocks = newPropertyValue/(max + 1);

                Block lowerBlock;
                if (simulateProperty.blockReplacement.isPresent()) {
                    lowerBlock = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                } else {
                    lowerBlock = this;
                }

                int height;
                int maxHeight = simulateProperty.maxHeight.orElse(1);
                if (simulateProperty.reverseHeightGrowthDirection) {
                    for(height = 1; level.getBlockState(pos.above(height)).is(lowerBlock) && height <= maxHeight; ++height) {}
                } else {
                    for(height = 1; level.getBlockState(pos.below(height)).is(lowerBlock) && height <= maxHeight; ++height) {}
                }
                height--;

                int newHeight = height + growNewBlocks;

                boolean finalFlowerIsMax = newHeight >= maxHeight;

                for (int i = 0; i < flowerOpportunities; i++) {

                    BlockPos checkPos = simulateProperty.reverseHeightGrowthDirection ? pos.below(i + 1) : pos.above(i + 1);
                    for(Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockState neighbor = level.getBlockState(checkPos.relative(direction));
                        if (neighbor.isSolid() || level.getFluidState(checkPos.relative(direction)).is(FluidTags.LAVA)) {
                            break;
                        }
                    }

                    double chanceToGrowFlower;
                    if (i+1 == flowerOpportunities && finalFlowerIsMax) {
                        chanceToGrowFlower = ATTEMPT_GROW_CACTUS_FLOWER_TALL_CACTUS_CHANCE;
                    } else {
                        chanceToGrowFlower = ATTEMPT_GROW_CACTUS_FLOWER_SMALL_CACTUS_CHANCE;
                    }

                    if (random.nextDouble() <= chanceToGrowFlower) {
                        growFlowerAtEnd = true;
                        if (current > ATTEMPT_GROW_CACTUS_FLOWER_AGE) {
                            newPropertyValue = 8 + 16 + i * (propertyMax + 1);
                        } else {
                            newPropertyValue = 8 + i * (propertyMax + 1);
                        }
                        break;
                    }
                }
            }
        }
        #endif


        if (simulateProperty.increasePerHeight) {
            if (simulateProperty.blockReplacement.isPresent()) {
                Block newBlock = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                BlockState newState = newBlock.defaultBlockState();

                for (String propertyName : simulateProperty.transferProperties) {
                    Optional<Property<?>> maybeNewProperty = SimulateChunkBlocks.getProperty(newState, propertyName);

                    if (maybeNewProperty.isEmpty()) {
                        continue;
                    }

                    Optional<Property<?>> maybeOldProperty = SimulateChunkBlocks.getProperty(state, propertyName);

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
                    state = this.defaultBlockState().setValue(CactusBlock.AGE, newValue);
                }

                for (var setProperty : simulateProperty.setProperties) {
                    String propertyName = setProperty.getFirst();
                    CalculateValue<Number> propertyValue = setProperty.getSecond();
                    Optional<Property<?>> maybeProperty = SimulateChunkBlocks.getProperty(state, propertyName);
                    if (maybeProperty.isPresent()) {
                        Property<?> property = maybeProperty.get();
                        if (property instanceof BooleanProperty booleanProperty) {
                            float value = propertyValue.calculateValue(new CalculationData(level, state, pos)).floatValue();
                            state = state.setValue(booleanProperty, value != 0);
                        }
                        if (property instanceof IntegerProperty integerProperty) {
                            int value = propertyValue.calculateValue(new CalculationData(level, state, pos)).intValue();
                            state = state.setValue(integerProperty, value);
                        }
                    }
                }

                level.setBlockAndUpdate(pos, state);
                boolean updateNeighbors = simulateProperty.updateNeighbors;
                if (updateNeighbors) {
                    #if MC_VER >= MC_1_21_3
                    level.neighborChanged(state, pos, this, null, false);
                    #else
                    level.neighborChanged(state, pos, this, pos, false);
                    #endif
                    level.scheduleTick(pos, this, 1);
                }
            }
        } else if (simulateProperty.maxHeight.isPresent()) {
            int growBlocks = newPropertyValue/(max + 1);
            int valueRemainer = newPropertyValue % (max + 1);

            boolean resetOnHeightChange = simulateProperty.resetOnHeightChange;

            int belowValue = resetOnHeightChange ? 0 : max;

            if (growBlocks == 0) {
                state = state.setValue(CactusBlock.AGE, valueRemainer);
            } else if (simulateProperty.blockReplacement.isPresent()) {
                Block newBlock = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                BlockState newState = newBlock.defaultBlockState();

                for (String propertyName : simulateProperty.transferProperties) {
                    Optional<Property<?>> maybeNewProperty = SimulateChunkBlocks.getProperty(newState, propertyName);

                    if (maybeNewProperty.isEmpty()) {
                        continue;
                    }

                    Optional<Property<?>> maybeOldProperty = SimulateChunkBlocks.getProperty(state, propertyName);

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
                state = state.setValue(CactusBlock.AGE, belowValue);
            }

            level.setBlock(pos, state, simulateProperty.updateType);

            for (int i=0;i<growBlocks;i++) {
                if (simulateProperty.reverseHeightGrowthDirection) {
                    pos = pos.below();
                } else {
                    pos = pos.above();
                }

                if (i+1==growBlocks) {
                    state = this.defaultBlockState().setValue(CactusBlock.AGE, valueRemainer);
                } else if (simulateProperty.blockReplacement.isPresent()) {
                    Block newBlock = simulateProperty.blockReplacement.get().calculateValue(new CalculationData(level, state, pos));
                    BlockState newState = newBlock.defaultBlockState();

                    for (String propertyName : simulateProperty.transferProperties) {
                        Optional<Property<?>> maybeNewProperty = SimulateChunkBlocks.getProperty(newState, propertyName);

                        if (maybeNewProperty.isEmpty()) {
                            continue;
                        }

                        Optional<Property<?>> maybeOldProperty = SimulateChunkBlocks.getProperty(state, propertyName);

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
                    state = this.defaultBlockState().setValue(CactusBlock.AGE, belowValue);
                }

                for (var setProperty : simulateProperty.setProperties) {
                    String propertyName = setProperty.getFirst();
                    CalculateValue<Number> propertyValue = setProperty.getSecond();
                    Optional<Property<?>> maybeProperty = SimulateChunkBlocks.getProperty(state, propertyName);
                    if (maybeProperty.isPresent()) {
                        Property<?> property = maybeProperty.get();
                        if (property instanceof BooleanProperty booleanProperty) {
                            float value = propertyValue.calculateValue(new CalculationData(level, state, pos)).floatValue();
                            state = state.setValue(booleanProperty, value != 0);
                        }
                        if (property instanceof IntegerProperty integerProperty) {
                            int value = propertyValue.calculateValue(new CalculationData(level, state, pos)).intValue();
                            state = state.setValue(integerProperty, value);
                        }
                    }
                }

                level.setBlockAndUpdate(pos, state);
                boolean updateNeighbors = simulateProperty.updateNeighbors;
                if (updateNeighbors) {
                    #if MC_VER >= MC_1_21_3
                    level.neighborChanged(state, pos, this, null, false);
                    #else
                    level.neighborChanged(state, pos, this, pos, false);
                    #endif
                    level.scheduleTick(pos, this, 1);
                }
            }
        } else {
            state = state.setValue(CactusBlock.AGE, newPropertyValue);
            level.setBlock(pos, state, simulateProperty.updateType);
        }

        #if MC_VER >= MC_1_21_5
        if (growFlowerAtEnd) {
            BlockPos flowerPos = simulateProperty.reverseHeightGrowthDirection ? pos.below() : pos.above();
            level.setBlockAndUpdate(flowerPos, Blocks.CACTUS_FLOWER.defaultBlockState());
        }
        #endif

        return null;
    }
}