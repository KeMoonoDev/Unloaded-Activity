package dev.moono.unloadedactivity;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import dev.moono.unloadedactivity.api.SimulationMethodRegistry;
import dev.moono.unloadedactivity.api.UnloadedActivityApi;
import dev.moono.unloadedactivity.datapack.group.GroupInfoResource;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationDataResource;
import dev.moono.unloadedactivity.config.BlockOrTag;
import dev.moono.unloadedactivity.config.UnloadedActivityConfig;
import dev.moono.unloadedactivity.api.NumberFetcherRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.moono.unloadedactivity.platform.IPlatformHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ServiceLoader;

public class UnloadedActivity {
    public static final String MOD_ID = "unloadedactivity";
    public static final String OLD_MOD_ID = "unloaded_activity";
    public static final String DISABLE_COMPAT = "disable_compat";
    public static final String OPTIONS_KEY = UnloadedActivity.id("options").toString();

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final long chunkSimVer = 2;
    public static UnloadedActivityConfig config;
    public static IPlatformHelper platform;
    public static List<String> disabledCompats;

    public static final NumberFetcherRegistry numberFetcherRegistry = new NumberFetcherRegistry();
    public static final SimulationMethodRegistry simulationMethodRegistry = new SimulationMethodRegistry();

    public static void init(IPlatformHelper platformHelper, List<String> providedDisabledCompats) {
        init(platformHelper, providedDisabledCompats, List.of());
    }

    public static void init(IPlatformHelper platformHelper, List<String> providedDisabledCompats, List<UnloadedActivityApi> extraEntryPoints) {
        platform = platformHelper;
        disabledCompats = providedDisabledCompats;
        loadConfig();
        loadRegistries(extraEntryPoints);
        LOGGER.info("Bleeghhh...");
        LOGGER.info("Disabled built in compatibility: " + disabledCompats.toString());
    }

    public static void loadRegistries(List<UnloadedActivityApi> extraEntryPoints) {
        ServiceLoader<UnloadedActivityApi> loader =
                ServiceLoader.load(UnloadedActivityApi.class);

        for (UnloadedActivityApi entrypoint : loader) {
            entrypoint.registerNumberFetchers(numberFetcherRegistry);
            entrypoint.registerSimulationMethods(simulationMethodRegistry);
        }

        for (UnloadedActivityApi entrypoint : extraEntryPoints) {
            entrypoint.registerNumberFetchers(numberFetcherRegistry);
            entrypoint.registerSimulationMethods(simulationMethodRegistry);
        }

    }

    public static void loadConfig() {
        LOGGER.info("Loading config.");
        File configFile = new File(platform.getConfigDirectory().toFile(), MOD_ID+".json");
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(BlockOrTag.class, new BlockOrTag.StringAdapter())
            .create();
        if (configFile.exists()) {
            try {
                FileReader fileReader = new FileReader(configFile);
                config = gson.fromJson(fileReader, UnloadedActivityConfig.class);
                fileReader.close();
            } catch (IOException e) {
                LOGGER.warn("Error loading UnloadedActivity configs: " + e.getLocalizedMessage());
            }
        }

        if (config == null) {
            config = new UnloadedActivityConfig();
            saveConfig();
        }
    }

    public static void saveConfig() {
        File configFile = new File(platform.getConfigDirectory().toFile(), MOD_ID+".json");
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(BlockOrTag.class, new BlockOrTag.StringAdapter())
            .create();

        if (!configFile.getParentFile().exists())
            configFile.getParentFile().mkdir();

        try {
            FileWriter fileWriter = new FileWriter(configFile);
            gson.toJson(config, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            LOGGER.warn("Error saving UnloadedActivity configs: " + e.getLocalizedMessage());
        }
    }

    public static #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id(String path) {
        return GameUtils.createId(MOD_ID, path);
    }

    public static #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif parseId(String path) {
        if (path.indexOf(':') >= 0) {
            return GameUtils.parseId(path);
        } else {
            return GameUtils.parseId(UnloadedActivity.MOD_ID+":"+path);
        }
    }

    public static boolean shouldDoCompat(String modId) {
        return platform.isModLoaded(modId) && !disabledCompats.contains(modId);
    }

    public static void addChunkToQueue(MinecraftServer server, LevelChunk chunk) {
        server.addChunkToQueue(chunk);
    }

    public static void dataPackReloaded(boolean success) {
        if (success) {
            SimulationDataResource.buildAllSimulationData();
            GroupInfoResource.buildAllGroupInfos();
        }
        SimulationDataResource.clearAllRawSimulationData();
        GroupInfoResource.clearAllRawGroupInfos();
    }
}
