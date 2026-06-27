package dev.moono.unloadedactivity.datapack.group;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import dev.moono.unloadedactivity.api.condition.Condition;
import dev.moono.unloadedactivity.api.condition.FixedCondition;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

import static dev.moono.unloadedactivity.GameUtils.returnError;

public class IncompleteGroupMemberInfo {
    public @Nullable Float value;
    public final ArrayList<Vec3i> ignoredOffsets = new ArrayList<>();
    public final ArrayList<FixedCondition> conditions = new ArrayList<>();

    public void merge(IncompleteGroupMemberInfo otherGroupMemberInfo) {
        if (otherGroupMemberInfo.value != null) this.value = otherGroupMemberInfo.value;
        this.ignoredOffsets.addAll(otherGroupMemberInfo.ignoredOffsets);
        this.conditions.addAll(otherGroupMemberInfo.conditions);
    }

    static public <T> DataResult<IncompleteGroupMemberInfo> parse(DynamicOps<T> ops, T input) {
        IncompleteGroupMemberInfo groupMemberInfo = new IncompleteGroupMemberInfo();

        DataResult<Number> number = ops.getNumberValue(input);
        if (number.result().isPresent()) {
            groupMemberInfo.value = number.result().get().floatValue();
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

            groupMemberInfo.value = valueResult.result().get().floatValue();
        }

        {
            T mapValue = map.get("conditions");
            if (mapValue != null) {
                var listResult = ops.getStream(mapValue);
                if (listResult.error().isPresent()) {
                    return returnError(listResult);
                }

                for (T unparsedCondition : listResult.result().get().toList()) {
                    #if MC_VER >= MC_1_20_6
                    var result = Condition.parse(ops, unparsedCondition);
                    #else
                    var result = Condition.parse(ops, unparsedCondition);
                    #endif

                    if (result.error().isPresent()) {
                        return returnError(result);
                    }

                    Condition condition = result.result().get();

                    groupMemberInfo.conditions.add(new FixedCondition(condition));
                }
            }
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
