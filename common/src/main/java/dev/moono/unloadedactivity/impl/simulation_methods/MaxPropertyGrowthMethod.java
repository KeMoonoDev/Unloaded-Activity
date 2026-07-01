package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.condition.FixedCondition;
import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.simulation_method.SeparableSimulationMethod;
import dev.moono.unloadedactivity.api.value_expression.FixedValueExpression;
import dev.moono.unloadedactivity.api.value_expression.RandomizedValueExpression;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MaxPropertyGrowthMethod extends SeparableSimulationMethod {
    public final Property<?> property;
    public final int updateType;
    public final boolean checkConditionsEveryHeight;
    public final boolean updateNeighbors;
    public final boolean reverseHeightGrowthDirection;
    public final boolean stopUpdatingAfterMaxHeight;
    public final boolean stopUpdatingAfterBlockage;
    public final boolean resetOnHeightChange;
    public final boolean onlyInWater;
    public final List<Pair<Property<?>, RandomizedValueExpression<Number>>> setProperties;
    public final List<String> transferProperties;
    public final Integer maxHeight;

    @Nullable public final List<Block> lowerBlocks;

    @Nullable public final RandomizedValueExpression<Block> bottomBlockReplacement;
    @Nullable public final FixedValueExpression<Number> maxValue;

    @Nullable public final AgeBloomConfig ageBloom;

    public record AgeBloomConfig(int bloomAtAge, Block bloomBlock, FixedValueExpression<Number> bloomProbability, List<FixedCondition> conditions) {
        public AgeBloomConfig(SimulationConfig config) {
            this(
                config.getNumber("bloom_at_age").intValue(),
                config.getBlock("bloom_block"),
                config.getFixedNumberExpression("bloom_probability"),
                config.getFixedConditionList("conditions")
            );
        }
    }

    public MaxPropertyGrowthMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, hasDependants);
        String propertyName = config.getString("property_name");

        Map<String, RandomizedValueExpression<Number>> setPropertiesNames = config.getRandomizedNumberExpressionMap("set_properties");

        this.updateType = config.getNumberOrDefault("update_type", Block.UPDATE_ALL).intValue();
        this.checkConditionsEveryHeight = config.getBooleanOrDefault("check_conditions_every_height", false);
        this.updateNeighbors = config.getBooleanOrDefault("update_neighbors", false);
        this.reverseHeightGrowthDirection = config.getBooleanOrDefault("reverse_height_growth_direction", false);
        this.stopUpdatingAfterMaxHeight = config.getBooleanOrDefault("stop_updating_after_max_height", false);
        this.stopUpdatingAfterBlockage = config.getBooleanOrDefault("stop_updating_after_blockage", false);
        this.resetOnHeightChange = config.getBooleanOrDefault("reset_on_height_change", true);
        this.onlyInWater = config.getBooleanOrDefault("only_in_water", false);
        this.transferProperties = config.getStringList("transfer_properties");
        this.maxHeight = config.getNumber("max_height").intValue();

        this.bottomBlockReplacement = config.getRandomizedBlockExpressionNullable("bottom_block_replacement");
        this.maxValue = config.getFixedNumberExpressionNullable("max_value");

        SimulationConfig bloomConfig = config.getConfigNullable("age_bloom");
        this.ageBloom = bloomConfig != null ? new AgeBloomConfig(bloomConfig) : null;

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

        int max = propertyMax;

        if (this.maxValue != null) {
            Number calculated = this.maxValue.evaluateFixed(level, state, pos);
            max = Math.min(propertyMax, calculated.intValue());
        }

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

        int freeSpaceLimit = this.maxHeight - height;

        int freeSpace;
        if (this.onlyInWater) {
            if (this.reverseHeightGrowthDirection) {
                for(freeSpace = 1; level.getBlockState(pos.below(freeSpace)).is(Blocks.WATER) && freeSpace <= freeSpaceLimit && (!this.checkConditionsEveryHeight || this.hasValidConditions(state, level, pos.below(freeSpace - 1))); ++freeSpace) {}
            } else {
                for(freeSpace = 1; level.getBlockState(pos.above(freeSpace)).is(Blocks.WATER) && freeSpace <= freeSpaceLimit && (!this.checkConditionsEveryHeight || this.hasValidConditions(state, level, pos.above(freeSpace - 1))); ++freeSpace) {}
            }
        } else {
            if (this.reverseHeightGrowthDirection) {
                for(freeSpace = 1; level.isEmptyBlock(pos.below(freeSpace)) && freeSpace <= freeSpaceLimit && (!this.checkConditionsEveryHeight || this.hasValidConditions(state, level, pos.below(freeSpace - 1))); ++freeSpace) {}
            } else {
                for(freeSpace = 1; level.isEmptyBlock(pos.above(freeSpace)) && freeSpace <= freeSpaceLimit && (!this.checkConditionsEveryHeight || this.hasValidConditions(state, level, pos.above(freeSpace - 1))); ++freeSpace) {}
            }
        }
        --freeSpace;

        boolean reachedMax = freeSpace == freeSpaceLimit;
        boolean failedConditions = false;
        boolean wasBlocked;

        BlockPos checkBlockedPos;
        BlockPos checkConditionsPos;
        if (this.reverseHeightGrowthDirection) {
            checkBlockedPos = pos.below(freeSpace + 1);
            checkConditionsPos = pos.below(freeSpace);
        } else {
            checkBlockedPos = pos.above(freeSpace + 1);
            checkConditionsPos = pos.above(freeSpace);
        }

        if (this.onlyInWater) {
            wasBlocked = !level.getBlockState(checkBlockedPos).is(Blocks.WATER);
        } else {
            wasBlocked = !level.isEmptyBlock(checkBlockedPos);
        }

        if (this.checkConditionsEveryHeight) failedConditions = !this.hasValidConditions(state, level, checkConditionsPos);

        // Updates for growing in height
        int updateCount = freeSpace;

        boolean stopAfterLastGrowth = reachedMax && stopUpdatingAfterMaxHeight || wasBlocked && stopUpdatingAfterBlockage || failedConditions;

        if (stopAfterLastGrowth) {
            if (freeSpace > 0) updateCount += Math.max(0, max - current);
            updateCount += max * Math.max(freeSpace - 1, 0);
        } else {
            updateCount += Math.max(0, max - current);
            updateCount += max * freeSpace;
        }

        return updateCount;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings) {
        DeferredBlockPlacer blockPlacer = DeferredBlockPlacer.empty();

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

        int newPropertyValue = current + occurrencesAndTimings.occurrences();

        boolean bloomAtEnd = false;
        if (this.ageBloom != null) {
            int flowerOpportunities = ((current > this.ageBloom.bloomAtAge ? current - (propertyMax + 1) : current) + this.ageBloom.bloomAtAge-1 + occurrencesAndTimings.occurrences()) / (propertyMax + 1);

            if (flowerOpportunities > 0) {
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

                for (int i = 0; i < flowerOpportunities; i++) {

                    BlockPos checkPos = reverseHeightGrowthDirection ? pos.below(i + 1) : pos.above(i + 1);
                    BlockPos contextPos = reverseHeightGrowthDirection ? pos.below(i) : pos.above(i);

                    if (!level.isEmptyBlock(checkPos)) break;

                    boolean doContinue = false;

                    FixedContext context = FixedContext.of(level, state, contextPos, Map.of("height", height + i));
                    for (FixedCondition condition : this.ageBloom.conditions) {
                        if (!condition.isValid(context)) {
                            doContinue = true;
                            break;
                        }
                    }

                    if (doContinue) continue;


                    double chanceToGrowFlower = this.ageBloom.bloomProbability.evaluate(context).doubleValue();

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

        SimulatedTime finalTime = occurrencesAndTimings.getFinalTime();
        long endTime = finalTime.endTime();

        if (growBlocks == 0) {
            doUpdateType = true;
            if (property instanceof IntegerProperty integerProperty) {
                state = state.setValue(integerProperty, valueRemainer);
            } else if (property instanceof BooleanProperty booleanProperty) {
                state = state.setValue(booleanProperty, valueRemainer > 0);
            } else {
                throw new RuntimeException("Property should have been validated at this point.");
            }
        } else if (this.bottomBlockReplacement != null) {
            doUpdateType = false;
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
        } else {
            doUpdateType = true;
            if (property instanceof IntegerProperty integerProperty) {
                state = state.setValue(integerProperty, belowValue);
            } else if (property instanceof BooleanProperty booleanProperty) {
                state = state.setValue(booleanProperty, belowValue > 0);
            } else {
                throw new RuntimeException("Property should have been validated at this point.");
            }
        }

        if (doUpdateType) {
            blockPlacer.setBlock(pos, state, updateNeighbors, this.updateType, finalTime);
        } else {
            blockPlacer.setBlock(pos, state, updateNeighbors, finalTime);
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
                } else {
                    throw new RuntimeException("Property should have been validated at this point.");
                }
            } else if (this.bottomBlockReplacement != null) {
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
            } else {
                if (property instanceof IntegerProperty integerProperty) {
                    state = thisBlock.defaultBlockState().setValue(integerProperty, belowValue);
                } else if (property instanceof BooleanProperty booleanProperty) {
                    state = thisBlock.defaultBlockState().setValue(booleanProperty, belowValue > 0);
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
                    throw new RuntimeException("SetProperties should have been validated at this point.");
                }
            }

            blockPlacer.setBlock(pos, state, updateNeighbors, finalTime);
        }

        if (bloomAtEnd) {
            BlockPos flowerPos = this.reverseHeightGrowthDirection ? pos.below() : pos.above();
            blockPlacer.setBlock(flowerPos, this.ageBloom.bloomBlock.defaultBlockState(), finalTime);
        }

        return blockPlacer;
    }
}
