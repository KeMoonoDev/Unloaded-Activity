package dev.moono.unloadedactivity.config;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Objects;

public class BlockOrTag {
    public final boolean isTag;
    public final #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id;

    public BlockOrTag(boolean isTag, #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id) {
        this.isTag = isTag;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return o instanceof BlockOrTag blockOrTag && this.isTag == blockOrTag.isTag && this.id.equals(blockOrTag.id);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(isTag, id);
    }

    @Override
    public String toString() {
        return this.isTag ? "#" + this.id.toString() : this.id.toString();
    }

    public static class StringAdapter implements JsonDeserializer<BlockOrTag>, JsonSerializer<BlockOrTag> {

        @Override
        public BlockOrTag deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            String parsedString = jsonDeserializationContext.deserialize(jsonElement, String.class);
            boolean isTag = parsedString.startsWith("#");

            String stringToParse;
            if (isTag) {
                stringToParse = parsedString.substring(1);
            } else {
                stringToParse = parsedString;
            }

            var result = #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif.read(stringToParse);
            if (result.result().isEmpty()) {
                throw new JsonParseException(result.error().get().message());
            }

            return new BlockOrTag(isTag, result.result().get());
        }

        @Override
        public JsonElement serialize(BlockOrTag blockOrTag, Type type, JsonSerializationContext jsonSerializationContext) {
            return jsonSerializationContext.serialize(blockOrTag.toString());
        }
    }
}
