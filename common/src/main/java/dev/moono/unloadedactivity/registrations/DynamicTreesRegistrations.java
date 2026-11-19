package dev.moono.unloadedactivity.registrations;

import com.ferreusveritas.dynamictrees.DynamicTrees;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.NumberFetcherRegistry;
import dev.moono.unloadedactivity.api.SimulationMethodRegistry;
import dev.moono.unloadedactivity.api.UnloadedActivityApi;
import dev.moono.unloadedactivity.impl.number_fetchers.dynamic_trees.CanSaplingGrowNaturallyValue;
import dev.moono.unloadedactivity.impl.number_fetchers.dynamic_trees.TreeGrowthFoldingValue;
import dev.moono.unloadedactivity.impl.simulation_methods.HatchMethod;
import dev.moono.unloadedactivity.impl.simulation_methods.dynamic_trees.GrowDynamicSaplingMethod;
import dev.moono.unloadedactivity.impl.simulation_methods.dynamic_trees.GrowDynamicTreeMethod;

public class DynamicTreesRegistrations implements UnloadedActivityApi {
    @Override
    public void registerNumberFetchers(NumberFetcherRegistry registry) {
        registry.register(GameUtils.createId(DynamicTrees.MOD_ID, "can_sapling_grow_naturally"), new CanSaplingGrowNaturallyValue());
        registry.register(GameUtils.createId(DynamicTrees.MOD_ID, "tree_growth_folding"), new TreeGrowthFoldingValue());
    }

    @Override
    public void registerSimulationMethods(SimulationMethodRegistry registry) {
        registry.register(GameUtils.createId(DynamicTrees.MOD_ID, "grow_sapling"), GrowDynamicSaplingMethod::new);
        registry.register(GameUtils.createId(DynamicTrees.MOD_ID, "grow_tree"), GrowDynamicTreeMethod::new);
    }
}
