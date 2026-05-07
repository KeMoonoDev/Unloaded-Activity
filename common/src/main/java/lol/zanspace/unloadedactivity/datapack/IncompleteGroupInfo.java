package lol.zanspace.unloadedactivity.datapack;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import net.minecraft.core.Vec3i;

import java.util.*;
import java.util.stream.Stream;

import static lol.zanspace.unloadedactivity.datapack.IncompleteSimulationData.returnError;

public class IncompleteGroupInfo {
    public static final Codec<IncompleteGroupInfo> CODEC;

    public Optional<LookupShape> shape;
    public Optional<Integer> width;
    public Optional<Integer> height;
    public HashMap<Pair<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, Boolean>, IncompleteGroupMemberInfo> values = new HashMap<>();

    public IncompleteGroupInfo replicate() {
        IncompleteGroupInfo newGroupSimulateInfo = new IncompleteGroupInfo();
        newGroupSimulateInfo.merge(this);
        return newGroupSimulateInfo;
    }

    public void merge(IncompleteGroupInfo otherGroupInfo) {
        this.shape = otherGroupInfo.shape.or(() -> this.shape);
        this.width = otherGroupInfo.width.or(() -> this.width);
        this.height = otherGroupInfo.height.or(() -> this.height);

        for (var entry : otherGroupInfo.values.entrySet()) {
            IncompleteGroupMemberInfo thisGroupMemberInfo = this.values.computeIfAbsent(entry.getKey(), k -> new IncompleteGroupMemberInfo());
            IncompleteGroupMemberInfo otherGroupMemberInfo = entry.getValue();

            thisGroupMemberInfo.merge(otherGroupMemberInfo);
        }
    }

    public static <T> DataResult<IncompleteGroupInfo> parse(DynamicOps<T> ops, T input) {
        var mapResult = ops.getMap(input);

        if (mapResult.result().isEmpty()) {
            return returnError(mapResult);
        }

        MapLike<T> map = mapResult.result().get();

        IncompleteGroupInfo groupInfo = new IncompleteGroupInfo();

        {
            T mapValue = map.get("shape");
            DataResult<String> valueResult = ops.getStringValue(mapValue);
            if (valueResult.result().isEmpty())
                return returnError(valueResult);

            String shapeName = valueResult.result().get();

            Optional<LookupShape> shape = LookupShape.fromString(shapeName);

            if (shape.isEmpty())
                return returnError(shapeName + " is not a valid shape.");

            groupInfo.shape = shape;
        }

        {
            T mapValue = map.get("width");
            DataResult<Number> valueResult = ops.getNumberValue(mapValue);
            if (valueResult.result().isEmpty())
                return returnError(valueResult);

            groupInfo.width = valueResult.result().map(Number::intValue);
        }

        {
            T mapValue = map.get("height");
            DataResult<Number> valueResult = ops.getNumberValue(mapValue);
            if (valueResult.result().isEmpty())
                return returnError(valueResult);

            groupInfo.height = valueResult.result().map(Number::intValue);
        }

        {
            T mapValue = map.get("values");

            DataResult<MapLike<T>> valueMapResult = ops.getMap(mapValue);
            if (valueMapResult.result().isEmpty())
                return returnError(valueMapResult);

            for (Iterator<Pair<T, T>> it = valueMapResult.result().get().entries().iterator(); it.hasNext(); ) {
                Pair<T, T> entry = it.next();
                T idValue = entry.getFirst();
                DataResult<String> idStringResult = ops.getStringValue(idValue);

                if (idStringResult.result().isEmpty())
                    return returnError(idStringResult);

                String idStringValue = idStringResult.result().get();

                Boolean isTag = idStringValue.startsWith("#");

                if (isTag)
                    idStringValue = idStringValue.substring(1);

                var idResult = #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif.read(idStringValue);

                if (idResult.result().isEmpty())
                    return returnError(idResult);

                var id = idResult.result().get();

                T memberValue = entry.getSecond();

                DataResult<IncompleteGroupMemberInfo> groupMemberInfoResult = IncompleteGroupMemberInfo.parse(ops, memberValue);

                if (groupMemberInfoResult.result().isEmpty())
                    return returnError(groupMemberInfoResult);

                IncompleteGroupMemberInfo groupMemberInfo = groupMemberInfoResult.result().get();

                groupInfo.values.put(Pair.of(id, isTag), groupMemberInfo);
            }
        }

        return DataResult.success(groupInfo);
    }

    static {
        CODEC = new Codec<>() {
            @Override
            public <T> DataResult<T> encode(IncompleteGroupInfo incompleteGroupInfo, DynamicOps<T> dynamicOps, T t) {
                throw new UnsupportedOperationException("I am never using this. Therefore, it does not need to be implemented.");
            }

            @Override
            public <T> DataResult<Pair<IncompleteGroupInfo, T>> decode(DynamicOps<T> dynamicOps, T t) {
                return IncompleteGroupInfo.parse(dynamicOps, t).map(incompleteGroupInfo -> Pair.of(incompleteGroupInfo, t));
            }
        };
    }
}
