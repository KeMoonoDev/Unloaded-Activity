package lol.zanspace.unloadedactivity.config;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

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

    public void registerDouble(String name, double defaultValue, double minValue, double maxValue, Function<Void, Double> getter, Consumer<Double> setter) {
        DoubleArgumentType argumentType = DoubleArgumentType.doubleArg(minValue, maxValue);
        ConfigOption<Double> configOption = new ConfigOption<>(argumentType, name, defaultValue, getter, setter, double.class);
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
                "groupTickDifferenceThreshold", groupTickDifferenceThreshold, 1, Integer.MAX_VALUE,
                unused -> groupTickDifferenceThreshold,
                value -> groupTickDifferenceThreshold = value
        );

        registerDouble(
                "groupChunkDifferencePercentage", groupChunkDifferencePercentage, 0.0, 1.0,
                unused -> groupChunkDifferencePercentage,
                value -> groupChunkDifferencePercentage = value
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

        registerDouble(
                "groupTickUpdateStrength", groupTickUpdateStrength, 0.0, 100.0,
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
                "maxChunkUpdates", maxChunkUpdates, 1, 32767,
                unused -> maxChunkUpdates,
                value -> maxChunkUpdates = value
        );

        registerInt(
                "maxKnownChunkUpdates", maxKnownChunkUpdates, 1, 32767,
                unused -> maxKnownChunkUpdates,
                value -> maxKnownChunkUpdates = value
        );

        registerBoolean(
                "randomizeBlockUpdates", randomizeBlockUpdates,
                unused -> randomizeBlockUpdates,
                value -> randomizeBlockUpdates = value
        );

        registerBoolean(
                "rememberBlockPositions", rememberBlockPositions,
                unused -> rememberBlockPositions,
                value -> rememberBlockPositions = value
        );

        registerBoolean(
                "multiplyMaxChunkUpdatesPerPlayer", multiplyMaxChunkUpdatesPerPlayer,
                unused -> multiplyMaxChunkUpdatesPerPlayer,
                value -> multiplyMaxChunkUpdatesPerPlayer = value
        );

        registerBoolean(
                "updateAllChunksWhenSleep", updateAllChunksWhenSleep,
                unused -> updateAllChunksWhenSleep,
                value -> updateAllChunksWhenSleep = value
        );

        registerBoolean(
                "enableRandomTicks", enableRandomTicks,
                unused -> enableRandomTicks,
                value -> enableRandomTicks = value
        );

        registerBoolean(
                "enablePrecipitationTicks", enablePrecipitationTicks,
                unused -> enablePrecipitationTicks,
                value -> enablePrecipitationTicks = value
        );

        registerBoolean(
                "growSaplings", growSaplings,
                unused -> growSaplings,
                value -> growSaplings = value
        );

        registerBoolean(
                "growCrops", growCrops,
                unused -> growCrops,
                value -> growCrops = value
        );

        registerBoolean(
                "growStems", growStems,
                unused -> growStems,
                value -> growStems = value
        );

        registerBoolean(
                "growSweetBerries", growSweetBerries,
                unused -> growSweetBerries,
                value -> growSweetBerries = value
        );

        registerBoolean(
                "growCocoa", growCocoa,
                unused -> growCocoa,
                value -> growCocoa = value
        );

        registerBoolean(
                "growSugarCane", growSugarCane,
                unused -> growSugarCane,
                value -> growSugarCane = value
        );

        registerBoolean(
                "growCactus", growCactus,
                unused -> growCactus,
                value -> growCactus = value
        );

        registerBoolean(
                "ageCopper", ageCopper,
                unused -> ageCopper,
                value -> ageCopper = value
        );

        registerBoolean(
                "decayLeaves", decayLeaves,
                unused -> decayLeaves,
                value -> decayLeaves = value
        );

        registerBoolean(
                "growAmethyst", growAmethyst,
                unused -> growAmethyst,
                value -> growAmethyst = value
        );

        registerBoolean(
                "growGlowBerries", growGlowBerries,
                unused -> growGlowBerries,
                value -> growGlowBerries = value
        );

        registerBoolean(
                "growKelp", growKelp,
                unused -> growKelp,
                value -> growKelp = value
        );

        registerBoolean(
                "growBamboo", growBamboo,
                unused -> growBamboo,
                value -> growBamboo = value
        );

        registerBoolean(
                "hatchTurtleEggs", hatchTurtleEggs,
                unused -> hatchTurtleEggs,
                value -> hatchTurtleEggs = value
        );

        registerBoolean(
                "meltSnow", meltSnow,
                unused -> meltSnow,
                value -> meltSnow = value
        );

        registerBoolean(
                "meltIce", meltIce,
                unused -> meltIce,
                value -> meltIce = value
        );

        registerBoolean(
                "growDripstone", growDripstone,
                unused -> growDripstone,
                value -> growDripstone = value
        );

        registerBoolean(
                "dripstoneFillCauldrons", dripstoneFillCauldrons,
                unused -> dripstoneFillCauldrons,
                value -> dripstoneFillCauldrons = value
        );

        registerBoolean(
                "dripstoneTurnMudToClay", dripstoneTurnMudToClay,
                unused -> dripstoneTurnMudToClay,
                value -> dripstoneTurnMudToClay = value
        );

        registerBoolean(
                "accumulateSnow", accumulateSnow,
                unused -> accumulateSnow,
                value -> accumulateSnow = value
        );

        registerBoolean(
                "waterFreezing", waterFreezing,
                unused -> waterFreezing,
                value -> waterFreezing = value
        );

        registerBoolean(
                "weatherFillCauldron", weatherFillCauldron,
                unused -> weatherFillCauldron,
                value -> weatherFillCauldron = value
        );

        registerBoolean(
                "accurateTurtleAgeAfterHatch", accurateTurtleAgeAfterHatch,
                unused -> accurateTurtleAgeAfterHatch,
                value -> accurateTurtleAgeAfterHatch = value
        );

        registerBoolean(
                "enableBlockEntities", enableBlockEntities,
                unused -> enableBlockEntities,
                value -> enableBlockEntities = value
        );

        registerBoolean(
                "updateFurnace", updateFurnace,
                unused -> updateFurnace,
                value -> updateFurnace = value
        );

        registerBoolean(
                "enableEntities", enableEntities,
                unused -> enableEntities,
                value -> enableEntities = value
        );

        registerBoolean(
                "ageEntities", ageEntities,
                unused -> ageEntities,
                value -> ageEntities = value
        );
    }


    //General
    public int tickDifferenceThreshold = 100;
    public int groupTickDifferenceThreshold = 1000;
    public double groupChunkDifferencePercentage = 0.1;
    public int maxNegativeBinomialAttempts = 20;
    public boolean debugLogs = false;
    public boolean convertCCAData = true;

    //Chunk
    public int maxChunkUpdates = 8;
    public int maxKnownChunkUpdates = 64;
    public int maxForcedChunkLoads = 8;
    public int maxGroupTickUpdates = 1;
    public int maxGroupTickIterations = 1000;
    public double groupTickUpdateStrength = 1.0;
    public int maxGroupTickSize = 10000;
    public boolean randomizeBlockUpdates = false;
    public boolean rememberBlockPositions = true;
    public boolean multiplyMaxChunkUpdatesPerPlayer = false;
    public boolean updateAllChunksWhenSleep = true;
    public boolean enableRandomTicks = true;
    public boolean enablePrecipitationTicks = true;

    //Random Ticks
    public boolean growSaplings = true;
    public boolean growCrops = true;
    public boolean growStems = true;
    public boolean growSweetBerries = true;
    public boolean growCocoa = true;
    public boolean growSugarCane = true;
    public boolean growCactus = true;
    public boolean ageCopper = true;
    public boolean decayLeaves = true;
    public boolean growAmethyst = true;
    public boolean growGlowBerries = true;
    public boolean growKelp = true;
    public boolean growBamboo = true;
    public boolean hatchTurtleEggs = true;
    public boolean meltSnow = true;
    public boolean meltIce = true;
    public boolean growDripstone = true;
    public boolean dripstoneFillCauldrons = true;
    public boolean dripstoneTurnMudToClay = true;

    //Precipitation Ticks
    public boolean accumulateSnow = true;
    public boolean waterFreezing = true;
    public boolean weatherFillCauldron = true;

    //Accuracy
    public boolean accurateTurtleAgeAfterHatch = true;

    //Block Entities
    public boolean enableBlockEntities = true;
    public boolean updateFurnace = true;

    //Entities
    public boolean enableEntities = true;
    public boolean ageEntities = true;
}
