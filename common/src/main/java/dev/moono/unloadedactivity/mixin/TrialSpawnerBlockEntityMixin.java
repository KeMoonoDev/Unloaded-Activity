package dev.moono.unloadedactivity.mixin;

#if MC_VER >= MC_1_21_1

#if MC_VER == MC_1_21_1
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerData;
#else
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
#endif

import dev.moono.unloadedactivity.interfaces.SimulateEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TrialSpawnerBlockEntity.class)
public abstract class TrialSpawnerBlockEntityMixin extends BlockEntity implements SimulateEntityHelper {
    public TrialSpawnerBlockEntityMixin(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
    }

    @Shadow @Final
    private TrialSpawner trialSpawner;

    @Override
    public void unloadedactivity$simulate(long ticksPassed, double multiplier) {
        if (this.trialSpawner.getState() != TrialSpawnerState.COOLDOWN) return;

        Level level = this.getLevel();
        if (level == null) return;

        long skippedGameTicks = level.getGameTime() - this.getLastGameTick();
        #if MC_VER == MC_1_21_1
        TrialSpawnerData spawnerData = this.trialSpawner.getData();
        #else
        TrialSpawnerStateData spawnerData = this.trialSpawner.getStateData();
        #endif
        // Compensate cooldownEndsAt with the amount of time passed.
        // cooldownEndsAt is using game time, so skippedGameTicks will add those back from ticksPassed.
        spawnerData.cooldownEndsAt -= ticksPassed - skippedGameTicks;
    }
}
#else
import net.minecraft.world.level.block.AirBlock;
import org.spongepowered.asm.mixin.Mixin;

// Empty mixin for versions without trial spawners
@Mixin(AirBlock.class)
public abstract class TrialSpawnerBlockEntityMixin {}
#endif

