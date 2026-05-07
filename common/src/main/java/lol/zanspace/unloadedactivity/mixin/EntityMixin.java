package lol.zanspace.unloadedactivity.mixin;

import lol.zanspace.unloadedactivity.interfaces.SimulateEntity;
import lol.zanspace.unloadedactivity.TimeMachine;
import lol.zanspace.unloadedactivity.UnloadedActivity;
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
import static lol.zanspace.unloadedactivity.UnloadedActivity.MOD_ID;
import static lol.zanspace.unloadedactivity.UnloadedActivity.OLD_MOD_ID;


@Mixin(value = Entity.class, priority = 999)
public abstract class EntityMixin implements Nameable, EntityAccess, CommandSource, SimulateEntity {

    @Unique
    private long lastTick = 0;

    @Shadow public Level level;
    @Inject(at = @At("HEAD"), method = "tick")
    public void tickMovement(CallbackInfo ci) {
        if (level.isClientSide())
            return;

        long currentTime = level.getDayTime();

        if (this.lastTick != 0) {

            long timeDifference = max(currentTime - this.lastTick,0);

            int differenceThreshold = UnloadedActivity.config.tickDifferenceThreshold;

            if (timeDifference > differenceThreshold)
                TimeMachine.simulateEntity((Entity)(Object)this, timeDifference);
        }
        this.lastTick = currentTime;
    }

    @Inject( at = @At("RETURN"), method = "<init>")
    private void init(EntityType<?> type, Level level, CallbackInfo ci) {
        this.lastTick = level.getDayTime();
    }

    #if MC_VER <= MC_1_21_5
    @Inject( at = @At("RETURN"), method = "saveWithoutId")
    private void save(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag returnedNbt = cir.getReturnValue();

        CompoundTag entityData = new CompoundTag();

        entityData.putLong("last_tick", this.lastTick);

        returnedNbt.put(MOD_ID, entityData);
    }
    #else
    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void save(ValueOutput nbt, CallbackInfo ci) {
        CompoundTag entityData = new CompoundTag();

        entityData.putLong("last_tick", this.lastTick);

        nbt.store(MOD_ID, CompoundTag.CODEC, entityData);
    }
    #endif

    #if MC_VER <= MC_1_21_5
    @Inject(method = "load", at = @At(value = "INVOKE", target = "net/minecraft/world/entity/Entity.readAdditionalSaveData (Lnet/minecraft/nbt/CompoundTag;)V", shift = At.Shift.AFTER))
    private void load(CompoundTag nbt, CallbackInfo ci) {
    #else
    @Inject(method = "load", at = @At(value = "INVOKE", target = "net/minecraft/world/entity/Entity.readAdditionalSaveData (Lnet/minecraft/world/level/storage/ValueInput;)V", shift = At.Shift.AFTER))
    private void load(ValueInput nbt, CallbackInfo ci) {
    #endif

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
            this.lastTick = entityData.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
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
                    this.lastTick = lastEntityTick.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
                }

                // This is so that cardinal components doesn't start sending warnings.
                cardinalData.remove("unloadedactivity:last-entity-tick");
            }
        }

        if (this.lastTick == 0) {
            this.lastTick = level.getDayTime();
        }
    }
}
