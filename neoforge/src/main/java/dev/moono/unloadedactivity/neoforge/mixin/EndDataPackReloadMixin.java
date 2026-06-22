package dev.moono.unloadedactivity.neoforge.mixin;

import dev.moono.unloadedactivity.UnloadedActivity;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

// There is no good alternative to ServerLifecycleEvents.END_DATA_PACK_RELOAD on NeoForge,
// so I just copy the mixin from the fabric api.

@Mixin(MinecraftServer.class)
public class EndDataPackReloadMixin {
    @Inject(method = "reloadResources", at = @At("TAIL"))
    private void endResourceReload(Collection<String> collection, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        cir.getReturnValue().handleAsync((returned, throwable) -> {
            UnloadedActivity.dataPackReloaded(throwable == null);
            return returned;
        }, (MinecraftServer) (Object) this);
    }
}
