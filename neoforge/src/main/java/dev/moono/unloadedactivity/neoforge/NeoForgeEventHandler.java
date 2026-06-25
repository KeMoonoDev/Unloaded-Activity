package dev.moono.unloadedactivity.neoforge;

import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.UnloadedActivityCommand;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationDataResource;
import dev.moono.unloadedactivity.datapack.group.GroupInfoResource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
#if MC_VER >= MC_1_21_4
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
#else
import net.neoforged.neoforge.event.AddReloadListenerEvent;
#endif
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public class NeoForgeEventHandler {
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        UnloadedActivity.dataPackReloaded(true);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        UnloadedActivity.dataPackReloaded(false);
    }

    /*
    // There is no good alternative to ServerLifecycleEvents.END_DATA_PACK_RELOAD on NeoForge,
    // so this is handled over at mixin/EndDataPackReloadMixin
    @SubscribeEvent
    public void onDatapackReloadEnd(Void event) {}
     */

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

    #if MC_VER >= MC_1_21_4
    @SubscribeEvent
    public void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(SimulationDataResource.BLOCKS_ID, new SimulationDataResource(true));
        event.addListener(SimulationDataResource.TAGS_ID, new SimulationDataResource(false));
        event.addListener(GroupInfoResource.GROUPS_ID, new GroupInfoResource());
    }
    #else
    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SimulationDataResource(true));
        event.addListener(new SimulationDataResource(false));
        event.addListener(new GroupInfoResource());
    }
    #endif
}
