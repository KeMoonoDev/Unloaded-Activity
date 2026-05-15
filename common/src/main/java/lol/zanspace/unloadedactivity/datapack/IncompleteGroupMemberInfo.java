package lol.zanspace.unloadedactivity.datapack;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static lol.zanspace.unloadedactivity.datapack.IncompleteSimulationData.returnError;

public class IncompleteGroupMemberInfo {
    public Optional<Float> value = Optional.empty();
    public ArrayList<Vec3i> ignoredOffsets = new ArrayList<>();

    public void merge(IncompleteGroupMemberInfo otherGroupMemberInfo) {
        this.value = otherGroupMemberInfo.value.or(() -> this.value);
        this.ignoredOffsets.addAll(otherGroupMemberInfo.ignoredOffsets);
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
            #if MC_VER >= MC_1_19_4
            return DataResult.error(() -> "Group member must be a Number or an Object.");
            #else
            return DataResult.error("Group member must be a Number or an Object.");
            #endif

        MapLike<T> map = mapResult.result().get();

        {
            T mapValue = map.get("value");
            DataResult<Number> valueResult = ops.getNumberValue(mapValue);
            if (valueResult.result().isEmpty())
                return returnError(valueResult);

            groupMemberInfo.value = valueResult.result().map(Number::floatValue);
        }

        {
            T mapValue = map.get("ignored_offsets");
            if (mapValue != null) {
                var listResult = ops.getStream(mapValue);
                if (listResult.error().isPresent()) {
                    return returnError(listResult);
                }

                for (T ignoredOffset : listResult.result().get().toList()) {
                    var result = Vec3i.CODEC.decode(ops, ignoredOffset);
                    if (result.result().isEmpty()) {
                        return returnError(result);
                    }

                    groupMemberInfo.ignoredOffsets.add(result.result().get().getFirst());
                }
            }

        }

        return DataResult.success(groupMemberInfo);
    }
}
