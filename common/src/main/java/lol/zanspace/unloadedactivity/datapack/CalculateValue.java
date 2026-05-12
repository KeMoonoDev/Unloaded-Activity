package lol.zanspace.unloadedactivity.datapack;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import lol.zanspace.unloadedactivity.datapack.calculate_value.*;
import net.minecraft.core.Vec3i;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

import static lol.zanspace.unloadedactivity.datapack.IncompleteSimulationData.returnError;

public interface CalculateValue<T> {
    T calculateValue(CalculationData data);

    default boolean isAffectedByWeather(CalculationData data) {
        return this.canBeAffectedByWeather();
    };

    boolean canBeAffectedByWeather();

    boolean canBeAffectedByTime();

    long getNextValueSwitchDuration(CalculationData data);

    /// Doesn't guarantee a clone. If a type doesn't get mutated, it's able to return itself.
    CalculateValue<T> replicate();

    void replaceSuper(CalculateValue<T> superValue);

    default boolean isSuper() {
        return false;
    };

    <U> CalculateValue<U> map(Function<T, U> mapFunction);

    static <T> CalculateValue<Number> parseNumber(DynamicOps<T> ops, T input) {

        var numberValue = ops.getNumberValue(input);
        if (numberValue.result().isPresent()) {
            return new SimpleValue<>(numberValue.result().get());
        }

        var booleanValue = ops.getBooleanValue(input);
        if (booleanValue.result().isPresent()) {
            return new SimpleValue<>(booleanValue.result().get() ? 1 : 0);
        }

        var stringValue = ops.getStringValue(input);
        if (stringValue.result().isPresent()) {
            String variableName = stringValue.result().get();

            Optional<FetchNumberValue> maybeFetchValue = FetchNumberValue.fromString(variableName);
            if (maybeFetchValue.isPresent()) {
                return maybeFetchValue.get();
            }

            switch (variableName.toLowerCase()) {
                case "local_brightness" -> {
                    return new LocalBrightnessValue();
                }
                case "local_brightness_above" -> {
                    return new LocalBrightnessValue(new Vec3i(0, 1, 0));
                }
            }

            if (variableName.toLowerCase().startsWith("local_brightness:")) {
                String propertyName = variableName.substring("property:".length());
                return new PropertyValue(propertyName);
            }

            if (variableName.toLowerCase().startsWith("property:")) {
                String propertyName = variableName.substring("property:".length());
                return new PropertyValue(propertyName);
            }

            throw new RuntimeException(variableName + " is not a valid value.");
        }

        var mapValue = ops.getMap(input);
        if (mapValue.result().isPresent()) {
            MapLike<T> map = mapValue.result().get();

            DataResult<String> operatorResult = ops.getStringValue(map.get("operator"));
            if (operatorResult.result().isPresent()) {
                String operatorValue = operatorResult.result().get();
                T oneValue = map.get("value");
                T value1 = map.get("value1");
                T value2 = map.get("value2");

                switch (operatorValue.toLowerCase()) {
                    case "+" -> {
                        return new NumberOperatorValue(Operator.ADD, parseNumber(ops, value1), parseNumber(ops, value2));
                    }
                    case "-" -> {
                        return new NumberOperatorValue(Operator.SUB, parseNumber(ops, value1), parseNumber(ops, value2));
                    }
                    case "/" -> {
                        return new NumberOperatorValue(Operator.DIV, parseNumber(ops, value1), parseNumber(ops, value2));
                    }
                    case "*" -> {
                        return new NumberOperatorValue(Operator.MUL, parseNumber(ops, value1), parseNumber(ops, value2));
                    }
                    case "^" -> {
                        return new NumberOperatorValue(Operator.POW, parseNumber(ops, value1), parseNumber(ops, value2));
                    }
                    case "^2" -> {
                        return new NumberOperatorValue(Operator.POW2, parseNumber(ops, oneValue));
                    }
                    case "floor" -> {
                        return new NumberOperatorValue(Operator.FLOOR, parseNumber(ops, oneValue));
                    }
                }

                throw new RuntimeException("Invalid operator " + operatorValue);

            }

            T predicateResult = map.get("predicate");

            if (predicateResult != null) {
                DataResult<Condition> conditionResult = Condition.parse(ops, predicateResult);

                if (conditionResult.result().isEmpty()) {
                    throw new RuntimeException("Failed to parse predicate: " + conditionResult.error().get().message());
                }

                Condition condition = conditionResult.result().get();

                T trueValue = map.get("success");
                T falseValue = map.get("fail");

                return new ConditionalValue<>(condition, parseNumber(ops, trueValue), parseNumber(ops, falseValue));

            }


            ArrayList<Pair<Long, CalculateValue<Number>>> list = new ArrayList<>();
            for (Iterator<Pair<T, T>> it = map.entries().iterator(); it.hasNext(); ) {
                var pair = it.next();
                var stringKeyResult = ops.getStringValue(pair.getFirst());
                if (stringKeyResult.error().isPresent()) {
                    throw new RuntimeException(stringKeyResult.error().get().message());
                }
                String stringKey = stringKeyResult.result().get();
                try {
                    long number = Long.parseLong(stringKey);
                    list.add(Pair.of(number, parseNumber(ops, pair.getSecond())));
                } catch(NumberFormatException e){
                    throw new RuntimeException("Probability value has no valid operator key, but also doesn't only contain integer keys.");
                }
            }
            if (list.isEmpty()) {
                throw new RuntimeException("Probability value has no keys.");
            }

            return new TimeValue(list);

        }

        throw new RuntimeException("Invalid value");
    }

    static <T> CalculateValue<String> parseString(DynamicOps<T> ops, T input) {

        var stringValue = ops.getStringValue(input);
        if (stringValue.result().isPresent()) {
            return new SimpleValue<>(stringValue.result().get());
        }

        var mapValue = ops.getMap(input);
        if (mapValue.result().isPresent()) {
            MapLike<T> map = mapValue.result().get();

            T predicateResult = map.get("predicate");

            if (predicateResult != null) {
                DataResult<Condition> conditionResult = Condition.parse(ops, predicateResult);

                if (conditionResult.result().isEmpty()) {
                    throw new RuntimeException("Failed to parse predicate: " + conditionResult.error().get().message());
                }

                Condition condition = conditionResult.result().get();

                T trueValue = map.get("success");
                T falseValue = map.get("fail");

                return new ConditionalValue<>(condition, parseString(ops, trueValue), parseString(ops, falseValue));

            }


            ArrayList<Pair<Long, CalculateValue<String>>> list = new ArrayList<>();
            for (Iterator<Pair<T, T>> it = map.entries().iterator(); it.hasNext(); ) {
                var pair = it.next();
                var stringKeyResult = ops.getStringValue(pair.getFirst());
                if (stringKeyResult.error().isPresent()) {
                    throw new RuntimeException(stringKeyResult.error().get().message());
                }
                String stringKey = stringKeyResult.result().get();
                try {
                    long number = Long.parseLong(stringKey);
                    list.add(Pair.of(number, parseString(ops, pair.getSecond())));
                } catch(NumberFormatException e){
                    throw new RuntimeException("Probability value has no valid operator key, but also doesn't only contain integer keys.");
                }
            }
            if (list.isEmpty()) {
                throw new RuntimeException("Probability value has no keys.");
            }

            return new TimeValue<>(list);

        }

        throw new RuntimeException("Invalid value");
    }
}