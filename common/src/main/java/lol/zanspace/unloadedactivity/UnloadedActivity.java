package lol.zanspace.unloadedactivity;

#if MC_VER >= MC_1_21_11
import lol.zanspace.unloadedactivity.api.SimulationMethodRegistry;
import lol.zanspace.unloadedactivity.api.UnloadedActivityApi;
import lol.zanspace.unloadedactivity.datapack.SimulationDataResource;
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import lol.zanspace.unloadedactivity.config.BlockOrTag;
import lol.zanspace.unloadedactivity.config.UnloadedActivityConfig;
import lol.zanspace.unloadedactivity.api.NumberFetcherRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lol.zanspace.unloadedactivity.platform.IPlatformHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.ServiceLoader;

public class UnloadedActivity {
    public static final String MOD_ID = "unloadedactivity";
    public static final String OLD_MOD_ID = "unloaded_activity";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final long chunkSimVer = 2;
    public static UnloadedActivityConfig config;
    public static IPlatformHelper platform;

    public static NumberFetcherRegistry numberFetcherRegistry = new NumberFetcherRegistry();
    public static SimulationMethodRegistry simulationMethodRegistry = new SimulationMethodRegistry();

    public static void init(IPlatformHelper platformHelper) {
        platform = platformHelper;
        loadConfig();
        loadRegistries();
        LOGGER.info("Bleeghhh...");
    }

    public static void loadRegistries() {
        ServiceLoader<UnloadedActivityApi> loader =
                ServiceLoader.load(UnloadedActivityApi.class);

        for (UnloadedActivityApi entrypoint : loader) {
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
        #if MC_VER >= MC_1_21_11
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
        #elif MC_VER >= MC_1_21_1
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        #else
        return new ResourceLocation(MOD_ID, path);
        #endif
    }

    public static void addChunkToQueue(MinecraftServer server, LevelChunk chunk) {
        server.addChunkToQueue(chunk);
    }

    public static void dataPackReloaded(boolean success) {
        if (success) SimulationDataResource.buildSimulationDatas();
        SimulationDataResource.clearRawSimulationDatas();
    }
}
