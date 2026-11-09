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
import org.spongepowered.asm.mixin.Unique;
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
            long currentDayTick = GameUtils.getDayTime(serverLevel);
            long currentGameTick = serverLevel.getGameTime();
            long currentMs = System.currentTimeMillis();

            long tickDifference = getTickDifference(currentDayTick, currentGameTick, currentMs);

            int differenceThreshold = UnloadedActivity.config.tickDifferenceThreshold;

            if (tickDifference > differenceThreshold) {
                TimeMachine.simulateBlockEntity(blockEntity, tickDifference);
            }

            this.blockEntity.setLastDayTick(currentDayTick);
            this.blockEntity.setLastGameTick(currentGameTick);
            this.blockEntity.setLastMs(currentMs);
        }
    }

    @Unique
    private long getTickDifference(long currentDayTick, long currentGameTick, long currentMs) {
        long tickDifference = 0;

        switch (UnloadedActivity.config.simulationClock) {
            case DAY_TIME -> {
                long lastTick = blockEntity.getLastDayTick();
                if (lastTick > 0) tickDifference = max(currentDayTick - lastTick,0);
            }
            case GAME_TIME -> {
                long lastTick = blockEntity.getLastGameTick();
                if (lastTick > 0) tickDifference = max(currentGameTick - lastTick,0);
            }
            case SYSTEM_TIME -> {
                long lastMs = blockEntity.getLastMs();
                if (lastMs > 0) tickDifference = Math.max(currentMs - lastMs, 0) / 50;
            }
        }
        return tickDifference;
    }
}