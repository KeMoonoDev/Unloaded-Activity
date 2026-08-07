package dev.moono.unloadedactivity.neoforge;

import dev.moono.unloadedactivity.neoforge.platform.NeoForgePlatformHelper;
import net.neoforged.fml.common.Mod;
import dev.moono.unloadedactivity.UnloadedActivity;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import java.util.*;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

@Mod(UnloadedActivity.MOD_ID)
public final class UnloadedActivityNeoForge {
    public UnloadedActivityNeoForge() {
        HashSet<String> disabledCompats = new HashSet<>();
        #if MC_VER >= MC_1_21_10
        List<ModInfo> modInfos = FMLLoader.getCurrent().getLoadingModList().getMods();
        #else
        List<ModInfo> modInfos = FMLLoader.getLoadingModList().getMods();
        #endif
        for (ModInfo modInfo : modInfos) {
            modInfo.getOwningFile().getConfigElement(UnloadedActivity.OPTIONS_KEY).ifPresent(optionsObject -> {
                if (optionsObject instanceof Map<?, ?> options) {
                    Object disableCompat =  options.get(UnloadedActivity.DISABLE_COMPAT);

                    if (disableCompat != null) {
                        if (disableCompat instanceof String disableCompatString) {
                            disabledCompats.add(disableCompatString);
                        } else if (disableCompat instanceof List<?> disableCompatList) {
                            for (Object innerValue : disableCompatList) {
                                if (innerValue instanceof String disableCompatString) {
                                    disabledCompats.add(disableCompatString);
                                } else {
                                    UnloadedActivity.LOGGER.warn("Mod \""+modInfo.getModId()+"\" tried to disable some compatibility in Unloaded Activity, but its array doesn't only contain string values. Some values will be ignored.");
                                }
                            }
                        } else {
                            UnloadedActivity.LOGGER.warn("Mod \""+modInfo.getModId()+"\"  tried to disable some compatibility in Unloaded Activity, but the value isn't an array of strings or a string. The value will be ignored.");
                        }
                    }
                } else {
                    UnloadedActivity.LOGGER.warn("Mod \""+modInfo.getModId()+"\" has an invalid \"" + UnloadedActivity.OPTIONS_KEY + "\" property. It needs to be a Map.");
                }
            });
        }

        UnloadedActivity.init(new NeoForgePlatformHelper(), new ArrayList<>(disabledCompats));
        EVENT_BUS.register(new NeoForgeEventHandler());
    }
}
