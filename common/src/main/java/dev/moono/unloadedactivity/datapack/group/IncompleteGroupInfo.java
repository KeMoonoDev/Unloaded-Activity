package dev.moono.unloadedactivity.datapack.group;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import dev.moono.unloadedactivity.impl.LookupShape;
import net.minecraft.resources.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class IncompleteGroupInfo {
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

    public static IncompleteGroupInfo parse(JsonObject input) {
        IncompleteGroupInfo groupInfo = new IncompleteGroupInfo();

        JsonElement shapeUnparsed = input.get("shape");

        if (shapeUnparsed != null) {
            String shapeName = input.get("shape").getAsString();

            Optional<LookupShape> shape = LookupShape.fromString(shapeName);

            if (shape.isEmpty())
                throw new RuntimeException(shapeName + " is not a valid shape.");

            groupInfo.shape = shape.get();
        }

        JsonElement widthUnparsed = input.get("width");
        if (widthUnparsed != null)
            groupInfo.width = widthUnparsed.getAsInt();

        JsonElement heightUnparsed = input.get("height");
        if (heightUnparsed != null)
            groupInfo.height = heightUnparsed.getAsInt();

        JsonElement penaltyUnparsed = input.get("group_size_penalty");
        if (penaltyUnparsed != null)
            groupInfo.groupSizePenalty = penaltyUnparsed.getAsFloat();

        JsonElement valuesUnparsed = input.get("values");
        if (valuesUnparsed != null) {
            JsonObject valuesMap = valuesUnparsed.getAsJsonObject();

            for (var entry : valuesMap.entrySet()) {
                String key = entry.getKey();

                Boolean isTag = key.startsWith("#");

                if (isTag)
                    key = key.substring(1);

                var idResult = #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif .read(key);

                if (idResult.error().isPresent())
                    throw new RuntimeException(idResult.error().get().message());

                var id = idResult.result().get();

                JsonElement memberValue = entry.getValue();

                IncompleteGroupMemberInfo groupMemberInfo = IncompleteGroupMemberInfo.parse(memberValue);

                groupInfo.values.put(Pair.of(id, isTag), groupMemberInfo);
            }
        }

        return groupInfo;
    }
}
