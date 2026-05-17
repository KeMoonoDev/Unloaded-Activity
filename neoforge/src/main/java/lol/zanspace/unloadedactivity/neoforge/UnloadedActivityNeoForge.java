package lol.zanspace.unloadedactivity.neoforge;

import lol.zanspace.unloadedactivity.neoforge.platform.NeoForgePlatformHelper;
import net.neoforged.fml.common.Mod;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

@Mod(UnloadedActivity.MOD_ID)
public final class UnloadedActivityNeoForge {
    public UnloadedActivityNeoForge() {
        UnloadedActivity.init(new NeoForgePlatformHelper());
        EVENT_BUS.register(new NeoForgeEventHandler());
    }
}
