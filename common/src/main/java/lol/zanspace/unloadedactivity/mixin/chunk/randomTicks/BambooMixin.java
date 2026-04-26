package lol.zanspace.unloadedactivity.mixin.chunk.randomTicks;

import lol.zanspace.unloadedactivity.ActiveGroupSimulateData;
import lol.zanspace.unloadedactivity.OccurrencesAndDuration;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.Utils;
import lol.zanspace.unloadedactivity.datapack.SimulateProperty;
import lol.zanspace.unloadedactivity.datapack.SimulationData;
import lol.zanspace.unloadedactivity.datapack.SimulationType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

#if MC_VER >= MC_1_19_4
import net.minecraft.world.level.block.BambooStalkBlock;
#else
import net.minecraft.world.level.block.BambooBlock;
#endif

#if MC_VER >= MC_1_19_4
@Mixin(BambooStalkBlock.class)
#else
@Mixin(BambooBlock.class)
#endif
public abstract class BambooMixin extends Block implements BonemealableBlock {

    public BambooMixin(Properties properties) {
        super(properties);
    }

    @Shadow @Final public static IntegerProperty STAGE;

    @Override
    public boolean isPropertyFinished(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty) {
        if (simulateProperty.isAction("grow_bamboo")) {
            return state.getValue(STAGE) == 1;
        }
        return super.isPropertyFinished(state, level, pos, simulateProperty);
    }

    @Shadow
    protected int getHeightBelowUpToMax(BlockGetter blockGetter, BlockPos pos) {
        return 0;
    }

    @Shadow protected abstract int getHeightAboveUpToMax(BlockGetter blockGetter, BlockPos pos);

    @Unique
    private int countAirAboveUpToMax(BlockGetter blockGetter, BlockPos pos, int maxCount) {
        int i;
        for (i = 0; i < maxCount && blockGetter.getBlockState(pos.above(i + 1)).isAir(); ++i) {
        }
        return i;
    }

    @Override
    public @Nullable Triple<BlockState, OccurrencesAndDuration, BlockPos> simulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty, RandomSource random, long timePassed, double randomPickOdds, boolean calculateDuration, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (simulateProperty.isAction("grow_bamboo")) {
            int height = getHeightBelowUpToMax(level, pos);

            int maxHeight = simulateProperty.maxHeight.orElse(15);

            if (height >= maxHeight)
                return Triple.of(state, OccurrencesAndDuration.empty(), pos);

            int heightDifference = maxHeight - height;
            int maxGrowth = this.countAirAboveUpToMax(level,pos, heightDifference);

            OccurrencesAndDuration result = Utils.getOccurrences(level, state, pos, level.getDayTime(), timePassed, simulateProperty, maxGrowth, randomPickOdds, false, random, groupSimulateData);

            int totalGrowth = 0;

            for(int i=0;i<result.occurrences();i++) {
                this.performBonemeal(level, random, pos, state);

                int grew = this.getHeightAboveUpToMax(level, pos);

                totalGrowth += grew;

                pos = pos.above(grew);
                state = level.getBlockState(pos);

                boolean blockChanged = state.getBlock() != this;

                if (blockChanged || this.isPropertyFinished(state, level, pos, simulateProperty)) {
                    if (blockChanged || calculateDuration) {
                        return Triple.of(state, OccurrencesAndDuration.recalculatedDuration(i+1, timePassed, result.averageProbability(), random), pos);
                    } else {
                        return Triple.of(state, result, pos);
                    }
                }

                // If it has successfully grown, the isPropertyFinished check should've passed.
                // The following checks are failed growths and doesn't need to calculate the duration.

                if (totalGrowth >= maxGrowth) {
                    return Triple.of(state, result, pos);
                }

                if (i + 1 == result.occurrences()) {
                    return Triple.of(state, result, pos);
                }

                if (!this.canSimulateProperty(state, level, pos, simulateProperty)) {
                    return Triple.of(state, result, pos);
                }
            }
            return Triple.of(state, result, pos);
        }
        return super.simulateProperty(state, level, pos, simulateProperty, random, timePassed, randomPickOdds, calculateDuration, groupSimulateData);
    }
}