package lol.zanspace.unloadedactivity.mixin;

import lol.zanspace.unloadedactivity.GameUtils;
import lol.zanspace.unloadedactivity.datapack.IncompleteSimulationData;
import lol.zanspace.unloadedactivity.datapack.SimulationData;
import lol.zanspace.unloadedactivity.datapack.SimulationDataResource;
import lol.zanspace.unloadedactivity.interfaces.SimulateChunkBlocks;
import net.minecraft.core.Holder;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
#endif

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;

@Mixin(Block.class)
public abstract class BlockMixin implements SimulateChunkBlocks {
}