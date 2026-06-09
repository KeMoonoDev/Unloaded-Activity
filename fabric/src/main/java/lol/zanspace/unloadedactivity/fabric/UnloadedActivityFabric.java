package lol.zanspace.unloadedactivity.fabric;

#if MC_VER >= MC_1_21_10
import lol.zanspace.unloadedactivity.datapack.GroupInfoResource;
import lol.zanspace.unloadedactivity.datapack.SimulationDataResource;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
#else
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
#endif

import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.UnloadedActivityCommand;
import lol.zanspace.unloadedactivity.fabric.platform.FabricPlatformHelper;
import lol.zanspace.unloadedactivity.platform.IPlatformHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.chunk.LevelChunk;


public class UnloadedActivityFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		UnloadedActivity.init(new FabricPlatformHelper());

		//FabricLoader.getInstance().getEntrypoints()

		CommandRegistrationCallback.EVENT.register((dispatcher,context,environment) -> UnloadedActivityCommand.register(dispatcher));
		#if MC_VER >= MC_26_1_2
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, ignored) -> UnloadedActivity.addChunkToQueue(level.getServer(), chunk));
		#else
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> UnloadedActivity.addChunkToQueue(level.getServer(), chunk));
		#endif

		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			UnloadedActivity.dataPackReloaded(true);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
			UnloadedActivity.dataPackReloaded(false);
		});

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			UnloadedActivity.dataPackReloaded(success);
		});

		#if MC_VER >= MC_26_1_2
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SimulationDataResource.TAGS_ID, new SimulationDataResource(false));
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SimulationDataResource.BLOCKS_ID, new SimulationDataResource(true));
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(GroupInfoResource.GROUPS_ID, new GroupInfoResource());
		#elif MC_VER >= MC_1_21_10
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
