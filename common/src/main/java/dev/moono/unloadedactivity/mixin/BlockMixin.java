package dev.moono.unloadedactivity.mixin;

import dev.moono.unloadedactivity.interfaces.SimulateChunkBlocks;

#if MC_VER >= MC_1_21_11
#else
#endif

import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixin implements SimulateChunkBlocks {
}