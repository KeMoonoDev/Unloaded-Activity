package dev.moono.unloadedactivity.mixin;

import net.minecraft.world.level.block.CauldronBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CauldronBlock.class)
public interface CauldronBlockInvoker {
    @Accessor("RAIN_FILL_CHANCE")
    public static float getRainFillChance() {
        throw new AssertionError();
    }

    @Accessor("POWDER_SNOW_FILL_CHANCE")
    public static float getSnowFillChance() {
        throw new AssertionError();
    }
}
