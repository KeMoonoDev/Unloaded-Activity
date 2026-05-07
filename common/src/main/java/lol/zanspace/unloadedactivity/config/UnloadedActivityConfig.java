package lol.zanspace.unloadedactivity.config;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagLocationArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class UnloadedActivityConfig {

    private transient HashMap<ResourceLocation, Boolean> blacklistCache = new HashMap<>();

    public static class SimpleOption<T> implements ConfigOption {
        public ArgumentType<T> argumentType;
        public T defaultValue;
        public String name;
        public Function<Void, T> getter;
        public Consumer<T> setter;
        public Class<T> tClass;

        public SimpleOption(ArgumentType<T> argumentType, String name, T defaultValue, Function<Void, T> getter, Consumer<T> setter, Class<T> tClass) {
            this.argumentType = argumentType;
            this.name = name;
            this.defaultValue = defaultValue;
            this.getter = getter;
            this.setter = setter;
            this.tClass = tClass;
        }

        int executeConfigGet(CommandContext<CommandSourceStack> context) {
            #if MC_VER >= MC_1_20_1
            context.getSource().sendSuccess(() -> Component.literal(this.name + " is currently set to: " + this.getter.apply(null)), false);
            #else
            context.getSource().sendSuccess(Component.literal(this.name + " is currently set to: " + this.getter.apply(null)), false);
            #endif
            return 0;
        }

        int executeConfigSet(CommandContext<CommandSourceStack> context) {
            T value = context.getArgument("value", this.tClass);
            this.setter.accept(value);
            UnloadedActivity.saveConfig();
            #if MC_VER >= MC_1_20_1
            context.getSource().sendSuccess(() -> Component.literal(this.name + " has been set to: " + value), true);
            #else
            context.getSource().sendSuccess(Component.literal(this.name + " has been set to: " + value), true);
            #endif
            return 0;
        }

        @Override
        public void addCommand(LiteralArgumentBuilder<CommandSourceStack> argumentBuilder) {
            argumentBuilder.then(literal(this.name).executes(this::executeConfigGet)
                .then(argument("value", this.argumentType).executes(this::executeConfigSet))
            );
        }
    }

    public static class IdList implements ConfigOption {
        public String name;
        public Supplier<List<BlockOrTag>> getter;

        public IdList(String name, Supplier<List<BlockOrTag>> getter) {
            this.name = name;
            this.getter = getter;
        }

        int executeConfigGet(CommandContext<CommandSourceStack> context) {
            var list = this.getter.get();
            #if MC_VER >= MC_1_20_1
            context.getSource().sendSuccess(() -> Component.literal(this.name + " has the following values: " + list.toString()), false);
            #else
            context.getSource().sendSuccess(Component.literal(this.name + " has the following values: " + list.toString()), false);
            #endif
            return 0;
        }

        int executeConfigAdd(CommandContext<CommandSourceStack> context) {
            ResourceOrTagLocationArgument.Result<Block> input = context.getArgument("value", ResourceOrTagLocationArgument.Result.class);
            Either<ResourceKey<Block>, TagKey<Block>> eitherBlockOrTag = input.unwrap();

            BlockOrTag blockOrTag;
            if (eitherBlockOrTag.left().isPresent()) {
                blockOrTag = new BlockOrTag(false, eitherBlockOrTag.left().get().location());
            } else {
                blockOrTag = new BlockOrTag(true, eitherBlockOrTag.right().orElseThrow().location());
            }

            var list = this.getter.get();

            if (list.contains(blockOrTag)) {
                #if MC_VER >= MC_1_20_1
                context.getSource().sendSuccess(() -> Component.literal(this.name + " already has the value " + blockOrTag), true);
                #else
                context.getSource().sendSuccess(Component.literal(this.name + " already has the value " + blockOrTag), true);
                #endif
                return 0;
            }
            list.add(blockOrTag);
            UnloadedActivity.config.blacklistCache.clear();
            UnloadedActivity.saveConfig();
            #if MC_VER >= MC_1_20_1
            context.getSource().sendSuccess(() -> Component.literal(blockOrTag + " has been added to " + this.name), true);
            #else
            context.getSource().sendSuccess(Component.literal(blockOrTag + " has been added to " + this.name), true);
            #endif
            return 0;
        }

        int executeConfigRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            String input = context.getArgument("value", String.class);

            String finalValue;

            // If user just writes "stone", this will correct it to "minecraft:stone"
            if (input.startsWith("#")) {
                finalValue = "#" + ResourceLocation.read(new StringReader(input.substring(1)));
            } else {
                finalValue = ResourceLocation.read(new StringReader(input)).toString();
            }

            var list = this.getter.get();

            boolean removed = list.removeIf(blockOrTag -> blockOrTag.toString().equals(finalValue));

            if (!removed) {
                #if MC_VER >= MC_1_20_1
                context.getSource().sendSuccess(() -> Component.literal(this.name + " doesn't have the value " + finalValue), true);
                #else
                context.getSource().sendSuccess(Component.literal(this.name + " doesn't have the value " + finalValue), true);
                #endif
                return 0;
            }

            UnloadedActivity.config.blacklistCache.clear();
            UnloadedActivity.saveConfig();
            #if MC_VER >= MC_1_20_1
            context.getSource().sendSuccess(() -> Component.literal(finalValue + " has been removed from " + this.name), true);
            #else
            context.getSource().sendSuccess(Component.literal(finalValue + " has been removed from " + this.name), true);
            #endif
            return 0;
        }

        @Override
        public void addCommand(LiteralArgumentBuilder<CommandSourceStack> argumentBuilder) {
            var listGetter = this.getter;
            var stringListSuggestions = new SuggestionProvider<CommandSourceStack>() {
                @Override
                public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
                    var list = listGetter.get();
                    String input = builder.getRemainingLowerCase();
                    for (var option : list) {
                        String optionString = option.toString();
                        if (optionString.contains(input)) {
                            builder.suggest(option.toString());
                        }
                    }
                    return builder.buildFuture();
                }
            };

            argumentBuilder.then(literal(this.name)
                .then(literal("list").executes(this::executeConfigGet))
                .then(literal("add").then(argument("value", new ResourceOrTagLocationArgument<Block>(Registry.BLOCK_REGISTRY)).executes(this::executeConfigAdd)))
                .then(literal("remove").then(argument("value", StringArgumentType.greedyString()).suggests(stringListSuggestions).executes(this::executeConfigRemove)))
            );
        }
    }

    public transient ArrayList<ConfigOption> configOptions = new ArrayList<>();

    public void registerInt(String name, int defaultValue, int minValue, int maxValue, Function<Void, Integer> getter, Consumer<Integer> setter) {
        IntegerArgumentType argumentType = IntegerArgumentType.integer(minValue, maxValue);
        SimpleOption<Integer> configOption = new SimpleOption<>(argumentType, name, defaultValue, getter, setter, int.class);
        configOptions.add(configOption);
    }

    public void registerFloat(String name, float defaultValue, float minValue, float maxValue, Function<Void, Float> getter, Consumer<Float> setter) {
        FloatArgumentType argumentType = FloatArgumentType.floatArg(minValue, maxValue);
        SimpleOption<Float> configOption = new SimpleOption<>(argumentType, name, defaultValue, getter, setter, float.class);
        configOptions.add(configOption);
    }

    public void registerBoolean(String name, boolean defaultValue, Function<Void, Boolean> getter, Consumer<Boolean> setter) {
        BoolArgumentType argumentType = BoolArgumentType.bool();
        SimpleOption<Boolean> configOption = new SimpleOption<>(argumentType, name, defaultValue, getter, setter, boolean.class);
        configOptions.add(configOption);
    }

    public void registerIdList(String name, Supplier<List<BlockOrTag>> getter) {
        configOptions.add(new IdList(name, getter));
    }

    public boolean isBlockBlacklisted(BlockState state) {
        var blockId = Registry.BLOCK.getKey(state.getBlock());
        return blacklistCache.computeIfAbsent(blockId, (unused) ->
            blacklistedBlocks.stream().anyMatch((blacklisted) -> {
                if (blacklisted.isTag) {
                    return state.is(TagKey.create(Registry.BLOCK_REGISTRY, blacklisted.id));
                } else {
                    return Registry.BLOCK.getKey(state.getBlock()).equals(blacklisted.id);
                }
            })
        );
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
                "maxGroupUpdatesPerTick", maxGroupUpdatesPerTick, 0, Integer.MAX_VALUE,
                unused -> maxGroupUpdatesPerTick,
                value -> maxGroupUpdatesPerTick = value
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

        registerIdList("blacklistedBlocks", () -> blacklistedBlocks);

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

        registerBoolean(
                "simulateSheepDesireToEat", simulateSheepDesireToEat,
                unused -> simulateSheepDesireToEat,
                value -> simulateSheepDesireToEat = value
        );
    }


    //General
    public int tickDifferenceThreshold = 100;
    public int maxNegativeBinomialAttempts = 20;
    public boolean debugLogs = false;
    public boolean convertCCAData = true;

    //Group
    public int maxGroupUpdatesPerTick = 1;
    public int groupTickDifferenceThreshold = 1000;
    public float maxGroupTickDeviationScale = 0.1F;
    public int maxForcedChunkLoads = 8;
    public int maxGroupTickIterations = 1000;
    public float groupTickUpdateStrength = 1F;
    public int maxGroupTickSize = 10000;

    //Chunk
    public int maxChunksIndexedPerTick = 8;
    public int maxChunkUpdatesPerTick = 64;
    public boolean randomizeBlockUpdates = false;
    public ArrayList<BlockOrTag> blacklistedBlocks = new ArrayList<>();
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
    public boolean simulateSheepDesireToEat = true;
}
