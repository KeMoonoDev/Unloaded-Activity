package dev.moono.unloadedactivity.api.simulation_methods;

import dev.moono.unloadedactivity.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.value_expression_containers.FixedValueExpression;
import dev.moono.unloadedactivity.api.value_expression_containers.RandomizedValueExpression;
import dev.moono.unloadedactivity.datapack.Condition;
import dev.moono.unloadedactivity.datapack.ExpressionContext;
import dev.moono.unloadedactivity.datapack.ValueExpression;
import dev.moono.unloadedactivity.mixin.IntegerPropertyAccessor;
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
import java.util.Objects;
import java.util.Optional;

public class MaxPropertyGrowthMethod extends SeparableSimulationMethod {
    public String propertyName;
    public int updateType;
    public boolean updateNeighbors;
    public boolean reverseHeightGrowthDirection;
    public boolean stopUpdatingAfterMaxHeight;
    public boolean stopUpdatingAfterBlockage;
    public boolean resetOnHeightChange;
    public boolean onlyInWater;
    public Map<String, RandomizedValueExpression<Number>> setProperties;
    public List<String> transferProperties;
    public Integer maxHeight;

    @Nullable public final List<Block> lowerBlocks;

    @Nullable public RandomizedValueExpression<Block> bottomBlockReplacement;
    @Nullable public FixedValueExpression<Number> maxValue;

    @Nullable public AgeBloomConfig ageBloom;

    public record AgeBloomConfig(int bloomAtAge, Block bloomBlock, FixedValueExpression<Number> bloomProbability, List<Condition> condititons) {
        public AgeBloomConfig(SimulationConfig config) {
            this(
                config.getNumber("bloom_at_age").intValue(),
                config.getBlock("bloom_block"),
                config.getFixedNumberExpression("bloom_probability"),
                config.getFixedConditionList("conditions")
            );
        }
    }

    public MaxPropertyGrowthMethod(SimulationConfig config) {
        super(config);
        this.propertyName = config.getString("property_name");
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

        if (config.isDefined("lower_blocks")) {
            this.lowerBlocks = config.getBlockList("lower_blocks");
        } else if (this.bottomBlockReplacement != null) {
            this.lowerBlocks = this.bottomBlockReplacement.inner.getPossibleValues().toList();
        } else {
            this.lowerBlocks = null;
        }
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        Optional<Property<?>> maybeProperty = GameUtils.getProperty(state, this.propertyName);

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
            Number calculated = this.maxValue.evaluateFixed(level, state, pos);
            max = Math.min(propertyMax, calculated.intValue());
        }

        int updateCount = Math.max(0, max - current);


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
        Optional<Property<?>> maybeProperty = GameUtils.getProperty(state, this.propertyName);

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
            Number calculated = this.maxValue.evaluateFixed(level, state, pos);
            max = Math.min(propertyMax, calculated.intValue());
        } else {
            max = propertyMax;
        }

        int newPropertyValue = current + occurrences;

        boolean bloomAtEnd = false;
        if (this.ageBloom != null) {
            int flowerOpportunities = ((current > this.ageBloom.bloomAtAge ? current - (propertyMax + 1) : current) + this.ageBloom.bloomAtAge-1 + occurrences) / (propertyMax + 1);

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

                for (int i = 0; i < flowerOpportunities; i++) {

                    BlockPos checkPos = reverseHeightGrowthDirection ? pos.below(i + 1) : pos.above(i + 1);

                    if (!level.isEmptyBlock(checkPos)) break;

                    boolean doContinue = false;

                    ExpressionContext fixedContext = ExpressionContext.fixed(level, state, pos, Map.of("height", height + i), null);
                    for (Condition condition : this.ageBloom.condititons) {
                        if (!condition.isValid(fixedContext)) {
                            doContinue = true;
                            break;
                        }
                    }

                    if (doContinue) continue;


                    double chanceToGrowFlower = this.ageBloom.bloomProbability.inner.evaluate(fixedContext).doubleValue();

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


        long currentTime = GameUtils.getTime(level);

        long finishTime = currentTime - timePassed + simulationDuration;

        if (growBlocks == 0) {
            doUpdateType = true;
            if (property instanceof IntegerProperty integerProperty) {
                state = state.setValue(integerProperty, valueRemainer);
            } else if (property instanceof BooleanProperty booleanProperty) {
                state = state.setValue(booleanProperty, valueRemainer > 0);
            }
        } else if (this.bottomBlockReplacement != null) {
            doUpdateType = false;
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
            } else {
                if (property instanceof IntegerProperty integerProperty) {
                    state = thisBlock.defaultBlockState().setValue(integerProperty, belowValue);
                } else if (property instanceof BooleanProperty booleanProperty) {
                    state = thisBlock.defaultBlockState().setValue(booleanProperty, belowValue > 0);
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

            blockPlacer.setBlock(pos, state, updateNeighbors, simulationDuration);
        }

        if (bloomAtEnd) {
            BlockPos flowerPos = this.reverseHeightGrowthDirection ? pos.below() : pos.above();
            blockPlacer.setBlock(flowerPos, this.ageBloom.bloomBlock.defaultBlockState(), simulationDuration);
        }

        return blockPlacer;
    }
}
