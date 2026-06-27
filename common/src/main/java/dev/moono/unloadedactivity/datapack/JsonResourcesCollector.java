package dev.moono.unloadedactivity.datapack;

import com.google.gson.JsonParser;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.UnloadedActivity;
import net.minecraft.resources.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class JsonResourcesCollector extends SimplePreparableReloadListener<Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<JsonObject>>> {
    #if MC_VER >= MC_1_21_4
    private final FileToIdConverter lister;

    protected JsonResourcesCollector(final FileToIdConverter lister) {
        this.lister = lister;
    }
    #else
    private final String directory;

    protected JsonResourcesCollector(final String directory) {
        this.directory = directory;
    }
    #endif

    protected Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<JsonObject>> prepare(final ResourceManager manager, final ProfilerFiller profiler) {
        Map<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, List<JsonObject>> result = new HashMap<>();
        #if MC_VER >= MC_1_21_4
        for(var entry : lister.listMatchingResourceStacks(manager).entrySet()) {
            var location = entry.getKey();
            var id = lister.fileToId(location);
        #else
        int pathPrefixLength = this.directory.length() + 1;
        int pathSuffixLength = ".json".length();
        for(var entry : manager.listResourceStacks(this.directory, location -> location.getPath().endsWith(".json")).entrySet()) {
            var location = entry.getKey();
            String path = location.getPath();
            var id = GameUtils.parseId(location.getNamespace() + ":" + path.substring(pathPrefixLength, path.length() - pathSuffixLength));
        #endif
            for (var resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement parsed = JsonParser.parseReader(reader);
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
        }

        return result;
    }
}
