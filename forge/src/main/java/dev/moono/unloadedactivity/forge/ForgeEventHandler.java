package dev.moono.unloadedactivity.forge;

import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.UnloadedActivityCommand;
import dev.moono.unloadedactivity.datapack.GroupInfoResource;
import dev.moono.unloadedactivity.datapack.SimulationDataResource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeEventHandler {
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        LevelAccessor level = event.getLevel();
        MinecraftServer server = level.getServer();

        if (server == null) {
            return;
        }

        if (event.getChunk() instanceof LevelChunk chunk) {
            UnloadedActivity.addChunkToQueue(server, chunk);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        UnloadedActivityCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SimulationDataResource(true));
        event.addListener(new SimulationDataResource(false));
        event.addListener(new GroupInfoResource());
    }
}
