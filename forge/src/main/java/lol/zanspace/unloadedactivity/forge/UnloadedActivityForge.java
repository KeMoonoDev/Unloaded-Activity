package lol.zanspace.unloadedactivity.forge;

import lol.zanspace.unloadedactivity.forge.platform.ForgePlatformHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

import lol.zanspace.unloadedactivity.UnloadedActivity;

@Mod(UnloadedActivity.MOD_ID)
public final class UnloadedActivityForge {
    public UnloadedActivityForge() {
        UnloadedActivity.init(new ForgePlatformHelper());
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
    }
}
