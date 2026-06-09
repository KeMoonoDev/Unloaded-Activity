package lol.zanspace.unloadedactivity.datapack;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

#if MC_VER >= MC_1_19_4
import net.minecraft.core.registries.BuiltInRegistries;
#else
import net.minecraft.core.Registry;
#endif

#if MC_VER >= MC_1_21_3
import net.minecraft.world.level.block.state.properties.EnumProperty;
#else
import net.minecraft.world.level.block.state.properties.DirectionProperty;
#endif

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;

//import static lol.zanspace.unloadedactivity.interfaces.SimulateChunkBlocks.getProperty;


public class SimulateProperty {
    /*
    public final String target;
    public final SimulationType simulationType;
    public final boolean isPrecipitation;
    public final boolean requiresRain;
    public final boolean canBeAffectedByWeather;
    public final boolean canBeAffectedByTime;
    public final ValueExpression<Number> advanceProbability;

    public final List<String> dependencies;
    public final List<Condition> conditions;
    public final List<String> transferProperties;
    public final Optional<ValueExpression<Number>> maxValue;
    public final Optional<Integer> maxHeight;
    public final Optional<String> waterloggedProperty;
    public final List<Direction> ignoreBuddingDirections;
    public final List<Pair<String, ValueExpression<Number>>> setProperties;
    public final Optional<String> buddingDirectionProperty;
    public final Optional<Integer> startingAge;
    public final Optional<ValueExpression<Number>> hatchCount;

    public final Optional<ValueExpression<Block>> blockReplacement;
    public final Optional<EntityType<?>> hatchEntity;

    public int updateType;
    public boolean updateNeighbors;
    public boolean resetOnHeightChange;
    public boolean keepUpdatingAfterMaxHeight;
    public boolean reverseHeightGrowthDirection;
    public boolean increasePerHeight;
    public boolean dropsResources;
    public boolean onlyInWater;
    public int minWaterValue;

    public List<Block> buddingBlocks;

    public Optional<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif> simulateWithGroup;

    public SimulateProperty(IncompleteSimulateProperty incomplete, String targetFallback) {
        // Required fields for all simulation types
        this.target = incomplete.target.orElse(targetFallback);

        if (incomplete.simulationType.isEmpty())
            throw new RuntimeException("simulation_method has not been set.");

        this.simulationType = incomplete.simulationType.get();

        if (incomplete.advanceProbability.isEmpty())
            throw new RuntimeException("advance_probability has not been set.");

        if (incomplete.advanceProbability.get().isRandom())
            throw new RuntimeException("advance_probability cannot have random values.");

        this.advanceProbability = incomplete.advanceProbability.get();
        this.canBeAffectedByWeather = this.advanceProbability.canBeAffectedByWeather();
        this.canBeAffectedByTime = this.advanceProbability.canBeAffectedByTime();

        // Simple transfer
        this.dependencies = incomplete.dependencies.stream().toList();
        this.conditions = incomplete.conditions.stream().toList();
        this.transferProperties = incomplete.transferProperties.stream().toList();
        this.maxValue = incomplete.maxValue;
        this.maxHeight = incomplete.maxHeight;
        this.waterloggedProperty = incomplete.waterloggedProperty;
        this.ignoreBuddingDirections = incomplete.ignoreBuddingDirections.stream().toList();
        this.buddingDirectionProperty = incomplete.buddingDirectionProperty;
        this.startingAge = incomplete.startingAge;
        this.hatchCount = incomplete.hatchCount;
        this.simulateWithGroup = incomplete.simulateWithGroup;

        // Convert types.
        this.blockReplacement = incomplete.blockReplacement.map(calculateValue -> calculateValue.map((id) -> {
            #if MC_VER >= MC_1_19_4
            Optional<Block> maybeBlock = BuiltInRegistries.BLOCK.getOptional(id);
            #else
            Optional<Block> maybeBlock = Registry.BLOCK.getOptional(id);
            #endif
            if (maybeBlock.isEmpty()) {
                throw new RuntimeException(id + " is not a valid block.");
            }
            return maybeBlock.get();
        }));

        this.hatchEntity = incomplete.hatchEntity.map(id -> {
            #if MC_VER >= MC_1_19_4
            Optional<EntityType<?>> maybeEntity = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            #else
            Optional<EntityType<?>> maybeEntity = Registry.ENTITY_TYPE.getOptional(id);
            #endif
            if (maybeEntity.isEmpty()) {
                throw new RuntimeException(id + " is not a valid mob.");
            }
            return maybeEntity.get();
        });

        ArrayList<Pair<String, ValueExpression<Number>>> setPropertiesList = new ArrayList<>();
        for (var entry : incomplete.setProperties.entrySet()) {
            setPropertiesList.add(Pair.of(entry.getKey(), entry.getValue()));
        }
        this.setProperties = setPropertiesList.stream().toList();

        // Default values for optional fields with defaults.
        this.isPrecipitation = incomplete.isPrecipitation.orElse(false);
        this.requiresRain = incomplete.requiresRain.orElse(this.isPrecipitation);
        this.updateType = incomplete.updateType.orElse(Block.UPDATE_ALL);
        this.updateNeighbors = incomplete.updateNeighbors.orElse(false);
        this.resetOnHeightChange = incomplete.resetOnHeightChange.orElse(true);
        this.keepUpdatingAfterMaxHeight = incomplete.keepUpdatingAfterMaxHeight.orElse(true);
        this.reverseHeightGrowthDirection = incomplete.reverseHeightGrowthDirection.orElse(false);
        this.dropsResources = incomplete.dropsResources.orElse(true);
        this.increasePerHeight = incomplete.increasePerHeight.orElse(false);
        this.onlyInWater = incomplete.onlyInWater.orElse(false);
        this.minWaterValue = incomplete.minWaterValue.orElse(0);

        // Default value for required fields depending on simulationType.
        this.buddingBlocks = List.of();

        switch (this.simulationType) {
            case PROPERTY -> {
            }
            case BUDDING -> {
                if (incomplete.buddingBlocks.isEmpty()) {
                    throw new RuntimeException("budding_blocks has not been set.");
                }

                if (incomplete.buddingBlocks.get().isEmpty()) {
                    throw new RuntimeException("budding_blocks must not be empty.");
                }

                ArrayList<Block> buddingBlocksList = new ArrayList<>();

                for (var buddingBlockId : incomplete.buddingBlocks.get()) {

                    #if MC_VER >= MC_1_19_4
                    Optional<Block> maybeBlock = BuiltInRegistries.BLOCK.getOptional(buddingBlockId);
                    #else
                    Optional<Block> maybeBlock = Registry.BLOCK.getOptional(buddingBlockId);
                    #endif
                    if (maybeBlock.isEmpty()) {
                        throw new RuntimeException(buddingBlockId + " is not a valid block.");
                    }

                    Block block = maybeBlock.get();


                    if (this.buddingDirectionProperty.isPresent()) {
                        String buddingDirectionProperty = this.buddingDirectionProperty.get();
                        Optional<Property<?>> maybeProperty = getProperty(block.defaultBlockState(), buddingDirectionProperty);

                        if (maybeProperty.isEmpty()) {
                            throw new RuntimeException(buddingDirectionProperty + " is not a valid direction property on " + block + ". It doesn't exist.");
                        }

                        Property<?> property = maybeProperty.get();

                        if (property instanceof #if MC_VER >= MC_1_21_3 EnumProperty<?> #else DirectionProperty #endif directionProperty ) {
                            List<Direction> availableDirections = Arrays.stream(Direction.values()).filter(direction -> !this.ignoreBuddingDirections.contains(direction)).toList();
                            var possibleValues = directionProperty.getPossibleValues();
                            for (Direction direction : availableDirections) {
                                if (!possibleValues.contains(direction)) {
                                    throw new RuntimeException(block + " direction property " + buddingDirectionProperty + " doesn't support the direction " + direction + ". Consider adding it to ignore_budding_directions.");
                                }
                            }
                        } else {
                            throw new RuntimeException(buddingDirectionProperty + " is not a valid direction property on " + block + ". It holds a different type.");
                        }
                    }

                    if (this.waterloggedProperty.isPresent()) {
                        String waterloggedProperty = this.waterloggedProperty.get();
                        Optional<Property<?>> maybeProperty = getProperty(block.defaultBlockState(), waterloggedProperty);

                        if (maybeProperty.isEmpty()) {
                            throw new RuntimeException(waterloggedProperty + " is not a valid boolean property on " + block + ". It doesn't exist.");
                        }

                        Property<?> property = maybeProperty.get();

                        if (property instanceof BooleanProperty) {
                            // yay
                        } else {
                            throw new RuntimeException(waterloggedProperty + " is not a valid boolean property on " + block + ". It holds a different type.");
                        }
                    }

                    buddingBlocksList.add(block);
                }

                this.buddingBlocks = buddingBlocksList.stream().toList();
            }
            case ACTION -> {
            }
        }
    }

    public boolean isBudding() {
        return this.simulationType == SimulationType.BUDDING;
    }

    public boolean isBudding(String target) {
        return this.simulationType == SimulationType.BUDDING && this.target.equals(target);
    }

    public boolean isDecay() {
        return this.simulationType == SimulationType.DECAY;
    }

    public boolean isDecay(String target) {
        return this.simulationType == SimulationType.DECAY && this.target.equals(target);
    }

    public boolean isAction() {
        return this.simulationType == SimulationType.ACTION;
    }

    public boolean isAction(String target) {
        return this.simulationType == SimulationType.ACTION && this.target.equals(target);
    }
     */
}
