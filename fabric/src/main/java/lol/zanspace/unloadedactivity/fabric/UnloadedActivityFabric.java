package lol.zanspace.unloadedactivity.fabric;

#if MC_VER >= MC_1_21_10
import lol.zanspace.unloadedactivity.datapack.GroupInfoResource;
import lol.zanspace.unloadedactivity.datapack.SimulationDataResource;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
#else
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
#endif

import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.UnloadedActivityCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.chunk.LevelChunk;


public class UnloadedActivityFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		UnloadedActivity.init();

		CommandRegistrationCallback.EVENT.register((dispatcher,context,environment) -> UnloadedActivityCommand.register(dispatcher));
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> level.getServer().addChunkToQueue(chunk));
		#if MC_VER >= MC_1_21_10
		ResourceLoader.get(PackType.SERVER_DATA).registerReloader(SimulationDataResource.TAGS_ID, new SimulationDataResource(false));
		ResourceLoader.get(PackType.SERVER_DATA).registerReloader(SimulationDataResource.BLOCKS_ID, new SimulationDataResource(true));
		ResourceLoader.get(PackType.SERVER_DATA).registerReloader(GroupInfoResource.GROUPS_ID, new GroupInfoResource());
		#else
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimulationDataResourceFabric(false));
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimulationDataResourceFabric(true));
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new GroupInfoResourceFabric());
		#endif
	}
}
