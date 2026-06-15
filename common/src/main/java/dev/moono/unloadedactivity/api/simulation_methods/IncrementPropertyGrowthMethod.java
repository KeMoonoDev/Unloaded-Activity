package dev.moono.unloadedactivity.api.simulation_methods;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.value_expression_containers.FixedValueExpression;
import dev.moono.unloadedactivity.api.value_expression_containers.RandomizedValueExpression;
import dev.moono.unloadedactivity.datapack.ExpressionContext;
import dev.moono.unloadedactivity.datapack.ValueExpression;
import dev.moono.unloadedactivity.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class IncrementPropertyGrowthMethod extends SeparableSimulationMethod {
    public final boolean updateNeighbors;
    public final boolean reverseHeightGrowthDirection;
    public final boolean onlyInWater;
    public final Map<String, RandomizedValueExpression<Number>> setProperties;
    public final List<String> transferProperties;

    @Nullable public final List<Block> lowerBlocks;

    @Nullable public final Integer maxHeight;
    @Nullable public final RandomizedValueExpression<Block> bottomBlockReplacement;
    @Nullable public final FixedValueExpression<Number> maxValue;

    public IncrementPropertyGrowthMethod(SimulationConfig config) {
        super(config);
        this.updateNeighbors = config.getBooleanOrDefault("update_neighbors", false);
        this.reverseHeightGrowthDirection = config.getBooleanOrDefault("reverse_height_growth_direction", false);
        this.onlyInWater = config.getBooleanOrDefault("only_in_water", false);
        this.setProperties = config.getRandomizedNumberExpressionMap("set_properties");
        this.transferProperties = config.getStringList("transfer_properties");

        Number numberMaxHeight = config.getNumberNullable("max_height");
        this.maxHeight = numberMaxHeight == null ? null : numberMaxHeight.intValue();
        this.bottomBlockReplacement = config.getRandomizedBlockExpressionNullable("bottom_block_replacement");
        this.maxValue = config.getFixedNumberExpressionNullable("max_value");

        if (config.isDefined("lower_blocks")) {
            this.lowerBlocks = config.getBlockList("lower_blocks");
        } else if (this.bottomBlockReplacement != null) {
            this.lowerBlocks = this.bottomBlockReplacement.inner.getPossibleValues().toList();
        } else {
            this.lowerBlocks = null;
        }

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
        if (blockingAbove) {
            return true;
        }

        if (maxHeight != null) {
            List<Block> currentLowerBlocks = Objects.requireNonNullElseGet(this.lowerBlocks, () -> List.of(thisBlock));

            int height;
            if (reverseHeightGrowthDirection) {
                for(height = 1; height <= maxHeight; ++height) {
                    boolean doContinue = false;
                    for (Block lowerBlock : currentLowerBlocks) {
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
                    for (Block lowerBlock : currentLowerBlocks) {
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
                Number calculated = maxValue.evaluateFixed(level, state, pos);
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

        int max;

        if (this.maxValue != null) {
            Number calculated = this.maxValue.evaluateFixed(level, state, pos);
            max = Math.min(propertyMax, calculated.intValue());
        } else {
            max = propertyMax;
        }

        int updateCount = Math.max(0, max - current);

        int freeSpaceLimit = Integer.MAX_VALUE;

        if (this.maxHeight != null) {
            List<Block> currentLowerBlocks = Objects.requireNonNullElseGet(this.lowerBlocks, () -> List.of(thisBlock));

            int height;
            if (reverseHeightGrowthDirection) {
                for(height = 1; height <= maxHeight; ++height) {
                    boolean doContinue = false;
                    for (Block lowerBlock : currentLowerBlocks) {
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
                    for (Block lowerBlock : currentLowerBlocks) {
                        if (level.getBlockState(pos.below(height)).is(lowerBlock)) {
                            doContinue = true;
                            break;
                        };
                    }
                    if (doContinue) continue;
                    break;
                }
            }

            freeSpaceLimit = Math.min(freeSpaceLimit, this.maxHeight - height);
        }

        freeSpaceLimit = Math.min(freeSpaceLimit, updateCount);

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

        return freeSpace;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, int occurrences, long simulationDuration, long timePassed, @Nullable ActiveGroupSimulateData groupSimulateData) {
        Optional<Property<?>> maybeProperty = GameUtils.getProperty(state, this.target);

        DeferredBlockPlacer blockPlacer = DeferredBlockPlacer.empty();

        if (maybeProperty.isEmpty())
            return blockPlacer;

        Property<?> property = maybeProperty.get();

        Block thisBlock = state.getBlock();

        int current;

        if (property instanceof IntegerProperty integerProperty) {
            current = state.getValue(integerProperty);

        } else if (property instanceof BooleanProperty booleanProperty) {
            current = state.getValue(booleanProperty) ? 1 : 0;

        } else {
            return blockPlacer;
        }

        long currentTime = GameUtils.getTime(level);

        long finishTime = currentTime - timePassed + simulationDuration;

        if (this.bottomBlockReplacement != null) {
            Block newBlock = this.bottomBlockReplacement.evaluateRandomized(level, state, pos, finishTime);
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
            blockPlacer.setBlock(pos, state, simulationDuration);
        }


        for (int i=0;i<occurrences;i++) {
            if (this.reverseHeightGrowthDirection) {
                pos = pos.below();
            } else {
                pos = pos.above();
            }

            boolean isFinal = i+1 == occurrences;

            if (this.bottomBlockReplacement != null && !isFinal) {
                Block newBlock = this.bottomBlockReplacement.evaluateRandomized(level, state, pos, finishTime);
                state = newBlock.defaultBlockState();
            } else {
                int newValue = current + (i + 1);
                if (property instanceof IntegerProperty integerProperty) {
                    state = thisBlock.defaultBlockState().setValue(integerProperty, newValue);
                } else if (property instanceof BooleanProperty booleanProperty) {
                    state = thisBlock.defaultBlockState().setValue(booleanProperty, newValue > 0);
                }
            }

            for (var entry : this.setProperties.entrySet()) {
                String propertyName = entry.getKey();
                RandomizedValueExpression<Number> propertyValue = entry.getValue();
                Optional<Property<?>> maybeSetProperty = GameUtils.getProperty(state, propertyName);
                if (maybeSetProperty.isPresent()) {
                    Property<?> newSetProperty = maybeSetProperty.get();
                    if (newSetProperty instanceof BooleanProperty booleanProperty) {
                        float value = propertyValue.evaluateRandomized(level, state, pos, finishTime).floatValue();
                        state = state.setValue(booleanProperty, value != 0);
                    }
                    if (newSetProperty instanceof IntegerProperty integerProperty) {
                        int value = propertyValue.evaluateRandomized(level, state, pos, finishTime).intValue();
                        state = state.setValue(integerProperty, value);
                    }
                }
            }

            blockPlacer.setBlock(pos, state, simulationDuration);
        }

        return blockPlacer;
    }
}
