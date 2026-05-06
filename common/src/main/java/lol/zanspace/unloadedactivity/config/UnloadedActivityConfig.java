package lol.zanspace.unloadedactivity.config;

import com.mojang.brigadier.arguments.*;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

public class UnloadedActivityConfig {

    public static class ConfigOption<T> {
        public ArgumentType<T> argumentType;
        public T defaultValue;
        public String name;
        public Function<Void, T> getter;
        public Consumer<T> setter;
        public Class<T> tClass;

        public ConfigOption(ArgumentType<T> argumentType, String name, T defaultValue, Function<Void, T> getter, Consumer<T> setter, Class<T> tClass) {
            this.argumentType = argumentType;
            this.name = name;
            this.defaultValue = defaultValue;
            this.getter = getter;
            this.setter = setter;
            this.tClass = tClass;
        }
    }

    public transient ArrayList<ConfigOption<?>> configOptions = new ArrayList<>();

    public void registerInt(String name, int defaultValue, int minValue, int maxValue, Function<Void, Integer> getter, Consumer<Integer> setter) {
        IntegerArgumentType argumentType = IntegerArgumentType.integer(minValue, maxValue);
        ConfigOption<Integer> configOption = new ConfigOption<>(argumentType, name, defaultValue, getter, setter, int.class);
        configOptions.add(configOption);
    }

    public void registerFloat(String name, float defaultValue, float minValue, float maxValue, Function<Void, Float> getter, Consumer<Float> setter) {
        FloatArgumentType argumentType = FloatArgumentType.floatArg(minValue, maxValue);
        ConfigOption<Float> configOption = new ConfigOption<>(argumentType, name, defaultValue, getter, setter, float.class);
        configOptions.add(configOption);
    }

    public void registerBoolean(String name, boolean defaultValue, Function<Void, Boolean> getter, Consumer<Boolean> setter) {
        BoolArgumentType argumentType = BoolArgumentType.bool();
        ConfigOption<Boolean> configOption = new ConfigOption<>(argumentType, name, defaultValue, getter, setter, boolean.class);
        configOptions.add(configOption);
    }

    public UnloadedActivityConfig() {
        registerInt(
                "tickDifferenceThreshold", tickDifferenceThreshold, 1, Integer.MAX_VALUE,
                unused -> tickDifferenceThreshold,
                value -> tickDifferenceThreshold = value
        );

        registerInt(
                "maxNegativeBinomialAttempts", maxNegativeBinomialAttempts, 1, Integer.MAX_VALUE,
                unused -> maxNegativeBinomialAttempts,
                value -> maxNegativeBinomialAttempts = value
        );

        registerInt(
                "groupTickDifferenceThreshold", groupTickDifferenceThreshold, 1, Integer.MAX_VALUE,
                unused -> groupTickDifferenceThreshold,
                value -> groupTickDifferenceThreshold = value
        );

        registerFloat(
                "maxGroupTickDeviationScale", maxGroupTickDeviationScale, 0F, 1F,
                unused -> maxGroupTickDeviationScale,
                value -> maxGroupTickDeviationScale = value
        );

        registerInt(
                "maxForcedChunkLoads", maxForcedChunkLoads, 0, Integer.MAX_VALUE,
                unused -> maxForcedChunkLoads,
                value -> maxForcedChunkLoads = value
        );

        registerInt(
                "maxGroupTickUpdates", maxGroupTickUpdates, 0, Integer.MAX_VALUE,
                unused -> maxGroupTickUpdates,
                value -> maxGroupTickUpdates = value
        );

        registerInt(
                "maxGroupTickIterations", maxGroupTickIterations, 1, Integer.MAX_VALUE,
                unused -> maxGroupTickIterations,
                value -> maxGroupTickIterations = value
        );

        registerFloat(
                "groupTickUpdateStrength", groupTickUpdateStrength, 0F, 100F,
                unused -> groupTickUpdateStrength,
                value -> groupTickUpdateStrength = value
        );

        registerInt(
                "maxGroupTickSize", maxGroupTickSize, 1, Integer.MAX_VALUE,
                unused -> maxGroupTickSize,
                value -> maxGroupTickSize = value
        );

        registerBoolean(
                "debugLogs", debugLogs,
                unused -> debugLogs,
                value -> debugLogs = value
        );

        registerBoolean(
                "convertCCAData", convertCCAData,
                unused -> convertCCAData,
                value -> convertCCAData = value
        );

        registerInt(
                "maxChunksIndexedPerTick", maxChunksIndexedPerTick, 1, 32767,
                unused -> maxChunksIndexedPerTick,
                value -> maxChunksIndexedPerTick = value
        );

        registerInt(
                "maxChunkUpdatesPerTick", maxChunkUpdatesPerTick, 1, 32767,
                unused -> maxChunkUpdatesPerTick,
                value -> maxChunkUpdatesPerTick = value
        );

        registerBoolean(
                "randomizeBlockUpdates", randomizeBlockUpdates,
                unused -> randomizeBlockUpdates,
                value -> randomizeBlockUpdates = value
        );

        registerBoolean(
                "multiplyMaxChunkUpdatesPerPlayer", multiplyMaxChunkUpdatesPerPlayer,
                unused -> multiplyMaxChunkUpdatesPerPlayer,
                value -> multiplyMaxChunkUpdatesPerPlayer = value
        );

        registerBoolean(
                "multiplyMaxChunksIndexedPerPlayer", multiplyMaxChunksIndexedPerPlayer,
                unused -> multiplyMaxChunksIndexedPerPlayer,
                value -> multiplyMaxChunksIndexedPerPlayer = value
        );

        registerBoolean(
                "enableSimulatingRandomTicks", enableSimulatingRandomTicks,
                unused -> enableSimulatingRandomTicks,
                value -> enableSimulatingRandomTicks = value
        );

        registerBoolean(
                "enableSimulatingPrecipitationTicks", enableSimulatingPrecipitationTicks,
                unused -> enableSimulatingPrecipitationTicks,
                value -> enableSimulatingPrecipitationTicks = value
        );

                registerBoolean(
                "enableSimulatingGroups", enableSimulatingGroups,
                unused -> enableSimulatingGroups,
                value -> enableSimulatingGroups = value
        );

        registerBoolean(
                "enableSimulatingBlockEntities", enableSimulatingBlockEntities,
                unused -> enableSimulatingBlockEntities,
                value -> enableSimulatingBlockEntities = value
        );

        registerBoolean(
                "enableSimulatingEntities", enableSimulatingEntities,
                unused -> enableSimulatingEntities,
                value -> enableSimulatingEntities = value
        );

        registerBoolean(
                "simulateFurnaceSmelting", simulateFurnaceSmelting,
                unused -> simulateFurnaceSmelting,
                value -> simulateFurnaceSmelting = value
        );

        registerBoolean(
                "simulateEntitiesAgeing", simulateEntitiesAgeing,
                unused -> simulateEntitiesAgeing,
                value -> simulateEntitiesAgeing = value
        );
    }


    //General
    public int tickDifferenceThreshold = 100;
    public int maxNegativeBinomialAttempts = 20;
    public boolean debugLogs = false;
    public boolean convertCCAData = true;

    //Group
    public int groupTickDifferenceThreshold = 1000;
    public float maxGroupTickDeviationScale = 0.1F;
    public int maxForcedChunkLoads = 8;
    public int maxGroupTickUpdates = 1;
    public int maxGroupTickIterations = 1000;
    public float groupTickUpdateStrength = 1F;
    public int maxGroupTickSize = 10000;

    //Chunk
    public int maxChunksIndexedPerTick = 8;
    public int maxChunkUpdatesPerTick = 64;
    public boolean randomizeBlockUpdates = false;
    public boolean multiplyMaxChunkUpdatesPerPlayer = false;
    public boolean multiplyMaxChunksIndexedPerPlayer = false;

    //Enable
    public boolean enableSimulatingRandomTicks = true;
    public boolean enableSimulatingPrecipitationTicks = true;
    public boolean enableSimulatingGroups = true;
    public boolean enableSimulatingBlockEntities = true;
    public boolean enableSimulatingEntities = true;

    //Block Entities
    public boolean simulateFurnaceSmelting = true;

    //Entities
    public boolean simulateEntitiesAgeing = true;
}
