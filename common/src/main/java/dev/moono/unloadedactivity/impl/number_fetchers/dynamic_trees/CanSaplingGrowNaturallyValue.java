package dev.moono.unloadedactivity.impl.number_fetchers.dynamic_trees;

#if MC_VER >= MC_1_21_1
import com.dtteam.dynamictrees.block.sapling.DynamicSaplingBlock;
import com.dtteam.dynamictrees.tree.species.Species;
#else
import com.ferreusveritas.dynamictrees.block.DynamicSaplingBlock;
import com.ferreusveritas.dynamictrees.tree.species.Species;
#endif

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Method;

public class CanSaplingGrowNaturallyValue implements FixedNumberFetcher {
    private final Method getSpecies;

    public CanSaplingGrowNaturallyValue() {
        try {
            this.getSpecies = DynamicSaplingBlock.class.getMethod("getSpecies"); // Method is public since 1.21.1
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Number evaluate(FixedContext context) {
        Block block = context.getBlockState().getBlock();
        if (block instanceof DynamicSaplingBlock) {
            Species species;
            try {
                species = (Species)this.getSpecies.invoke(block);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            boolean result = species.canSaplingGrowNaturally(context.getServerLevel(), context.getBlockPos());
            return result ? 1 : 0;
        }
        return 0;
    }
}
