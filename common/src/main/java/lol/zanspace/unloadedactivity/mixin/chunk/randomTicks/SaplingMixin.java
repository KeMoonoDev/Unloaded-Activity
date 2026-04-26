package lol.zanspace.unloadedactivity.mixin.chunk.randomTicks;

import lol.zanspace.unloadedactivity.ActiveGroupSimulateData;
import lol.zanspace.unloadedactivity.OccurrencesAndDuration;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.Utils;
import lol.zanspace.unloadedactivity.datapack.SimulateProperty;
import lol.zanspace.unloadedactivity.datapack.SimulationData;
import lol.zanspace.unloadedactivity.datapack.SimulationType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
#if MC_VER >= MC_1_20_4
import net.minecraft.world.level.block.grower.TreeGrower;
#else
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
#endif
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

import static java.lang.Math.*;

@Mixin(SaplingBlock.class)
public abstract class SaplingMixin extends #if MC_VER >= MC_1_21_5 VegetationBlock #else BushBlock #endif {

    protected SaplingMixin(Properties properties) {
        super(properties);
    }

    #if MC_VER >= MC_1_20_4
    @Shadow @Final private TreeGrower treeGrower;
    #else
    @Shadow @Final private AbstractTreeGrower treeGrower;
    #endif

    @Override
    public boolean isPropertyFinished(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty) {
        if (simulateProperty.isAction("grow_tree")) {
            return false;
        }

        return super.isPropertyFinished(state, level, pos, simulateProperty);
    }

    @Override
    public @Nullable Triple<BlockState, OccurrencesAndDuration, BlockPos> simulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty, RandomSource random, long timePassed, double randomPickOdds, boolean calculateDuration, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (simulateProperty.isAction("grow_tree")) {

            OccurrencesAndDuration result = Utils.getOccurrences(level, state, pos, level.getDayTime(), timePassed, simulateProperty, 1, randomPickOdds, calculateDuration, random, groupSimulateData);

            if (result.occurrences() == 0)
                return Triple.of(state, result, pos);

            this.treeGrower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);

            return null;
        }
        return super.simulateProperty(state, level, pos, simulateProperty, random, timePassed, randomPickOdds, calculateDuration, groupSimulateData);
    }
}