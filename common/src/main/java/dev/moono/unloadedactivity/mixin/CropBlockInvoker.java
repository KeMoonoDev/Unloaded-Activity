package dev.moono.unloadedactivity.mixin;

import org.spongepowered.asm.mixin.Mixin;

#if MC_VER < MC_1_21_1
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CropBlock.class)
public interface CropBlockInvoker {
    @Invoker("getGrowthSpeed")
    public static float invokeGetGrowthSpeed(Block block, BlockGetter blockGetter, BlockPos pos) {
        throw new AssertionError();
    }
}
#else
// An empty mixin to the air block for 1.20 and above because we need a different mixin for neoforge.
// the Block parameter got replaced with a BlockState parameter.
import net.minecraft.world.level.block.AirBlock;
@Mixin(AirBlock.class)
public class CropBlockInvoker {}
#endif
