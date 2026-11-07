package dev.moono.unloadedactivity.mixin;

import dev.moono.unloadedactivity.GameUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Pseudo
@Mixin(
    targets = {
        "net/caffeinemc/mods/lithium/common/block/entity/SleepingBlockEntity",
        "me/jellysquid/mods/lithium/common/block/entity/SleepingBlockEntity"
    },
    remap = false
)
public interface LithiumSleepingBlockEntityMixin {
    @Inject(
        method = "wakeUpNow",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At("RETURN")
    )
    default
    void unloadedactivity$wakeUpNow(CallbackInfo ci, TickingBlockEntity sleepingTicker) {
        if (sleepingTicker == null) return;
        if (sleepingTicker instanceof BoundTickingBlockEntityAccessor accessor) {
            BlockEntity blockEntity = accessor.unloadedactivity$getBlockEntity();
            Level level = blockEntity.getLevel();
            if (level instanceof ServerLevel serverLevel) {
                long currentTick = GameUtils.getTime(serverLevel);
                long currentMs = System.currentTimeMillis();
                blockEntity.setLastTick(currentTick);
                blockEntity.setLastMs(currentMs);
            }
        }
    }
}
