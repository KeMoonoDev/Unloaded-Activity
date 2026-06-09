package dev.moono.unloadedactivity.neoforge.mixin;

import net.minecraft.world.level.block.CropBlock;
import org.spongepowered.asm.mixin.Mixin;

#if MC_VER >= MC_1_21_1
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CropBlock.class)
public interface CropBlockInvoker {
    @Invoker("getGrowthSpeed")
    public static float invokeGetGrowthSpeed(BlockState blockState, BlockGetter blockGetter, BlockPos pos) {
        throw new AssertionError();
    }
}
#else
// An empty mixin to the air block.
import net.minecraft.world.level.block.AirBlock;
@Mixin(AirBlock.class)
public class CropBlockInvoker {}
#endif
