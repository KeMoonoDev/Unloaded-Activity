package lol.zanspace.unloadedactivity.forge;

import lol.zanspace.unloadedactivity.UnloadedActivityCommand;
import lol.zanspace.unloadedactivity.datapack.GroupInfoResource;
import lol.zanspace.unloadedactivity.datapack.SimulationDataResource;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeEventHandler {
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
