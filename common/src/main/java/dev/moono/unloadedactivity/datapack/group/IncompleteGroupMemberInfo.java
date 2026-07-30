package dev.moono.unloadedactivity.datapack.group;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import dev.moono.unloadedactivity.api.condition.Condition;
import dev.moono.unloadedactivity.api.condition.FixedCondition;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class IncompleteGroupMemberInfo {
    public @Nullable Float value;
    public final ArrayList<Vec3i> ignoredOffsets = new ArrayList<>();
    public final ArrayList<FixedCondition> conditions = new ArrayList<>();

    public void merge(IncompleteGroupMemberInfo otherGroupMemberInfo) {
        if (otherGroupMemberInfo.value != null) this.value = otherGroupMemberInfo.value;
        this.ignoredOffsets.addAll(otherGroupMemberInfo.ignoredOffsets);
        this.conditions.addAll(otherGroupMemberInfo.conditions);
    }

    static public IncompleteGroupMemberInfo parse(JsonElement input) {
        IncompleteGroupMemberInfo groupMemberInfo = new IncompleteGroupMemberInfo();

        if (input.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = input.getAsJsonPrimitive();
            if (jsonPrimitive.isNumber()) {
                groupMemberInfo.value = jsonPrimitive.getAsFloat();
                return groupMemberInfo;
            }
        }

        if (!input.isJsonObject())
            throw new RuntimeException("Group member must be a Number or an Object.");

        JsonObject jsonObject = input.getAsJsonObject();


        JsonElement valueUnparsed = jsonObject.get("value");
        if (valueUnparsed == null)
            throw new RuntimeException("\"value\" must be defined.");
        groupMemberInfo.value = valueUnparsed.getAsFloat();


        JsonElement conditionsUnparsed = jsonObject.get("conditions");
        if (conditionsUnparsed != null) {
            JsonArray unparsedConditions = conditionsUnparsed.getAsJsonArray();

            for (JsonElement unparsedCondition : unparsedConditions) {
                Condition condition = Condition.parse(unparsedCondition);
                groupMemberInfo.conditions.add(new FixedCondition(condition));
            }
        }

        JsonElement ignoredOffsetsUnparsed = jsonObject.get("ignored_offsets");
        if (ignoredOffsetsUnparsed != null) {
            var unparsedIgnoredOffsets = ignoredOffsetsUnparsed.getAsJsonArray();

            for (JsonElement unparsedIgnoredOffset : unparsedIgnoredOffsets) {
                var result = Vec3i.CODEC.decode(JsonOps.INSTANCE, unparsedIgnoredOffset);
                if (result.error().isPresent()) {
                    throw new RuntimeException(result.error().get().message());
                }

                groupMemberInfo.ignoredOffsets.add(result.result().get().getFirst());
            }
        }

        return groupMemberInfo;
    }
}
