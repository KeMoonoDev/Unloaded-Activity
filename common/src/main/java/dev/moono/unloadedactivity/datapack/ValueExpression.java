package dev.moono.unloadedactivity.datapack;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.datapack.value_expression.*;
import dev.moono.unloadedactivity.datapack.value_expression.*;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

public interface ValueExpression<T> {
    T evaluate(ValueContext context);

    default boolean isAffectedByWeather(ValueContext context) {
        return this.canBeAffectedByWeather();
    };

    boolean canBeAffectedByWeather();

    boolean canBeAffectedByTime();

    boolean isRandom();

    long getNextValueSwitchDuration(ValueContext context);

    default long getNextConditionSwitchDuration(ValueContext context, float target, Comparison comparison) {
        return getNextValueSwitchDuration(context);
    };

    /// Doesn't guarantee a clone. If a type doesn't get mutated, it's able to return itself.
    ValueExpression<T> replicate();

    void replaceSuper(ValueExpression<T> superValue);

    default boolean isSuper() {
        return false;
    };

    <U> ValueExpression<U> map(Function<T, U> mapFunction);

    static <T> ValueExpression<Number> parseNumber(DynamicOps<T> ops, T input) {

        var numberValue = ops.getNumberValue(input);
        if (numberValue.result().isPresent()) {
            return new SimpleValue<>(numberValue.result().get().floatValue());
        }

        var booleanValue = ops.getBooleanValue(input);
        if (booleanValue.result().isPresent()) {
            return new SimpleValue<>(booleanValue.result().get() ? 1 : 0);
        }

        var stringValue = ops.getStringValue(input);
        if (stringValue.result().isPresent()) {
            String fetcherIdUnparsed = stringValue.result().get();

            if (fetcherIdUnparsed.equals("super")) {
                return new SuperValue<>();
            }

            Identifier fetcherId;
            if (fetcherIdUnparsed.indexOf(':') >= 0) {
                fetcherId = Identifier.parse(fetcherIdUnparsed);
            } else {
                fetcherId = Identifier.parse(UnloadedActivity.MOD_ID+":"+fetcherIdUnparsed);
            }

            Optional<ValueExpression<Number>> resolvedFetcher = UnloadedActivity.numberFetcherRegistry.resolve(fetcherId);

            if (resolvedFetcher.isPresent()) {
                return resolvedFetcher.get();
            }

            throw new RuntimeException(fetcherId + " is not a valid number fetcher.");
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


            ArrayList<Pair<Long, ValueExpression<Number>>> list = new ArrayList<>();
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

    static <T> ValueExpression<String> parseString(DynamicOps<T> ops, T input) {

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


            ArrayList<Pair<Long, ValueExpression<String>>> list = new ArrayList<>();
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