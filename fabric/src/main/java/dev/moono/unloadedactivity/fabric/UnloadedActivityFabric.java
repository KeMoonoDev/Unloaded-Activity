package dev.moono.unloadedactivity.fabric;

#if MC_VER >= MC_1_21_10
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
#else
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
#endif

import dev.moono.unloadedactivity.api.UnloadedActivityApi;
import dev.moono.unloadedactivity.datapack.group.GroupInfoResource;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationDataResource;
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.UnloadedActivityCommand;
import dev.moono.unloadedactivity.fabric.platform.FabricPlatformHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.server.packs.PackType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class UnloadedActivityFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		FabricLoader fabricLoader = FabricLoader.getInstance();
		List<UnloadedActivityApi> extraEntryPoints = fabricLoader.getEntrypoints(UnloadedActivity.MOD_ID, UnloadedActivityApi.class);

		HashSet<String> disabledCompats = new HashSet<>();

		for (ModContainer container : fabricLoader.getAllMods()) {
			ModMetadata metadata = container.getMetadata();

			if (!metadata.containsCustomValue(UnloadedActivity.OPTIONS_KEY)) continue;

			CustomValue options = metadata.getCustomValue(UnloadedActivity.OPTIONS_KEY);

			if (options.getType() != CustomValue.CvType.OBJECT) {
				UnloadedActivity.LOGGER.warn("Mod \""+metadata.getId()+"\" has an invalid \"" + UnloadedActivity.OPTIONS_KEY + "\" property. It needs to be an Object.");
				continue;
			}

			CustomValue.CvObject object = options.getAsObject();

			CustomValue disableCompat = object.get(UnloadedActivity.DISABLE_COMPAT);

			if (disableCompat != null) {
				if (disableCompat.getType() == CustomValue.CvType.STRING) {
					disabledCompats.add(disableCompat.getAsString());
				} else if (disableCompat.getType() == CustomValue.CvType.ARRAY) {
					for (CustomValue innerValue : disableCompat.getAsArray()) {
						if (innerValue.getType() != CustomValue.CvType.STRING) {
							UnloadedActivity.LOGGER.warn("Mod \""+metadata.getId()+"\" tried to disable some compatibility in Unloaded Activity, but its array doesn't only contain string values. Some values will be ignored.");
							continue;
						}
						disabledCompats.add(innerValue.getAsString());
					}
				} else {
					UnloadedActivity.LOGGER.warn("Mod \""+metadata.getId()+"\"  tried to disable some compatibility in Unloaded Activity, but the value isn't an array of strings or a string. The value will be ignored.");
				}
			}
		}

		UnloadedActivity.init(new FabricPlatformHelper(), new ArrayList<>(disabledCompats), extraEntryPoints);

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
