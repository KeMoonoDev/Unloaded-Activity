package dev.moono.unloadedactivity.forge;

import dev.moono.unloadedactivity.forge.platform.ForgePlatformHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

import dev.moono.unloadedactivity.UnloadedActivity;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Mod(UnloadedActivity.MOD_ID)
public final class UnloadedActivityForge {
    public UnloadedActivityForge() {
        HashSet<String> disabledCompats = new HashSet<>();
        for (ModInfo modInfo : FMLLoader.getLoadingModList().getMods()) {
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

        UnloadedActivity.init(new ForgePlatformHelper(), new ArrayList<>(disabledCompats));
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
    }
}
