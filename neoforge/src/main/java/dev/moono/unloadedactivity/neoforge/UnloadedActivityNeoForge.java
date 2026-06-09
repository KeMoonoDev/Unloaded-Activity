package dev.moono.unloadedactivity.neoforge;

import dev.moono.unloadedactivity.neoforge.platform.NeoForgePlatformHelper;
import net.neoforged.fml.common.Mod;
import dev.moono.unloadedactivity.UnloadedActivity;
import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

@Mod(UnloadedActivity.MOD_ID)
public final class UnloadedActivityNeoForge {
    public UnloadedActivityNeoForge() {
        UnloadedActivity.init(new NeoForgePlatformHelper());
        EVENT_BUS.register(new NeoForgeEventHandler());
    }
}
