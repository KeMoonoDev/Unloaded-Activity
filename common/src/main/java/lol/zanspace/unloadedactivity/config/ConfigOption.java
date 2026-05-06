package lol.zanspace.unloadedactivity.config;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

public interface ConfigOption {
    void addCommand(LiteralArgumentBuilder<CommandSourceStack> argumentBuilder);
}
