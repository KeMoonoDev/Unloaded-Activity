package lol.zanspace.unloadedactivity.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

import lol.zanspace.unloadedactivity.UnloadedActivity;

@Mod(UnloadedActivity.MOD_ID)
public final class UnloadedActivityForge {
    public UnloadedActivityForge() {
        UnloadedActivity.init();
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
    }
}
