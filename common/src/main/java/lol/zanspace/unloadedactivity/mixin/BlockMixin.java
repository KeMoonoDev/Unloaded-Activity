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

    @Shadow @Final private Holder.Reference<Block> builtInRegistryHolder;

    @Override
    public SimulationData getSimulationData() {
        return SimulationDataResource.COMPLETE_BLOCK_MAP.computeIfAbsent(GameUtils.getBlockIntId((Block)(Object)this), (blockIntId) -> {
            var blockId = GameUtils.getBlockId((Block)(Object)this);
            IncompleteSimulationData blockSimulationData = SimulationDataResource.BLOCK_MAP.get(blockId);

            IncompleteSimulationData finalSimulationData = new IncompleteSimulationData();

            for (Iterator<TagKey<Block>> it = builtInRegistryHolder.tags().iterator(); it.hasNext(); ) {
                TagKey<Block> tag = it.next();
                var tagId = tag.location();

                IncompleteSimulationData tagSimulationData = SimulationDataResource.TAG_MAP.get(tagId);

                if (tagSimulationData != null) {
                    finalSimulationData.merge(tagSimulationData);
                }
            }

            if (blockSimulationData != null) {
                finalSimulationData.merge(blockSimulationData);
            }

            try {
                SimulationData simulationData = new SimulationData(finalSimulationData);
                return simulationData;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create SimulationData for " + blockId + ".\n" + e.getMessage());
            }
        });
    }
}