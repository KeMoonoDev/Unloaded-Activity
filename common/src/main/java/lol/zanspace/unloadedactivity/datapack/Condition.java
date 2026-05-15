package lol.zanspace.unloadedactivity.datapack;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

import static lol.zanspace.unloadedactivity.datapack.IncompleteSimulationData.returnError;

public record Condition (CalculateValue<Number> value1, CalculateValue<Number> value2, Comparison comparison) {
    public boolean isValid(CalculationData data) {
        Number calculatedValue1 = value1.calculateValue(data);
        Number calculatedValue2 = value2.calculateValue(data);
        boolean result = comparison.compare(calculatedValue1.floatValue(), calculatedValue2.floatValue());

        if (UnloadedActivity.config.debugLogs)
            UnloadedActivity.LOGGER.info("Checking if " + value1.getClass().getSimpleName() + " (" + calculatedValue1 + ") " + comparison.name() + " " + value2.getClass().getSimpleName() +  " (" + calculatedValue2 + ") (" + result + ")");

        return result;
    }

    public boolean isDynamic() {
        return this.canBeAffectedByWeather() || this.canBeAffectedByTime();
    };
    public boolean canBeAffectedByWeather() {
        return value1.canBeAffectedByWeather() || value2.canBeAffectedByWeather();
    };
    public boolean isRandom() {
        return value1.isRandom() || value2.isRandom();
    }
    public boolean canBeAffectedByTime() {
        return value1.canBeAffectedByTime() || value2.canBeAffectedByTime();
    };
    public boolean isAffectedByWeather(CalculationData data) {
        return value1.isAffectedByWeather(data) || value2.isAffectedByWeather(data);
    };

    public long getNextConditionSwitchDuration(CalculationData data) {
        return Math.min(value1.getNextValueSwitchDuration(data), value2.getNextValueSwitchDuration(data));
    };

    public static <T> DataResult<Condition> parse(DynamicOps<T> ops, T input) {
        var mapValue = ops.getMap(input);
        if (mapValue.result().isPresent()) {
            MapLike<T> map = mapValue.result().get();

            DataResult<String> comparisonResult = ops.getStringValue(map.get("comparison"));
            if (comparisonResult.error().isPresent()) {
                return returnError(comparisonResult);
            }
            String comparisonString = comparisonResult.result().get();
            Optional<Comparison> maybeComparison = Comparison.fromString(comparisonString);

            if (!maybeComparison.isPresent()) {
                throw new RuntimeException(comparisonString + " is not a valid comparison.");
            }

            Comparison comparison = maybeComparison.get();

            T checkValue = map.get("check");
            CalculateValue<Number> checkCalculateValue = CalculateValue.parseNumber(ops, checkValue);

            T valueValue = map.get("value");
            CalculateValue<Number> valueCalculateValue = CalculateValue.parseNumber(ops, valueValue);

            return DataResult.success(new Condition(checkCalculateValue, valueCalculateValue, comparison));
        }

        throw new RuntimeException("Invalid condition");
    }
}
