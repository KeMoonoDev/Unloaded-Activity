package dev.moono.unloadedactivity;

#if MC_VER >= MC_1_21_11
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
#endif

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.moono.unloadedactivity.config.ConfigOption;
import net.minecraft.commands.*;
import net.minecraft.server.level.ServerPlayer;

import static dev.moono.unloadedactivity.UnloadedActivity.MOD_ID;
import static net.minecraft.commands.Commands.literal;

public class UnloadedActivityCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        LiteralArgumentBuilder<CommandSourceStack> commandBuilder = literal(MOD_ID).requires(source -> {
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

        addConfigs(commandBuilder, context);

        dispatcher.register(commandBuilder);
    }

    public static void addConfigs(LiteralArgumentBuilder<CommandSourceStack> commandBuilder, CommandBuildContext context) {
        LiteralArgumentBuilder<CommandSourceStack> configBuilder = literal("config");

        for (ConfigOption configOption : UnloadedActivity.config.configOptions) {
            configOption.addCommand(configBuilder, context);
        }

        commandBuilder.then(configBuilder);
    }
}
