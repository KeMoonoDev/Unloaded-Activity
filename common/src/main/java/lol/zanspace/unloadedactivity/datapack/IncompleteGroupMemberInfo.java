package lol.zanspace.unloadedactivity.datapack;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;

import java.util.Optional;

import static lol.zanspace.unloadedactivity.datapack.IncompleteSimulationData.returnError;

public class IncompleteGroupMemberInfo {
    public Optional<Float> value;

    public void merge(IncompleteGroupMemberInfo otherGroupMemberInfo) {
        this.value = otherGroupMemberInfo.value.or(() -> this.value);
    }

    static public <T> DataResult<IncompleteGroupMemberInfo> parse(DynamicOps<T> ops, T input) {
        IncompleteGroupMemberInfo groupMemberInfo = new IncompleteGroupMemberInfo();

        DataResult<Number> number = ops.getNumberValue(input);
        if (number.result().isPresent()) {
            groupMemberInfo.value = Optional.of(number.result().get().floatValue());
            return DataResult.success(groupMemberInfo);
        }

        DataResult<MapLike<T>> mapResult = ops.getMap(input);
        if (mapResult.result().isEmpty())
            return DataResult.error("Group member must be a Number or an Object.");

        MapLike<T> map = mapResult.result().get();

        {
            T mapValue = map.get("value");
            DataResult<Number> valueResult = ops.getNumberValue(mapValue);
            if (valueResult.result().isEmpty())
                return returnError(valueResult);

            groupMemberInfo.value = valueResult.result().map(Number::floatValue);
        }

        return DataResult.success(groupMemberInfo);
    }
}
