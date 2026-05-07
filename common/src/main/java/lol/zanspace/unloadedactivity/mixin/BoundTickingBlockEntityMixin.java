package lol.zanspace.unloadedactivity.mixin;

import lol.zanspace.unloadedactivity.TimeMachine;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.util.perf.Profiler;

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

        if (level instanceof ServerLevel ServerLevel) {
            long lastTick = blockEntity.getLastTick();

            long currentTime = ServerLevel.getDayTime();

            if (lastTick != 0) {

                long timeDifference = max(currentTime - lastTick,0);

                int differenceThreshold = UnloadedActivity.config.tickDifferenceThreshold;

                if (timeDifference > differenceThreshold)
                    TimeMachine.simulateBlockEntity(blockEntity, timeDifference);
            }
            this.blockEntity.setLastTick(currentTime);
        }
    }
}