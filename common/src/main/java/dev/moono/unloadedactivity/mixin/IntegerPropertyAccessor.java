package dev.moono.unloadedactivity.mixin;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IntegerProperty.class)
public interface IntegerPropertyAccessor {
    @Accessor("max")
    int unloaded_activity$getMax();
}
