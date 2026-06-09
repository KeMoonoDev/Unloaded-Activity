package lol.zanspace.unloadedactivity.api.simulation_methods;

import lol.zanspace.unloadedactivity.ActiveGroupSimulateData;
import lol.zanspace.unloadedactivity.DeferredBlockPlacer;
import lol.zanspace.unloadedactivity.GameUtils;
import lol.zanspace.unloadedactivity.api.SimulationConfig;
import lol.zanspace.unloadedactivity.datapack.ValueContext;
import lol.zanspace.unloadedactivity.datapack.ValueExpression;
import lol.zanspace.unloadedactivity.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MaxPropertyGrowthMethod extends SeparableSimulationMethod {
    public int updateType;
    public boolean updateNeighbors;
    public boolean reverseHeightGrowthDirection;
    public boolean stopUpdatingAfterMaxHeight;
    public boolean stopUpdatingAfterBlockage;
    public boolean resetOnHeightChange;
    public boolean onlyInWater;
    public Map<String, ValueExpression<Number>> setProperties;
    public List<String> transferProperties;
    public Integer maxHeight;

    @Nullable public ValueExpression<Block> bottomBlockReplacement;
    @Nullable public ValueExpression<Number> maxValue;

    @Nullable public AgeBloomConfig ageBloom;

    public record AgeBloomConfig(int bloomAtAge, Block bloomBlock, List<Number> heightProbabilities) {
        public AgeBloomConfig(SimulationConfig config) {
            int bloomAtAge = config.getNumber("bloom_at_age").intValue();
            Block bloomBlock = config.getBlock("bloom_block");
            List<Number> heightProbabilities = config.getNumberList("height_probabilities");
            this(bloomAtAge, bloomBlock, heightProbabilities);
        }
    }

    public MaxPropertyGrowthMethod(SimulationConfig config) {
        super(config);
        this.updateType = config.getNumberOrDefault("update_type", Block.UPDATE_ALL).intValue();
        this.updateNeighbors = config.getBooleanOrDefault("update_neighbors", false);
        this.reverseHeightGrowthDirection = config.getBooleanOrDefault("reverse_height_growth_direction", false);
        this.stopUpdatingAfterMaxHeight = config.getBooleanOrDefault("stop_updating_after_max_height", false);
        this.stopUpdatingAfterBlockage = config.getBooleanOrDefault("stop_updating_after_blockage", false);
        this.resetOnHeightChange = config.getBooleanOrDefault("reset_on_height_change", true);
        this.onlyInWater = config.getBooleanOrDefault("only_in_water", false);
        this.setProperties = config.getRandomizedNumberExpressionMap("set_properties");
        this.transferProperties = config.getStringList("transfer_properties");
        this.maxHeight = config.getNumber("max_height").intValue();

        this.bottomBlockReplacement = config.getRandomizedBlockExpressionNullable("bottom_block_replacement");
        this.maxValue = config.getFixedNumberExpressionNullable("max_value");

        SimulationConfig bloomConfig = config.getConfigNullable("age_bloom");
        this.ageBloom = bloomConfig != null ? new AgeBloomConfig(bloomConfig) : null;
    }

    @Override
    public boolean isFinished(BlockState state, ServerLevel level, BlockPos pos) {
        Block thisBlock = state.getBlock();

        BlockState blockStateAbove;

        if (reverseHeightGrowthDirection) {
            blockStateAbove = level.getBlockState(pos.below());
        } else {
            blockStateAbove = level.getBlockState(pos.above());
        }

        boolean emptyAbove = blockStateAbove.isAir();
        boolean blockingAbove = !emptyAbove;
        boolean continuesAbove = blockStateAbove.is(thisBlock);
        if (blockingAbove && (stopUpdatingAfterBlockage || continuesAbove)) {
            return true;
        }

        List<Block> lowerBlocks;
        if (bottomBlockReplacement != null) {
            // TODO create a "getPossibleValues" method.
            lowerBlocks = List.of(bottomBlockReplacement.evaluate(new ValueContext(level, state, pos)));
        } else {
            lowerBlocks = List.of(thisBlock);
        }

        int height;
        if (reverseHeightGrowthDirection) {
            for(height = 1; height <= maxHeight; ++height) {
                boolean doContinue = false;
                for (Block lowerBlock : lowerBlocks) {
                    if (level.getBlockState(pos.above(height)).is(lowerBlock)) {
                        doContinue = true;
                        break;
                    };
                }
                if (doContinue) continue;
                break;
            }
        } else {
            for(height = 1; height <= maxHeight; ++height) {
                boolean doContinue = false;
                for (Block lowerBlock : lowerBlocks) {
                    if (level.getBlockState(pos.below(height)).is(lowerBlock)) {
                        doContinue = true;
                        break;
                    };
                }
                if (doContinue) continue;
                break;
            }
        }

        if (height < maxHeight) {
            return false;
        }

        if (stopUpdatingAfterMaxHeight) {
            return true;
        }

        Optional<Property<?>> maybeProperty = GameUtils.getProperty(state, target);

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

            if (maxValue != null) {
                Number calculated = maxValue.evaluate(new ValueContext(level, state, pos));
                max = Math.min(propertyMax, calculated.intValue());
            }

            return current >= max;
        }


        return true;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        Optional<Property<?>> maybeProperty = GameUtils.getProperty(state, target);

        if (maybeProperty.isEmpty())
            return 0;

        Property<?> property = maybeProperty.get();

        Block thisBlock = state.getBlock();
        int propertyMax;
        int current;

        if (property instanceof IntegerProperty integerProperty) {
            propertyMax = ((IntegerPropertyAccessor)integerProperty).unloaded_activity$getMax();
            current = state.getValue(integerProperty);

        } else if (property instanceof BooleanProperty booleanProperty) {
            propertyMax = 1;
            current = state.getValue(booleanProperty) ? 1 : 0;

        } else {
            return 0;
        }

        int max = propertyMax;

        if (this.maxValue != null) {
            Number calculated = this.maxValue.evaluate(new ValueContext(level, state, pos));
            max = Math.min(propertyMax, calculated.intValue());
        }

        int updateCount = Math.max(0, max - current);


        Block lowerBlock;
        if (this.bottomBlockReplacement != null) {
            lowerBlock = this.bottomBlockReplacement.evaluate(new ValueContext(level, state, pos));
        } else {
            lowerBlock = thisBlock;
        }


        int height;
        if (this.reverseHeightGrowthDirection) {
            for(height = 1; level.getBlockState(pos.above(height)).is(lowerBlock) && height <= this.maxHeight; ++height) {}
        } else {
            for(height = 1; level.getBlockState(pos.below(height)).is(lowerBlock) && height <= this.maxHeight; ++height) {}
        }

        int freeSpaceLimit = this.maxHeight - height;


        int freeSpace;
        if (this.onlyInWater) {
            if (this.reverseHeightGrowthDirection) {
                for(freeSpace = 1; level.getBlockState(pos.below(freeSpace)).is(Blocks.WATER) && freeSpace <= freeSpaceLimit; ++freeSpace) {}
            } else {
                for(freeSpace = 1; level.getBlockState(pos.above(freeSpace)).is(Blocks.WATER) && freeSpace <= freeSpaceLimit; ++freeSpace) {}
            }
        } else {
            if (this.reverseHeightGrowthDirection) {
                for(freeSpace = 1; level.isEmptyBlock(pos.below(freeSpace)) && freeSpace <= freeSpaceLimit; ++freeSpace) {}
            } else {
                for(freeSpace = 1; level.isEmptyBlock(pos.above(freeSpace)) && freeSpace <= freeSpaceLimit; ++freeSpace) {}
            }
        }
        --freeSpace;

        // Updates for growing in height
        updateCount += freeSpace;

        if (stopUpdatingAfterMaxHeight) {
            updateCount += max * Math.max(freeSpace - 1, 0);
        } else {
            updateCount += max * freeSpace;
        }

        return updateCount;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, int occurrences, long simulationDuration, long timePassed, @Nullable ActiveGroupSimulateData groupSimulateData) {
        Optional<Property<?>> maybeProperty = GameUtils.getProperty(state, this.target);

        DeferredBlockPlacer blockPlacer = DeferredBlockPlacer.empty();

        if (maybeProperty.isEmpty())
            return blockPlacer;

        Property<?> property = maybeProperty.get();

        Block thisBlock = state.getBlock();
        int propertyMax;
        int current;

        if (property instanceof IntegerProperty integerProperty) {
            propertyMax = ((IntegerPropertyAccessor)integerProperty).unloaded_activity$getMax();
            current = state.getValue(integerProperty);

        } else if (property instanceof BooleanProperty booleanProperty) {
            propertyMax = 1;
            current = state.getValue(booleanProperty) ? 1 : 0;

        } else {
            return blockPlacer;
        }

        int max;

        if (this.maxValue != null) {
            Number calculated = this.maxValue.evaluate(new ValueContext(level, state, pos));
            max = Math.min(propertyMax, calculated.intValue());
        } else {
            max = propertyMax;
        }

        int newPropertyValue = current + occurrences;

        boolean bloomAtEnd = false;
        if (this.ageBloom != null) {
            int flowerOpportunities = ((current > this.ageBloom.bloomAtAge ? current - (propertyMax + 1) : current) + this.ageBloom.bloomAtAge-1 + occurrences) / (propertyMax + 1);

            if (flowerOpportunities > 0) {
                int growNewBlocks = newPropertyValue/(max + 1);

                List<Block> lowerBlocks;
                if (bottomBlockReplacement != null) {
                    // TODO create a "getPossibleValues" method.
                    lowerBlocks = List.of(bottomBlockReplacement.evaluate(new ValueContext(level, state, pos)));
                } else {
                    lowerBlocks = List.of(thisBlock);
                }

                int remainingHeightGrowths;
                if (reverseHeightGrowthDirection) {
                    for(remainingHeightGrowths = 1; remainingHeightGrowths <= maxHeight; ++remainingHeightGrowths) {
                        boolean doContinue = false;
                        for (Block lowerBlock : lowerBlocks) {
                            if (level.getBlockState(pos.above(remainingHeightGrowths)).is(lowerBlock)) {
                                doContinue = true;
                                break;
                            };
                        }
                        if (doContinue) continue;
                        break;
                    }
                } else {
                    for(remainingHeightGrowths = 1; remainingHeightGrowths <= maxHeight; ++remainingHeightGrowths) {
                        boolean doContinue = false;
                        for (Block lowerBlock : lowerBlocks) {
                            if (level.getBlockState(pos.below(remainingHeightGrowths)).is(lowerBlock)) {
                                doContinue = true;
                                break;
                            };
                        }
                        if (doContinue) continue;
                        break;
                    }
                }
                remainingHeightGrowths--;

                for (int i = 0; i < flowerOpportunities; i++) {

                    BlockPos checkPos = reverseHeightGrowthDirection ? pos.below(i + 1) : pos.above(i + 1);

                    if (!level.isEmptyBlock(checkPos)) break;

                    boolean doContinue = false;

                    // TODO swap this out for conditions controllable by datapack.
                    for(Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockState neighbor = level.getBlockState(checkPos.relative(direction));
                        if (neighbor.isSolid() || level.getFluidState(checkPos.relative(direction)).is(FluidTags.LAVA)) {
                            doContinue = true;
                            break;
                        }
                    }

                    if (doContinue) continue;


                    double chanceToGrowFlower = this.ageBloom.heightProbabilities.get(Math.min(remainingHeightGrowths + i, this.ageBloom.heightProbabilities.size() - 1)).doubleValue();

                    RandomSource random = GameUtils.getRand(level);

                    if (random.nextDouble() <= chanceToGrowFlower) {
                        bloomAtEnd = true;
                        if (current > this.ageBloom.bloomAtAge) {
                            newPropertyValue = 8 + 16 + i * (propertyMax + 1);
                        } else {
                            newPropertyValue = 8 + i * (propertyMax + 1);
                        }
                        break;
                    }
                }
            }
        }

        int growBlocks = newPropertyValue/(max + 1);
        int valueRemainer = newPropertyValue % (max + 1);

        int belowValue = resetOnHeightChange ? 0 : max;

        boolean doUpdateType;

        if (growBlocks == 0) {
            doUpdateType = true;
            if (property instanceof IntegerProperty integerProperty) {
                state = state.setValue(integerProperty, valueRemainer);
            } else if (property instanceof BooleanProperty booleanProperty) {
                state = state.setValue(booleanProperty, valueRemainer > 0);
            }
        } else if (this.bottomBlockReplacement != null) {
            doUpdateType = false;
            Block newBlock = this.bottomBlockReplacement.evaluate(new ValueContext(level, state, pos));
            BlockState newState = newBlock.defaultBlockState();

            for (String propertyName : this.transferProperties) {
                Optional<Property<?>> maybeNewProperty = GameUtils.getProperty(newState, propertyName);

                if (maybeNewProperty.isEmpty()) {
                    continue;
                }

                Optional<Property<?>> maybeOldProperty = GameUtils.getProperty(state, propertyName);

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
            doUpdateType = true;
            if (property instanceof IntegerProperty integerProperty) {
                state = state.setValue(integerProperty, belowValue);
            } else if (property instanceof BooleanProperty booleanProperty) {
                state = state.setValue(booleanProperty, belowValue > 0);
            }
        }

        if (doUpdateType) {
            blockPlacer.setBlock(pos, state, updateNeighbors, this.updateType, simulationDuration);
        } else {
            blockPlacer.setBlock(pos, state, updateNeighbors, simulationDuration);
        }


        for (int i=0;i<growBlocks;i++) {
            if (this.reverseHeightGrowthDirection) {
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
            } else if (this.bottomBlockReplacement != null) {
                Block newBlock = this.bottomBlockReplacement.evaluate(new ValueContext(level, state, pos));
                BlockState newState = newBlock.defaultBlockState();

                for (String propertyName : this.transferProperties) {
                    Optional<Property<?>> maybeNewProperty = GameUtils.getProperty(newState, propertyName);

                    if (maybeNewProperty.isEmpty()) {
                        continue;
                    }

                    Optional<Property<?>> maybeOldProperty = GameUtils.getProperty(state, propertyName);

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

            for (var entry : this.setProperties.entrySet()) {
                String propertyName = entry.getKey();
                ValueExpression<Number> propertyValue = entry.getValue();
                Optional<Property<?>> maybeSetProperty = GameUtils.getProperty(state, propertyName);
                if (maybeSetProperty.isPresent()) {
                    Property<?> newSetProperty = maybeSetProperty.get();
                    if (newSetProperty instanceof BooleanProperty booleanProperty) {
                        float value = propertyValue.evaluate(new ValueContext(level, state, pos)).floatValue();
                        state = state.setValue(booleanProperty, value != 0);
                    }
                    if (newSetProperty instanceof IntegerProperty integerProperty) {
                        int value = propertyValue.evaluate(new ValueContext(level, state, pos)).intValue();
                        state = state.setValue(integerProperty, value);
                    }
                }
            }

            blockPlacer.setBlock(pos, state, updateNeighbors, simulationDuration);
        }

        if (bloomAtEnd) {
            BlockPos flowerPos = this.reverseHeightGrowthDirection ? pos.below() : pos.above();
            blockPlacer.setBlock(flowerPos, this.ageBloom.bloomBlock.defaultBlockState(), simulationDuration);
        }

        return blockPlacer;
    }
}
