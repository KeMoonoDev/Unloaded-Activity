package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SeparableSimulationMethod;
import dev.moono.unloadedactivity.api.value_expression.FixedValueExpression;
import dev.moono.unloadedactivity.api.value_expression.RandomizedValueExpression;
import dev.moono.unloadedactivity.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class IncrementPropertyGrowthMethod extends SeparableSimulationMethod {
    public final Property<?> property;
    public final boolean updateNeighbors;
    public final boolean reverseHeightGrowthDirection;
    public final boolean onlyInWater;
    public final List<Pair<Property<?>, RandomizedValueExpression<Number>>> setProperties;
    public final List<String> transferProperties;

    @Nullable public final List<Block> lowerBlocks;

    @Nullable public final Integer maxHeight;
    @Nullable public final RandomizedValueExpression<Block> bottomBlockReplacement;
    @Nullable public final FixedValueExpression<Number> maxValue;

    public IncrementPropertyGrowthMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, hasDependants);
        String propertyName = config.getString("property_name");

        Map<String, RandomizedValueExpression<Number>> setPropertiesNames = config.getRandomizedNumberExpressionMap("set_properties");

        this.updateNeighbors = config.getBooleanOrDefault("update_neighbors", false);
        this.reverseHeightGrowthDirection = config.getBooleanOrDefault("reverse_height_growth_direction", false);
        this.onlyInWater = config.getBooleanOrDefault("only_in_water", false);
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

        Optional<Property<?>> maybeProperty = GameUtils.getProperty(block.defaultBlockState(), propertyName);;

        if (maybeProperty.isEmpty())
            throw new RuntimeException("Block " + block + " does not have a property named " + propertyName);

        this.property = maybeProperty.get();

        if (!(property instanceof IntegerProperty) && !(property instanceof BooleanProperty))
            throw new RuntimeException("Block " + block + " has the property named " + propertyName + ", but the property isn't an IntegerProperty or BooleanProperty.");

        ArrayList<Pair<Property<?>, RandomizedValueExpression<Number>>> convertedSetProperties = new ArrayList<>();
        for (var entry : setPropertiesNames.entrySet()) {
            String setPropertyName = entry.getKey();
            Optional<Property<?>> maybeSetProperty = GameUtils.getProperty(block.defaultBlockState(), setPropertyName);;

            if (maybeSetProperty.isEmpty())
                throw new RuntimeException("Block " + block + " does not have a property named " + setPropertyName);

            Property<?> setProperty = maybeSetProperty.get();

            if (!(setProperty instanceof IntegerProperty) && !(setProperty instanceof BooleanProperty))
                throw new RuntimeException("Block " + block + " has the property named " + setPropertyName + ", but the property isn't an IntegerProperty or BooleanProperty.");

            convertedSetProperties.add(Pair.of(setProperty, entry.getValue()));
        }

        // Make it immutable
        this.setProperties = List.copyOf(convertedSetProperties);
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
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
            throw new RuntimeException("Property should have been validated at this point.");
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
                        }
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
                        }
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
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings) {
        DeferredBlockPlacer blockPlacer = DeferredBlockPlacer.empty();
        Block thisBlock = state.getBlock();

        int current;

        if (property instanceof IntegerProperty integerProperty) {
            current = state.getValue(integerProperty);
        } else if (property instanceof BooleanProperty booleanProperty) {
            current = state.getValue(booleanProperty) ? 1 : 0;
        } else {
            throw new RuntimeException("Property should have been validated at this point.");
        }

        SimulatedTime finalTime = occurrencesAndTimings.getFinalTime();
        long endTime = finalTime.endTime();

        if (this.bottomBlockReplacement != null) {
            Block newBlock = this.bottomBlockReplacement.evaluateRandomized(level, state, pos, endTime);
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
            blockPlacer.setBlock(pos, state, finalTime);
        }

        int occurrences = occurrencesAndTimings.occurrences();

        for (int i=0;i<occurrences;i++) {
            if (this.reverseHeightGrowthDirection) {
                pos = pos.below();
            } else {
                pos = pos.above();
            }

            boolean isFinal = i+1 == occurrences;

            if (this.bottomBlockReplacement != null && !isFinal) {
                Block newBlock = this.bottomBlockReplacement.evaluateRandomized(level, state, pos, endTime);
                state = newBlock.defaultBlockState();
            } else {
                int newValue = current + (i + 1);
                if (property instanceof IntegerProperty integerProperty) {
                    state = thisBlock.defaultBlockState().setValue(integerProperty, newValue);
                } else if (property instanceof BooleanProperty booleanProperty) {
                    state = thisBlock.defaultBlockState().setValue(booleanProperty, newValue > 0);
                } else {
                    throw new RuntimeException("Property should have been validated at this point.");
                }
            }

            for (var pair : this.setProperties) {
                Property<?> setProperty = pair.getLeft();
                RandomizedValueExpression<Number> propertyValue = pair.getRight();
                if (setProperty instanceof BooleanProperty booleanProperty) {
                    float value = propertyValue.evaluateRandomized(level, state, pos, endTime).floatValue();
                    state = state.setValue(booleanProperty, value != 0);
                } else if (setProperty instanceof IntegerProperty integerProperty) {
                    int value = propertyValue.evaluateRandomized(level, state, pos, endTime).intValue();
                    state = state.setValue(integerProperty, value);
                } else {
                    throw new RuntimeException("Property should have been validated at this point.");
                }
            }

            blockPlacer.setBlock(pos, state, finalTime);
        }

        return blockPlacer;
    }
}
