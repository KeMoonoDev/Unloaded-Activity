package lol.zanspace.unloadedactivity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lol.zanspace.unloadedactivity.config.UnloadedActivityConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.WaterFluid;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.LongArgumentType.getLong;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

#if MC_VER >= MC_1_21_11
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
#endif

public class UnloadedActivityCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder commandBuilder = literal("unloadedactivity").requires(source -> {
            // If it's single player then the host should have access to the commands. Otherwise, only people with permission level 4 have access to them.

            #if MC_VER >= MC_1_21_11
            if (source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(4)))) return true;
            #else
            if (source.hasPermission(Commands.LEVEL_OWNERS)) return true;
            #endif

            ServerPlayer player = source.getPlayer();
            if (player == null) return false;
            #if MC_VER >= MC_1_21_10
            return source.getServer().isSingleplayerOwner(player.nameAndId());
            #else
            return source.getServer().isSingleplayerOwner(player.getGameProfile());
            #endif
        });

        addConfigs(commandBuilder);
        addBenchmark(commandBuilder);

        commandBuilder.then(
                literal("check").requires(source ->
            #if MC_VER >= MC_1_21_11
                source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(4)))
            #else
                                source.hasPermission(Commands.LEVEL_OWNERS)
            #endif
            ).then(
                argument("x", integer()).then(
                    argument("y", integer()).then(
                        argument("z", integer()).executes(context -> {
                            try {
                                int x = getInteger(context, "x");
                                int y = getInteger(context, "y");
                                int z = getInteger(context, "z");
                                BlockPos pos = new BlockPos(x, y, z);
                                var state = context.getSource().getLevel().getBlockState(pos);
                                var properties = state.getProperties();
                                String finalStr = "";
                                for (var property: properties) {
                                    finalStr = finalStr + property.getName() + ": " + state.getValue(property).toString() + ", ";
                                }
                                context.getSource().sendSystemMessage(Component.literal(finalStr));
                            } catch (RuntimeException err) {
                                context.getSource().sendSystemMessage(Component.literal(err.toString()));
                            }
                            return 1;
                        })
                    )
                )
            )
        );

        dispatcher.register(commandBuilder);
    }

    public static void addConfigs(LiteralArgumentBuilder commandBuilder) {
        LiteralArgumentBuilder configBuilder = literal("config");

        for (UnloadedActivityConfig.ConfigOption<?> configOption : UnloadedActivity.config.configOptions) {
            configBuilder.then(literal(configOption.name).executes(context ->
                executeConfigGet(context.getSource(), configOption)
            ).then(argument("value", configOption.argumentType).executes(context ->
                executeConfigSet(context, configOption)
            )));
        }

        commandBuilder.then(configBuilder);
    }

    static <T> int executeConfigSet(CommandContext<CommandSourceStack> context, UnloadedActivityConfig.ConfigOption<T> configOption) {
        T value = context.getArgument("value", configOption.tClass);
        configOption.setter.accept(value);
        UnloadedActivity.saveConfig();
        #if MC_VER >= MC_1_20_1
        context.getSource().sendSuccess(() -> Component.literal(configOption.name + " has been set to: " + value), true);
        #else
        context.getSource().sendSuccess(Component.literal(configOption.name + " has been set to: " + value), true);
        #endif
        return 0;
    }

    static <T> int executeConfigGet(CommandSourceStack source, UnloadedActivityConfig.ConfigOption<T> configOption) {
        #if MC_VER >= MC_1_20_1
        source.sendSuccess(() -> Component.literal(configOption.name + " is currently set to: " + configOption.getter.apply(null)), false);
        #else
        source.sendSuccess(Component.literal(configOption.name + " is currently set to: " + configOption.getter.apply(null)), false);
        #endif
        return 0;
    }

    public static void addBenchmark(LiteralArgumentBuilder commandBuilder) {
        commandBuilder.then(
            literal("benchmark").requires(source ->
            #if MC_VER >= MC_1_21_11
                source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(4)))
            #else
                source.hasPermission(Commands.LEVEL_OWNERS)
            #endif
            ).then(
                argument("method", string()).then(
                    argument("trials", integer()).then(
                        argument("attempts", longArg()).then(
                            argument("maxOccurrences", integer()).then(
                                argument("odds", doubleArg()).executes(context -> {

                                    java.lang.reflect.Method method;

                                    try {
                                        method = Utils.class.getMethod(getString(context, "method"), long.class, double.class, int.class, RandomSource.class);
                                    } catch (NoSuchMethodException e) {
                                        context.getSource().sendSystemMessage(Component.literal("No such method."));
                                        return 0;
                                    }

                                    int trials = getInteger(context, "trials");
                                    long attempts = getLong(context, "attempts");
                                    int maxOccurrences = getInteger(context, "maxOccurrences");
                                    double odds = getDouble(context, "odds");

                                    RandomSource random = context.getSource().getLevel().random;

                                    long now = Instant.now().toEpochMilli();

                                    for (int i = 0; i<trials; ++i) {
                                        try {
                                            long newAttempts = attempts > 0L ? attempts : random.nextIntBetweenInclusive(10_000, 100_000_000);
                                            int newMaxOccurrences = maxOccurrences > 0 ? maxOccurrences : random.nextIntBetweenInclusive(1, 100);
                                            double newOdds = odds > 0.0 ? odds : random.nextDouble();
                                            method.invoke(Utils.class, newAttempts, newOdds, newMaxOccurrences, random);
                                        } catch (IllegalAccessException e) {
                                            throw new RuntimeException(e);
                                        } catch (InvocationTargetException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    long difference = Instant.now().toEpochMilli() - now;

                                    float avg = (float)difference/(float)trials;
                                    context.getSource().sendSystemMessage(Component.literal("Total: "+difference + "ms\nAverage: " + avg + "ms"));
                                    return 1;
                                })
                            )
                        )
                    )
                )
            )
        );
    }
}
