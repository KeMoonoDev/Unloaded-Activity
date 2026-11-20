package dev.moono.unloadedactivity.impl.number_fetchers.dynamic_trees;

#if MC_VER >= MC_1_21_1
import com.dtteam.dynamictrees.config.DTConfigs;
#else
import com.ferreusveritas.dynamictrees.init.DTConfigs;
#endif

import dev.moono.unloadedactivity.api.context.FixedContext;
import dev.moono.unloadedactivity.api.number_fetcher.FixedNumberFetcher;

import java.util.function.Supplier;


@SuppressWarnings("unchecked")
public class TreeGrowthProbabilityValue implements FixedNumberFetcher {
    #if MC_VER >= MC_1_21_1
    private final Supplier<Double> treeGrowthMultiplier;

    public TreeGrowthProbabilityValue() {
        try {
            this.treeGrowthMultiplier = (Supplier<Double>) DTConfigs.class.getField("treeGrowthMultiplier").get(DTConfigs.SERVER);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public Number evaluate(FixedContext context) {
        return Math.min(treeGrowthMultiplier.get(), 1.0);
    }
    #else
    private final Supplier<Integer> treeGrowthFolding;

    public TreeGrowthProbabilityValue() {
        try {
            this.treeGrowthFolding = (Supplier<Integer>) DTConfigs.class.getField("TREE_GROWTH_FOLDING").get(null);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public Number evaluate(FixedContext context) {
        return 1f / (float)treeGrowthFolding.get();
    }
    #endif
}
