package lol.zanspace.unloadedactivity.datapack;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import java.util.*;

import static lol.zanspace.unloadedactivity.datapack.IncompleteSimulationData.returnError;

public class IncompleteSimulateProperty {
    public Set<String> dependencies = new HashSet<>();
    public Optional<SimulationType> simulationType = Optional.empty();
    public Optional<Boolean> isPrecipitation = Optional.empty();
    public Optional<Boolean> requiresRain = Optional.empty();
    public Optional<String> target = Optional.empty();
    public Optional<Integer> maxHeight = Optional.empty();
    public Optional<Boolean> updateNeighbors = Optional.empty();
    public Optional<Boolean> resetOnHeightChange = Optional.empty();
    public Optional<Boolean> keepUpdatingAfterMaxHeight = Optional.empty();
    public Optional<Boolean> dropsResources = Optional.empty();
    public Optional<Boolean> reverseHeightGrowthDirection = Optional.empty();
    public Optional<Boolean> increasePerHeight = Optional.empty();
    public Optional<Boolean> onlyInWater = Optional.empty();
    public Optional<Integer> updateType = Optional.empty();
    public Optional<CalculateValue<Number>> advanceProbability = Optional.empty();
    public Optional<CalculateValue<Number>> maxValue = Optional.empty();
    public ArrayList<Condition> conditions = new ArrayList<>();
    public Optional<String> buddingDirectionProperty = Optional.empty();
    public Optional<Integer> minWaterValue = Optional.empty();
    public Optional<String> waterloggedProperty = Optional.empty();
    public ArrayList<Direction> ignoreBuddingDirections = new ArrayList<>();
    public HashMap<String, IncompleteRandomProperty> randomProperties = new HashMap<>();
    public Optional<CalculateValue<Number>> hatchCount = Optional.empty();
    public Optional<Integer> startingAge = Optional.empty();
    public Optional< #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif > simulateWithGroup = Optional.empty();
    public Optional< #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif > hatchEntity = Optional.empty();
    public Optional< #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif > blockReplacement = Optional.empty();
    public Optional<ArrayList< #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif >> buddingBlocks = Optional.empty();

    public void merge(IncompleteSimulateProperty otherSimulateProperty) {
        this.simulationType = otherSimulateProperty.simulationType.or(() -> this.simulationType);
        this.isPrecipitation = otherSimulateProperty.isPrecipitation.or(() -> this.isPrecipitation);
        this.requiresRain = otherSimulateProperty.requiresRain.or(() -> this.requiresRain);
        this.target = otherSimulateProperty.target.or(() -> this.target);
        this.maxHeight = otherSimulateProperty.maxHeight.or(() -> this.maxHeight);
        this.dependencies.addAll(otherSimulateProperty.dependencies);
        this.updateType = otherSimulateProperty.updateType.or(() -> this.updateType);
        this.updateNeighbors = otherSimulateProperty.updateNeighbors.or(() -> this.updateNeighbors);
        this.resetOnHeightChange = otherSimulateProperty.resetOnHeightChange.or(() -> this.resetOnHeightChange);
        this.keepUpdatingAfterMaxHeight = otherSimulateProperty.keepUpdatingAfterMaxHeight.or(() -> this.keepUpdatingAfterMaxHeight);
        this.conditions.addAll(otherSimulateProperty.conditions);
        this.ignoreBuddingDirections.addAll(otherSimulateProperty.ignoreBuddingDirections);
        this.minWaterValue = otherSimulateProperty.minWaterValue.or(() -> this.minWaterValue);
        this.waterloggedProperty = otherSimulateProperty.waterloggedProperty.or(() -> this.waterloggedProperty);
        this.buddingDirectionProperty = otherSimulateProperty.buddingDirectionProperty.or(() -> this.buddingDirectionProperty);
        this.buddingBlocks = otherSimulateProperty.buddingBlocks.or(() -> this.buddingBlocks);
        this.blockReplacement = otherSimulateProperty.blockReplacement.or(() -> this.blockReplacement);
        this.dropsResources = otherSimulateProperty.dropsResources.or(() -> this.dropsResources);
        this.reverseHeightGrowthDirection = otherSimulateProperty.reverseHeightGrowthDirection.or(() -> this.reverseHeightGrowthDirection);
        this.onlyInWater = otherSimulateProperty.onlyInWater.or(() -> this.onlyInWater);
        this.increasePerHeight = otherSimulateProperty.increasePerHeight.or(() -> this.increasePerHeight);
        this.hatchEntity = otherSimulateProperty.hatchEntity.or(() -> this.hatchEntity);
        this.startingAge = otherSimulateProperty.startingAge.or(() -> this.startingAge);
        this.simulateWithGroup = otherSimulateProperty.simulateWithGroup.or(() -> this.simulateWithGroup);

        for (var entry : otherSimulateProperty.randomProperties.entrySet()) {
            IncompleteRandomProperty thisRandomProperty = this.randomProperties.computeIfAbsent(entry.getKey(), k -> new IncompleteRandomProperty());
            IncompleteRandomProperty otherRandomProperty = entry.getValue();

            thisRandomProperty.merge(otherRandomProperty);
        }

        if (otherSimulateProperty.advanceProbability.isPresent() && this.advanceProbability.isPresent()) {
            var oldProbability = this.advanceProbability.get();
            var newProbability = otherSimulateProperty.advanceProbability.get().replicate();

            newProbability.replaceSuper(oldProbability);

            this.advanceProbability = Optional.of(newProbability);
        } else {
            this.advanceProbability = otherSimulateProperty.advanceProbability.map(CalculateValue::replicate).or(() -> this.advanceProbability);
        }

        if (otherSimulateProperty.maxValue.isPresent() && this.maxValue.isPresent()) {
            var oldValue = this.maxValue.get();
            var newValue = otherSimulateProperty.maxValue.get().replicate();

            newValue.replaceSuper(oldValue);

            this.maxValue = Optional.of(newValue);
        } else {
            this.maxValue = otherSimulateProperty.maxValue.map(CalculateValue::replicate).or(() -> this.maxValue);
        }

        if (otherSimulateProperty.hatchCount.isPresent() && this.hatchCount.isPresent()) {
            var oldValue = this.hatchCount.get();
            var newValue = otherSimulateProperty.hatchCount.get().replicate();

            newValue.replaceSuper(oldValue);

            this.hatchCount = Optional.of(newValue);
        } else {
            this.hatchCount = otherSimulateProperty.hatchCount.map(CalculateValue::replicate).or(() -> this.hatchCount);
        }
    }

    public <T> void parseAndApplyProbability(DynamicOps<T> ops, T input) {
        CalculateValue calculateValue = CalculateValue.parseNumber(ops, input);
        this.advanceProbability = Optional.of(calculateValue);
    }

    public <T> void parseAndApplyCondition(DynamicOps<T> ops, T input) {
        Condition condition = Condition.parse(ops, input).getOrThrow(true, e->{});
        this.conditions.add(condition);
    }

    static public <T> DataResult<IncompleteSimulateProperty> parse(DynamicOps<T> ops, T input) {

        IncompleteSimulateProperty simulateProperty = new IncompleteSimulateProperty();

        var propertyInfoResult = ops.getMap(input);

        if (propertyInfoResult.error().isPresent()) {
            return returnError(propertyInfoResult);
        }

        MapLike<T> propertyInfo = propertyInfoResult.result().get();

        {
            T mapValue = propertyInfo.get("simulation_type");
            if (mapValue != null) {
                DataResult<String> valueResult = ops.getStringValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }

                String simulationTypeString = valueResult.result().get();
                Optional<SimulationType> maybeSimulationType = SimulationType.fromString(simulationTypeString);

                if (maybeSimulationType.isEmpty()) {
                    return returnError(simulationTypeString + " is not a valid simulation type.");
                }

                simulateProperty.simulationType = maybeSimulationType;
            }
        }

        {
            T mapValue = propertyInfo.get("is_precipitation");
            if (mapValue != null) {
                DataResult<Boolean> valueResult = ops.getBooleanValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }

                simulateProperty.isPrecipitation = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("requires_rain");
            if (mapValue != null) {
                DataResult<Boolean> valueResult = ops.getBooleanValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }

                simulateProperty.requiresRain = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("target");
            if (mapValue != null) {
                DataResult<String> valueResult = ops.getStringValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }
                simulateProperty.target = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("update_type");
            if (mapValue != null) {
                DataResult<String> stringResult = ops.getStringValue(mapValue);
                if (stringResult.result().isPresent()) {
                    String updateType = stringResult.result().get();
                    switch (updateType.toLowerCase()) {
                        case "update_clients" -> simulateProperty.updateType = Optional.of(Block.UPDATE_CLIENTS);
                        case "update_invisible" -> simulateProperty.updateType = Optional.of(Block.UPDATE_INVISIBLE);
                        case "update_all" -> simulateProperty.updateType = Optional.of(Block.UPDATE_ALL);
                        case "update_none" -> simulateProperty.updateType = Optional.of(Block.UPDATE_NONE);
                        default -> {
                            return returnError("Invalid update type: " + updateType);
                        }
                    }
                } else {
                    DataResult<Number> numberResult = ops.getNumberValue(mapValue);
                    if (numberResult.error().isPresent()) {
                        return returnError("Must be a number or a string.", numberResult);
                    }
                    simulateProperty.updateType = Optional.of(numberResult.result().get().intValue());
                }
            }
        }

        {
            T mapValue = propertyInfo.get("advance_probability");
            if (mapValue != null) {
                simulateProperty.parseAndApplyProbability(ops, mapValue);
            }
        }

        {
            T mapValue = propertyInfo.get("max_value");
            if (mapValue != null) {
                CalculateValue<Number> calculateValue = CalculateValue.parseNumber(ops, mapValue);
                simulateProperty.maxValue = Optional.of(calculateValue);
            }
        }

        {
            T mapValue = propertyInfo.get("max_height");
            if (mapValue != null) {
                DataResult<Number> valueResult = ops.getNumberValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }
                simulateProperty.maxHeight = valueResult.result().map(Number::intValue);
            }
        }

        {
            T mapValue = propertyInfo.get("update_neighbors");
            if (mapValue != null) {
                DataResult<Boolean> valueResult = ops.getBooleanValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }
                simulateProperty.updateNeighbors = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("drops_resources");
            if (mapValue != null) {
                DataResult<Boolean> valueResult = ops.getBooleanValue(mapValue);
                if (valueResult.result().isEmpty()) {
                    return returnError(valueResult);
                }
                simulateProperty.dropsResources = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("reverse_height_growth_direction");
            if (mapValue != null) {
                DataResult<Boolean> valueResult = ops.getBooleanValue(mapValue);
                if (valueResult.result().isEmpty()) {
                    return returnError(valueResult);
                }
                simulateProperty.reverseHeightGrowthDirection = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("increase_per_height");
            if (mapValue != null) {
                DataResult<Boolean> valueResult = ops.getBooleanValue(mapValue);
                if (valueResult.result().isEmpty()) {
                    return returnError(valueResult);
                }
                simulateProperty.increasePerHeight = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("only_in_water");
            if (mapValue != null) {
                DataResult<Boolean> valueResult = ops.getBooleanValue(mapValue);
                if (valueResult.result().isEmpty()) {
                    return returnError(valueResult);
                }
                simulateProperty.onlyInWater = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("block_replacement");
            if (mapValue != null) {
                var result = ResourceLocation.CODEC.decode(ops, mapValue);
                if (result.result().isEmpty()) {
                    returnError(result);
                }

                simulateProperty.blockReplacement = result.result().map(Pair::getFirst);
            }
        }

        {
            T mapValue = propertyInfo.get("reset_on_height_change");
            if (mapValue != null) {
                DataResult<Boolean> valueResult = ops.getBooleanValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }
                simulateProperty.resetOnHeightChange = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("keep_updating_after_max_height");
            if (mapValue != null) {
                DataResult<Boolean> valueResult = ops.getBooleanValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }
                simulateProperty.keepUpdatingAfterMaxHeight = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("ignore_budding_directions");
            if (mapValue != null) {
                var listResult = ops.getStream(mapValue);
                if (listResult.error().isPresent()) {
                    return returnError(listResult);
                }

                for (T ignoredDirection : listResult.result().get().toList()) {
                    var result = Direction.CODEC.decode(ops, ignoredDirection);
                    if (result.result().isEmpty()) {
                        returnError(result);
                    }

                    simulateProperty.ignoreBuddingDirections.add(result.result().get().getFirst());
                }
            }
        }

        {
            T mapValue = propertyInfo.get("starting_age");
            if (mapValue != null) {
                DataResult<Number> valueResult = ops.getNumberValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }
                simulateProperty.startingAge = valueResult.result().map(Number::intValue);
            }
        }

        {
            T mapValue = propertyInfo.get("hatch_entity");
            if (mapValue != null) {
                var result = ResourceLocation.CODEC.decode(ops, mapValue);
                if (result.result().isEmpty()) {
                    returnError(result);
                }

                simulateProperty.hatchEntity = result.result().map(Pair::getFirst);
            }
        }

        {
            T mapValue = propertyInfo.get("hatch_count");
            if (mapValue != null) {
                CalculateValue<Number> result = CalculateValue.parseNumber(ops, mapValue);
                simulateProperty.hatchCount = Optional.of(result);
            }
        }

        {
            T mapValue = propertyInfo.get("min_water_value");
            if (mapValue != null) {
                DataResult<Number> valueResult = ops.getNumberValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }
                simulateProperty.minWaterValue = valueResult.result().map(Number::intValue);
            }
        }

        {
            T mapValue = propertyInfo.get("waterlogged_property");
            if (mapValue != null) {
                DataResult<String> valueResult = ops.getStringValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }
                simulateProperty.waterloggedProperty = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("budding_direction_property");
            if (mapValue != null) {
                DataResult<String> valueResult = ops.getStringValue(mapValue);
                if (valueResult.error().isPresent()) {
                    return returnError(valueResult);
                }
                simulateProperty.buddingDirectionProperty = valueResult.result();
            }
        }

        {
            T mapValue = propertyInfo.get("budding_blocks");
            if (mapValue != null) {
                var listResult = ops.getStream(mapValue);
                if (listResult.error().isPresent()) {
                    return returnError(listResult);
                }

                ArrayList< ResourceLocation > buddingBlocks = new ArrayList<>();

                for (T buddingBlockId : listResult.result().get().toList()) {
                    var result = ResourceLocation.CODEC.decode(ops, buddingBlockId);
                    if (result.result().isEmpty()) {
                        returnError(result);
                    }

                    buddingBlocks.add(result.result().get().getFirst());
                }

                simulateProperty.buddingBlocks = Optional.of(buddingBlocks);
            }
        }

        {
            T mapValue = propertyInfo.get("conditions");
            if (mapValue != null) {
                var listResult = ops.getStream(mapValue);
                if (listResult.error().isPresent()) {
                    return returnError(listResult);
                }

                for (T condition : listResult.result().get().toList()) {
                    simulateProperty.parseAndApplyCondition(ops, condition);
                }
            }
        }

        {
            T mapValue = propertyInfo.get("dependencies");
            if (mapValue != null) {
                var dependencies = ops.getStream(mapValue);
                if (dependencies.error().isPresent()) {
                    return returnError(dependencies);
                }

                for (T dependencyValue : dependencies.result().get().toList()) {
                    var stringResult = ops.getStringValue(dependencyValue);

                    if (stringResult.error().isPresent()) {
                        return returnError(stringResult);
                    }

                    simulateProperty.dependencies.add(stringResult.result().get());
                }
            }
        }

        {
            T mapValue = propertyInfo.get("random_properties");
            if (mapValue != null) {
                var mapResult = ops.getMap(mapValue);
                if (mapResult.result().isEmpty()) {
                    return returnError(mapResult);
                }

                for (Pair<T, T> pair : mapResult.result().get().entries().toList()) {
                    var keyResult = ops.getStringValue(pair.getFirst());
                    if (keyResult.result().isEmpty()) {
                        returnError(keyResult);
                    }

                    var valueResult = IncompleteRandomProperty.parse(ops, pair.getSecond());
                    if (valueResult.result().isEmpty()) {
                        returnError(valueResult);
                    }

                    simulateProperty.randomProperties.put(keyResult.result().get(), valueResult.result().get());
                }
            }
        }

        {
            T mapValue = propertyInfo.get("simulate_with_group");
            if (mapValue != null) {
                var result = ResourceLocation.CODEC.decode(ops, mapValue);
                if (result.result().isEmpty()) {
                    returnError(result);
                }

                simulateProperty.simulateWithGroup = result.result().map(Pair::getFirst);
            }
        }

        return DataResult.success(simulateProperty);
    }
}
