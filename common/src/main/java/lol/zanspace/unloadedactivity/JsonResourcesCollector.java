package lol.zanspace.unloadedactivity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class JsonResourcesCollector extends SimplePreparableReloadListener<Map<Identifier, List<JsonObject>>> {
    private final FileToIdConverter lister;

    protected JsonResourcesCollector(final FileToIdConverter lister) {
        this.lister = lister;
    }

    protected Map<Identifier, List<JsonObject>> prepare(final ResourceManager manager, final ProfilerFiller profiler) {
        Map<Identifier, List<JsonObject>> result = new HashMap<>();

        for(Map.Entry<Identifier, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier id = lister.fileToId(location);

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement parsed = StrictJsonParser.parse(reader);
                if (!parsed.isJsonObject()) {
                    UnloadedActivity.LOGGER.error("Data file '{}' from '{}' didn't return a JsonObject. It will be ignored.", id, location);
                }

                JsonObject jsonObject = parsed.getAsJsonObject();

                JsonElement priority = jsonObject.get("priority");

                if (priority != null && (!priority.isJsonPrimitive() || !priority.getAsJsonPrimitive().isNumber())) {
                    UnloadedActivity.LOGGER.error("Data file '{}' from '{}' defines the field \"priority\" as something that is not a number or null. It will be defaulted to 1000.", id, location);
                }

                result.computeIfAbsent(id, ignored -> new ArrayList<>()).add(parsed.getAsJsonObject());
            } catch (IllegalArgumentException | IOException | JsonParseException e) {
                UnloadedActivity.LOGGER.error("Couldn't parse data file '{}' from '{}'", id, location, e);
            }
        }

        return result;
    }
}
