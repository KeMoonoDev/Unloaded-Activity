package dev.moono.unloadedactivity.impl.number_fetchers.dynamic_trees;

import com.ferreusveritas.dynamictrees.init.DTConfigs;
import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;
import net.minecraft.world.level.block.Block;
import vectorwing.farmersdelight.common.Configuration;
import vectorwing.farmersdelight.common.registry.ModBlocks;

import java.util.function.Supplier;


@SuppressWarnings("unchecked")
public class TreeGrowthFoldingValue implements FixedNumberFetcher {

    private final Supplier<Integer> treeGrowthFolding;

    public TreeGrowthFoldingValue() {
        try {
            this.treeGrowthFolding = (Supplier<Integer>) DTConfigs.class.getField("TREE_GROWTH_FOLDING").get(null);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public Number evaluate(FixedContext context) {
        return treeGrowthFolding.get();
    }
}
