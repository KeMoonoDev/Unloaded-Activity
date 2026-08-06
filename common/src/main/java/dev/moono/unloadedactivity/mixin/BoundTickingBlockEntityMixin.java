package dev.moono.unloadedactivity.mixin;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.TimeMachine;
import dev.moono.unloadedactivity.UnloadedActivity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static java.lang.Long.max;


@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoundTickingBlockEntityMixin<T extends BlockEntity> {
    @Shadow @Final private T blockEntity;

    @Inject(
            at = @At(
                    value="INVOKE",
                    target="net/minecraft/world/level/block/entity/BlockEntityTicker.tick (Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
            ),
            method = "tick",
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void blockEntityTick(CallbackInfo ci, BlockPos blockPos, ProfilerFiller profilerfiller, BlockState blockState) {

        Level level = blockEntity.getLevel();

        if (level instanceof ServerLevel serverLevel) {
            long currentTick = GameUtils.getTime(serverLevel);
            long currentMs = System.currentTimeMillis();

            long tickDifference = 0;

            if (UnloadedActivity.config.useSystemTime) {
                long lastMs = blockEntity.getLastMs();
                if (lastMs > 0) tickDifference = Math.max(currentMs - lastMs, 0) / 50;
            } else {
                long lastTick = blockEntity.getLastTick();
                if (lastTick > 0) tickDifference = max(currentTick - lastTick,0);
            }

            int differenceThreshold = UnloadedActivity.config.tickDifferenceThreshold;

            if (tickDifference > differenceThreshold) {
                double multiplier = UnloadedActivity.config.unloadedSimulationSpeed;
                TimeMachine.simulateBlockEntity(blockEntity, (long)(tickDifference * multiplier));
            }

            this.blockEntity.setLastTick(currentTick);
            this.blockEntity.setLastMs(currentMs);
        }
    }
}