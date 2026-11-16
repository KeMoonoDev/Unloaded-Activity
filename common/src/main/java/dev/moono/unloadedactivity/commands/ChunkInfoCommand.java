package dev.moono.unloadedactivity.commands;

#if MC_VER >= MC_1_21_1
import net.minecraft.world.level.chunk.status.ChunkStatus;
#else
import net.minecraft.world.level.chunk.ChunkStatus;
#endif

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.moono.unloadedactivity.SimulationInfo;
import dev.moono.unloadedactivity.TimeMachine;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec2;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ChunkInfoCommand {
    public static void register(LiteralArgumentBuilder<CommandSourceStack> commandBuilder) {
        commandBuilder.then(
        literal("chunk_info").then(
                argument("chunk_pos", Vec2Argument.vec2(false)).executes(context -> {
                    var source = context.getSource();
                    try {
                        Vec2 chunkPos = Vec2Argument.getVec2(context, "chunk_pos");
                        ChunkAccess chunk = source.getLevel().getChunk((int)chunkPos.x, (int)chunkPos.y, ChunkStatus.FULL, false);

                        if (chunk == null) {
                            source.sendSystemMessage(Component.literal("Chunk is not loaded."));
                            return 1;
                        }

                        var message = Component.literal("------------------------------");

                        message.append(Component.literal("\nChunk info").withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
                        message.append(Component.literal("\nindexed: ").withStyle(ChatFormatting.AQUA));
                        message.append(Component.literal(""+ TimeMachine.isChunkIndexed(chunk)));

                        message.append(Component.literal("\nentries: ").withStyle(ChatFormatting.AQUA));
                        message.append(Component.literal(""+ chunk.getSimulationBlocks().size()));

                        message.append(Component.literal("\nlastDayTick: ").withStyle(ChatFormatting.AQUA));
                        message.append(Component.literal(""+chunk.getLastDayTick()));

                        message.append(Component.literal("\nlastGameTick: ").withStyle(ChatFormatting.AQUA));
                        message.append(Component.literal(""+chunk.getLastGameTick()));

                        message.append(Component.literal("\nlastMs: ").withStyle(ChatFormatting.AQUA));
                        message.append(Component.literal(""+chunk.getLastMs()));

                        message.append(Component.literal("\nsimulationsSinceLoad: ").withStyle(ChatFormatting.AQUA));
                        message.append(Component.literal(""+chunk.getSimulationsSinceLoad()));

                        message.append(Component.literal("\nLast simulation info: ").withStyle(ChatFormatting.YELLOW));
                        SimulationInfo lastSimulationInfo = chunk.getLastSimulationInfo();

                        if (lastSimulationInfo == null) {
                            message.append(Component.literal("None").withStyle(ChatFormatting.DARK_RED));
                        } else {
                            message.append(Component.literal("\nsimulationClock: ").withStyle(ChatFormatting.AQUA));
                            message.append(Component.literal(lastSimulationInfo.simulationClock.name()));

                            message.append(Component.literal("\nsimulatedTicks: ").withStyle(ChatFormatting.AQUA));
                            message.append(Component.literal(""+lastSimulationInfo.simulatedTicks));

                            message.append(Component.literal("\nsimulationDuration: ").withStyle(ChatFormatting.AQUA));
                            message.append(Component.literal(lastSimulationInfo.simulationDuration + "ms"));
                        }

                        message.append(Component.literal("\n\nSimulation groups").withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
                        for (var groupIndex : chunk.getGroupIndexes()) {
                            message.append(Component.literal("\n\n"+groupIndex.groupId.toString()).withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN));
                            message.append(Component.literal("\nentries: ").withStyle(ChatFormatting.AQUA));
                            message.append(Component.literal(""+groupIndex.getPositions().size()));

                            message.append(Component.literal("\nlastDayTick: ").withStyle(ChatFormatting.AQUA));
                            message.append(Component.literal(
                                    groupIndex.getLastDayTick(Long.MIN_VALUE) + " (Fixed: "+groupIndex.getLastDayTick(chunk.getLastDayTick())+")"
                            ));

                            message.append(Component.literal("\nlastGameTick: ").withStyle(ChatFormatting.AQUA));
                            message.append(Component.literal(
                                    groupIndex.getLastGameTick(Long.MIN_VALUE) + " (Fixed: "+groupIndex.getLastGameTick(chunk.getLastGameTick())+")"
                            ));

                            message.append(Component.literal("\nlastMs: ").withStyle(ChatFormatting.AQUA));
                            message.append(Component.literal(
                                    groupIndex.getLastMs(Long.MIN_VALUE) + " (Fixed: "+groupIndex.getLastMs(chunk.getLastMs())+")"
                            ));

                            message.append(Component.literal("\nsimulationsSinceLoad: ").withStyle(ChatFormatting.AQUA));
                            message.append(Component.literal(""+groupIndex.getSimulationsSinceLoad()));

                            message.append(Component.literal("\nLast simulation info: ").withStyle(ChatFormatting.YELLOW));
                            SimulationInfo lastGroupSimulationInfo = groupIndex.getLastSimulationInfo();

                            if (lastGroupSimulationInfo == null) {
                                message.append(Component.literal("None").withStyle(ChatFormatting.DARK_RED));
                            } else {
                                message.append(Component.literal("\nsimulationClock: ").withStyle(ChatFormatting.AQUA));
                                message.append(Component.literal(lastGroupSimulationInfo.simulationClock.name()));

                                message.append(Component.literal("\nsimulatedTicks: ").withStyle(ChatFormatting.AQUA));
                                message.append(Component.literal(""+lastGroupSimulationInfo.simulatedTicks));

                                message.append(Component.literal("\nsimulationDuration: ").withStyle(ChatFormatting.AQUA));
                                message.append(Component.literal(lastGroupSimulationInfo.simulationDuration + "ms"));
                            }
                        }

                        message.append("\n------------------------------");

                        source.sendSystemMessage(message);
                    } catch (RuntimeException err) {
                        source.sendSystemMessage(Component.literal(err.toString()));
                    }
                    return 1;
                })
            )
        );
    }
}
