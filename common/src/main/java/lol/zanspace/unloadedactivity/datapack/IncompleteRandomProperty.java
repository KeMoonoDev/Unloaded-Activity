package lol.zanspace.unloadedactivity.datapack;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;

import java.util.Optional;

import static lol.zanspace.unloadedactivity.datapack.IncompleteSimulationData.returnError;

public class IncompleteRandomProperty {
    Optional<PropertyType> propertyType = Optional.empty();
    Optional<Float> probability = Optional.empty();
    Optional<Integer> successValue = Optional.empty();
    Optional<Integer> failValue = Optional.empty();

    public void merge(IncompleteRandomProperty otherRandomProperty) {
        this.propertyType = otherRandomProperty.propertyType.or(() -> this.propertyType);
        this.probability = otherRandomProperty.probability.or(() -> this.probability);
        this.successValue = otherRandomProperty.successValue.or(() -> this.successValue);
        this.failValue = otherRandomProperty.failValue.or(() -> this.failValue);
    }

    public static <T> DataResult<IncompleteRandomProperty> parse(DynamicOps<T> ops, T input) {
        var mapResult = ops.getMap(input);

        if (mapResult.result().isEmpty()) {
            return returnError(mapResult);
        }

        MapLike<T> map = mapResult.result().get();

        IncompleteRandomProperty randomProperty = new IncompleteRandomProperty();

        {
            T mapValue = map.get("property_type");
            if (mapValue != null) {
                DataResult<String> valueResult = ops.getStringValue(mapValue);
                if (valueResult.result().isEmpty())
                    return returnError(valueResult);

                Optional<PropertyType> propertyType = PropertyType.fromString(valueResult.result().get());

                if (propertyType.isEmpty())
                    return returnError(valueResult.result().get() + " is not a valid property type.");

                randomProperty.propertyType = propertyType;
            }
        }

        {
            T mapValue = map.get("probability");
            if (mapValue != null) {
                DataResult<Number> valueResult = ops.getNumberValue(mapValue);
                if (valueResult.result().isEmpty())
                    return returnError(valueResult);

                randomProperty.probability = Optional.of(valueResult.result().get().floatValue());
            }
        }

        {
            T mapValue = map.get("success");
            if (mapValue != null) {
                DataResult<Number> valueResult = ops.getNumberValue(mapValue);
                if (valueResult.result().isEmpty()) {
                    DataResult<Boolean> boolValueResult = ops.getBooleanValue(mapValue);

                    if (boolValueResult.result().isEmpty())
                        return returnError(valueResult);

                    randomProperty.successValue = Optional.of(boolValueResult.result().get() ? 1 : 0);
                } else {
                    randomProperty.successValue = Optional.of(valueResult.result().get().intValue());
                }
            }
        }

        {
            T mapValue = map.get("fail");
            if (mapValue != null) {
                DataResult<Number> valueResult = ops.getNumberValue(mapValue);
                if (valueResult.result().isEmpty()) {
                    DataResult<Boolean> boolValueResult = ops.getBooleanValue(mapValue);

                    if (boolValueResult.result().isEmpty())
                        return returnError(valueResult);

                    randomProperty.failValue = Optional.of(boolValueResult.result().get() ? 1 : 0);
                } else {
                    randomProperty.failValue = Optional.of(valueResult.result().get().intValue());
                }
            }
        }

        return DataResult.success(randomProperty);
    }
}
