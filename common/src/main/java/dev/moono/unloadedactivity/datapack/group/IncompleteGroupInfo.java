package dev.moono.unloadedactivity.datapack.group;


import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import dev.moono.unloadedactivity.impl.LookupShape;
import net.minecraft.resources.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static dev.moono.unloadedactivity.GameUtils.returnError;

public class IncompleteGroupInfo {
    public static final Codec<IncompleteGroupInfo> CODEC;

    public @Nullable LookupShape shape;
    public @Nullable Integer width;
    public @Nullable Integer height;
    public @Nullable Float groupSizePenalty;
    public final HashMap<Pair<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, Boolean>, IncompleteGroupMemberInfo> values = new HashMap<>();

    public void merge(IncompleteGroupInfo otherGroupInfo) {
        if (otherGroupInfo.shape != null) this.shape = otherGroupInfo.shape;
        if (otherGroupInfo.width != null) this.width = otherGroupInfo.width;
        if (otherGroupInfo.height != null) this.height = otherGroupInfo.height;
        if (otherGroupInfo.groupSizePenalty != null) this.groupSizePenalty = otherGroupInfo.groupSizePenalty;

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

            groupInfo.shape = shape.get();
        }

        {
            T mapValue = map.get("width");
            if (mapValue != null) {
                DataResult<Number> valueResult = ops.getNumberValue(mapValue);
                if (valueResult.result().isEmpty())
                    return returnError(valueResult);

                groupInfo.width = valueResult.result().get().intValue();
            }
        }

        {
            T mapValue = map.get("height");
            if (mapValue != null) {
                DataResult<Number> valueResult = ops.getNumberValue(mapValue);
                if (valueResult.result().isEmpty())
                    return returnError(valueResult);

                groupInfo.height = valueResult.result().get().intValue();
            }
        }

        {
            T mapValue = map.get("group_size_penalty");
            if (mapValue != null) {
                DataResult<Number> valueResult = ops.getNumberValue(mapValue);
                if (valueResult.result().isEmpty())
                    return returnError(valueResult);

                groupInfo.groupSizePenalty = valueResult.result().get().floatValue();
            }
        }

        {
            T mapValue = map.get("values");

            if (mapValue != null) {
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

                    var idResult = #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif .read(idStringValue);

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
