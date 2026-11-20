package dev.moono.unloadedactivity.impl.simulation_methods.dynamic_trees;

import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.network.MapSignal;
import com.ferreusveritas.dynamictrees.api.treedata.TreePart;
import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.systems.nodemapper.FindEndsNode;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import com.ferreusveritas.dynamictrees.util.SimpleVoxmap;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SeparableSimulationMethod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class GrowDynamicTreeMethod extends SeparableSimulationMethod {
    private final RootyBlock rootyBlock;

    public GrowDynamicTreeMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);

        if (block instanceof RootyBlock rooty) {
            this.rootyBlock = rooty;
        }else{
            throw new RuntimeException("The block " + block + " cannot have this simulation method.");
        }
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        int fertility = rootyBlock.getFertility(state, level, pos);
        if (fertility == 0) return 0;
        // Limit to 500 due to trees that don't spend much fertility. (Dark oak)
        return 500;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState rootyState, ServerLevel level, BlockPos rootPos, OccurrencesAndTimings occurrencesAndTimings) {
        int iter = 0;
        boolean viable = false;
        boolean first = true;

        Species species = rootyBlock.getSpecies(rootyState, level, rootPos);
        BlockPos treePos = rootPos.relative(rootyBlock.getTrunkDirection(level, rootPos));
        TreePart treeBase = TreeHelper.getTreePart(level.getBlockState(treePos));
        SimpleVoxmap leafMap = species.getLeavesProperties().getCellKit().getLeafCluster();

        if (species.isValid()) {
            if (treeBase != TreeHelper.NULL_TREE_PART) {
                viable = true;
                LongSet previousEndPositions = new LongOpenHashSet();
                RandomSource random = GameUtils.getRand(level);

                while (viable && iter < occurrencesAndTimings.occurrences()) {
                    iter++;

                    int fertility = rootyBlock.getFertility(rootyState, level, rootPos);
                    if (fertility == 0) break;

                    FindEndsNode endFinder = new FindEndsNode(); // TODO update the ends list instead of recalculating it every loop.
                    treeBase.analyse(level.getBlockState(treePos), level, treePos, null, new MapSignal(endFinder));
                    List<BlockPos> ends = endFinder.getEnds();

                    if (!first) {
                        for (var endPos : ends) {
                            if (!previousEndPositions.contains(endPos.asLong())) {
                                // Not very accurate, but the outcome is mostly fine.
                                // Dynamic leaves have their own simulations,
                                // but trying to do them properly would be very hard.
                                if (random.nextInt(4) == 1) {
                                    TreeHelper.ageVolume(
                                        level,
                                        // Try to optimize dark oak simulations by reducing the volume using Math.min
                                        endPos.below(Math.min((leafMap.getLenZ() - 1) / 2, 3)),
                                        Math.min((leafMap.getLenX() - 1) / 2, 3),
                                        Math.min(leafMap.getLenY(), 3),
                                        1,
                                        SafeChunkBounds.ANY
                                    );
                                }
                            }
                        }

                        previousEndPositions.clear();

                        for (var endPos : ends) {
                            previousEndPositions.add(endPos.asLong());
                        }
                    }

                    first = false;

                    if (species.handleRot(level, ends, rootPos, treePos, fertility, SafeChunkBounds.ANY)) {
                        viable = false;
                        break;
                    } else {
                        // Don't handle handleVoluntaryDrops.
                        // If unsimulated for a long time, it will drop a lot of items.
                        // species.handleVoluntaryDrops(level, ends, rootPos, treePos, fertility);
                        if (species.handleDisease(level, treeBase, treePos, random, fertility)) {
                            continue;
                        }

                        viable = species.grow(level, rootyBlock, rootPos, fertility, treeBase, treePos, random, true);

                        BlockState newState = level.getBlockState(rootPos);
                        if (newState.getBlock() != rootyState.getBlock()) break;
                        rootyState = newState;
                    }
                }

                if (viable) {
                    FindEndsNode endFinder = new FindEndsNode();
                    treeBase.analyse(level.getBlockState(treePos), level, treePos, null, new MapSignal(endFinder));
                    List<BlockPos> ends = endFinder.getEnds();

                    // Cleanup some weird branches
                    int fertility = rootyBlock.getFertility(rootyState, level, rootPos);
                    // We wouldn't want to clean up branches if the tree is still growing.
                    if (fertility == 0) {
                        if (species.handleRot(level, ends, rootPos, treePos, 0, SafeChunkBounds.ANY)) {
                            viable = false;
                        }
                    }

                    // Cleanup the leaves shape + remove stray leaves that sometimes appear.
                    TreeHelper.ageVolume(level, rootPos, 8, 32, 1, SafeChunkBounds.ANY);

                    // Cleanup the leaves shape a bit more.
                    for (var endPos : ends) {
                        TreeHelper.ageVolume(level, endPos.below((leafMap.getLenZ() - 1) / 2), (leafMap.getLenX() - 1) / 2, leafMap.getLenY(), 2, SafeChunkBounds.ANY);
                    }
                }
            }
        }

        if (!viable) {
            level.setBlock(rootPos, rootyBlock.getDecayBlockState(rootyState, level, rootPos), 3);
        }

        return null;
    }

    @Override
    public boolean isDependable() {
        return false;
    }
}
