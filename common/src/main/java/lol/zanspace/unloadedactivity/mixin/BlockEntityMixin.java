package lol.zanspace.unloadedactivity.mixin;

import lol.zanspace.unloadedactivity.GameUtils;
import lol.zanspace.unloadedactivity.interfaces.BlockEntityTimeData;
import lol.zanspace.unloadedactivity.interfaces.SimulateBlockEntity;
import lol.zanspace.unloadedactivity.UnloadedActivity;
#if MC_VER > MC_1_20_4
import net.minecraft.core.HolderLookup;
#endif
#if MC_VER > MC_1_21_5
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
#endif
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static lol.zanspace.unloadedactivity.UnloadedActivity.MOD_ID;
import static lol.zanspace.unloadedactivity.UnloadedActivity.OLD_MOD_ID;

@Mixin(value = BlockEntity.class, priority = 999)
public abstract class BlockEntityMixin implements SimulateBlockEntity, BlockEntityTimeData {
    @Unique
    private long lastTick = 0;

    @Override
    public long getLastTick() {
        return this.lastTick;
    }

    @Override
    public void setLastTick(long tick) {
        this.lastTick = tick;
    }

    @Shadow
    public Level getLevel() {
        return null;
    }

    @Shadow
    public boolean hasLevel() {
        return true;
    }

    #if MC_VER >= MC_26_1_2
    @Inject(method = {"saveWithoutMetadata(Lnet/minecraft/world/level/storage/ValueOutput;)V", "saveCustomOnly(Lnet/minecraft/world/level/storage/ValueOutput;)V"}, at = @At("RETURN"))
    private void save(ValueOutput nbt, CallbackInfo ci)
    #elif MC_VER >= MC_1_21_8
    @Inject(method = {"saveWithoutMetadata(Lnet/minecraft/world/level/storage/ValueOutput;)V", "saveCustomOnly(Lnet/minecraft/world/level/storage/ValueOutput;)V"}, at = @At("RETURN"))
    private void save(ValueOutput nbt, CallbackInfo ci)
    #else
    @Inject(method = "saveAdditional", at = @At("RETURN"))
        #if MC_VER <= MC_1_20_4
        private void save(CompoundTag nbt, CallbackInfo ci)
        #else
        private void save(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci)
        #endif
    #endif
    {
        CompoundTag blockData = new CompoundTag();

        blockData.putLong("last_tick", this.lastTick);

        #if MC_VER <= MC_1_21_5
        nbt.put(MOD_ID, blockData);
        #else
        nbt.store(MOD_ID, CompoundTag.CODEC, blockData);
        #endif
    }
    #if MC_VER <= MC_1_20_4
    @Inject(method = "load", at = @At(value = "RETURN"))
    private void load(CompoundTag nbt, CallbackInfo ci)
    #elif MC_VER <= MC_1_21_5
    @Inject(method = "loadAdditional", at = @At(value = "RETURN"))
    private void load(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci)
    #else
    @Inject(method = "loadWithComponents", at = @At("RETURN"))
    private void load(ValueInput nbt, CallbackInfo ci)
    #endif
    {
        #if MC_VER <= MC_1_21_5
        CompoundTag blockData = nbt.getCompound(MOD_ID)#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
        #else
        CompoundTag blockData = nbt.read(MOD_ID, CompoundTag.CODEC).orElse(new CompoundTag());
        #endif

        if (blockData.isEmpty()) {
            #if MC_VER <= MC_1_21_5
            blockData = nbt.getCompound(OLD_MOD_ID)#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
            #else
            blockData = nbt.read(OLD_MOD_ID, CompoundTag.CODEC).orElse(new CompoundTag());
            #endif
        }

        boolean isEmpty = blockData.isEmpty();

        if (!isEmpty) {
            this.lastTick = blockData.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
        }

        if (UnloadedActivity.config.convertCCAData && isEmpty) {
            #if MC_VER <= MC_1_21_5
            CompoundTag cardinalData = nbt.getCompound("cardinal_components")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
            #else
            CompoundTag cardinalData = nbt.read("cardinal_components", CompoundTag.CODEC).orElse(new CompoundTag());
            #endif

            if (!cardinalData.isEmpty()) {
                CompoundTag lastEntityTick = cardinalData.getCompound("unloadedactivity:last-blockentity-tick")#if MC_VER >= MC_1_21_5 .orElse(new CompoundTag())#endif;
                if (!lastEntityTick.isEmpty()) {
                    this.lastTick = lastEntityTick.getLong("last_tick")#if MC_VER >= MC_1_21_5 .orElse(0L) #endif;
                }

                // This is so that cardinal components doesn't start sending warnings.
                cardinalData.remove("unloadedactivity:last-blockentity-tick");
            }
        }

        if (this.lastTick == 0 && this.hasLevel()) {
            this.lastTick = GameUtils.getTime(this.getLevel());
        }
    }
}