package dev.moono.unloadedactivity.mixin;

import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.TimeMachine;
import dev.moono.unloadedactivity.UnloadedActivity;
import net.minecraft.commands.CommandSource;
#if MC_VER > MC_1_21_5
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
#endif

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static java.lang.Long.max;
import static dev.moono.unloadedactivity.UnloadedActivity.MOD_ID;
import static dev.moono.unloadedactivity.UnloadedActivity.OLD_MOD_ID;


@Mixin(value = Entity.class, priority = 999)
public abstract class EntityMixin implements Nameable, EntityAccess, CommandSource {

    @Unique
    private long lastDayTick = 0;
    @Unique
    private long lastGameTick = 0;
    @Unique
    private long lastMs = 0;

    @Shadow public Level level;
    @Inject(at = @At("HEAD"), method = "tick")
    public void tickMovement(CallbackInfo ci) {
        if (level.isClientSide())
            return;

        long currentDayTick = GameUtils.getDayTime(level);
        long currentGameTick = level.getGameTime();
        long currentMs = System.currentTimeMillis();

        long tickDifference = 0;

        switch (UnloadedActivity.config.simulationClock) {
            case DAY_TIME -> {
                if (lastDayTick > 0) tickDifference = max(currentDayTick - lastDayTick, 0);
            }
            case GAME_TIME -> {
                if (lastGameTick > 0) tickDifference = max(currentGameTick - lastGameTick,0);
            }
            case SYSTEM_TIME -> {
                if (lastMs > 0) tickDifference = Math.max(currentMs - lastMs, 0) / 50;
            }
        }

        int differenceThreshold = UnloadedActivity.config.tickDifferenceThreshold;

        if (tickDifference > differenceThreshold) {
            double multiplier = UnloadedActivity.config.unloadedSimulationSpeed;
            TimeMachine.simulateEntity((Entity) (Object) this, (long)(tickDifference * multiplier));
        }

        this.lastDayTick = currentDayTick;
        this.lastGameTick = currentGameTick;
        this.lastMs = currentMs;
    }

    @Inject( at = @At("RETURN"), method = "<init>")
    private void init(EntityType<?> type, Level level, CallbackInfo ci) {
        this.lastDayTick = GameUtils.getDayTime(level);
        this.lastGameTick = level.getGameTime();
        this.lastMs = System.currentTimeMillis();
    }

    #if MC_VER <= MC_1_21_5
    @Inject( at = @At("RETURN"), method = "saveWithoutId")
    private void save(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag returnedNbt = cir.getReturnValue();

        CompoundTag entityData = new CompoundTag();

        entityData.putLong("last_day_tick", this.lastDayTick);
        entityData.putLong("last_game_tick", this.lastGameTick);
        entityData.putLong("last_ms", this.lastMs);

        returnedNbt.put(MOD_ID, entityData);
    }
    #else
    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void save(ValueOutput nbt, CallbackInfo ci) {
        CompoundTag entityData = new CompoundTag();

        entityData.putLong("last_day_tick", this.lastDayTick);
        entityData.putLong("last_game_tick", this.lastGameTick);
        entityData.putLong("last_ms", this.lastMs);

        nbt.store(MOD_ID, CompoundTag.CODEC, entityData);
    }
    #endif

    #if MC_VER <= MC_1_21_5
    @Inject(method = "load", at = @At(value = "INVOKE", target = "net/minecraft/world/entity/Entity.readAdditionalSaveData (Lnet/minecraft/nbt/CompoundTag;)V", shift = At.Shift.AFTER))
    private void load(CompoundTag nbt, CallbackInfo ci)
    #else
    @Inject(method = "load", at = @At(value = "INVOKE", target = "net/minecraft/world/entity/Entity.readAdditionalSaveData (Lnet/minecraft/world/level/storage/ValueInput;)V", shift = At.Shift.AFTER))
    private void load(ValueInput nbt, CallbackInfo ci)
    #endif
    {
        #if MC_VER <= MC_1_21_5
        CompoundTag entityData = nbt.getCompound(MOD_ID)#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag()) #endif;
        #else
        CompoundTag entityData = nbt.read(MOD_ID, CompoundTag.CODEC).orElse(new CompoundTag());
        #endif

        if (entityData.isEmpty()) {
            #if MC_VER <= MC_1_21_5
            entityData = nbt.getCompound(OLD_MOD_ID)#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag()) #endif;
            #else
            entityData = nbt.read(OLD_MOD_ID, CompoundTag.CODEC).orElse(new CompoundTag());
            #endif
        }

        boolean isEmpty = entityData.isEmpty();

        if (!isEmpty) {
            if (entityData.contains("last_day_tick")) {

                this.lastDayTick = entityData.getLong("last_day_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            } else {
                this.lastDayTick = entityData.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            }
            this.lastGameTick = entityData.getLong("last_game_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
            this.lastMs = entityData.getLong("last_ms")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
        }

        if (UnloadedActivity.config.convertCCAData && isEmpty) {
            #if MC_VER <= MC_1_21_5
            CompoundTag cardinalData = nbt.getCompound("cardinal_components")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
            #else
            CompoundTag cardinalData = nbt.read("cardinal_components", CompoundTag.CODEC).orElse(new CompoundTag());
            #endif

            if (!cardinalData.isEmpty()) {
                CompoundTag lastEntityTick = cardinalData.getCompound("unloadedactivity:last-entity-tick")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
                if (!lastEntityTick.isEmpty()) {
                    this.lastGameTick = lastEntityTick.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
                }

                // This is so that cardinal components doesn't start sending warnings.
                cardinalData.remove("unloadedactivity:last-entity-tick");
            }
        }

        if (this.lastDayTick == 0) this.lastDayTick = GameUtils.getDayTime(level);
        if (this.lastGameTick == 0) this.lastGameTick = level.getGameTime();
        if (this.lastMs == 0) this.lastMs = System.currentTimeMillis();

    }
}
