package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.*;
import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.OccurrencesAndDuration;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GrowBambooMethod extends SimulationMethod {
    public final int maxHeight;
    public GrowBambooMethod(SimulationConfig config) {
        super(config);
        this.maxHeight = config.getNumber("max_height").intValue();
    }

    private int countAirAboveUpToMax(BlockGetter blockGetter, BlockPos pos, int maxCount) {
        int i;
        for (i = 0; i < maxCount && blockGetter.getBlockState(pos.above(i + 1)).isAir(); ++i) {
        }
        return i;
    }

    @Override
    public boolean canDoMore(BlockState state, ServerLevel level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public @Nullable DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, long timePassed, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (maxHeight <= 1 || !level.isEmptyBlock(pos.above())) return null;

        Block thisBlock = state.getBlock();
        if (thisBlock instanceof BambooSaplingBlock bambooSaplingBlock) {

            OccurrencesAndDuration result = MathUtils.getOccurrences(level, state, pos, GameUtils.getTime(level), timePassed, this.advanceProbability, this.requiresRain, 1, randomPickOdds, true, random, groupSimulateData);

            if (result.occurrences() != 0) {
                bambooSaplingBlock.growBamboo(level, pos);

                long newTimePassed = timePassed - result.duration();
                if (newTimePassed > 0) {
                    BlockState lastState = level.getBlockState(pos);
                    BlockPos lastPos = pos;
                    Block searchBlock = lastState.getBlock();
                    if (searchBlock.equals(thisBlock)) return null;

                    for(int i=1; i<maxHeight; i++) {
                        BlockState aboveState = level.getBlockState(pos.above(i));
                        if (!searchBlock.equals(aboveState.getBlock())) break;
                        lastState = aboveState;
                        lastPos = pos.above(i);
                    }

                    this.simulate(lastState, level, lastPos, random, newTimePassed, randomPickOdds, hasDependents, groupSimulateData);
                }
            }
        } else if (thisBlock instanceof #if MC_VER >= MC_1_19_4 BambooStalkBlock #else BambooBlock #endif bambooBlock) {
            int height = bambooBlock.getHeightBelowUpToMax(level, pos);

            if (height >= maxHeight)
                return DeferredBlockPlacer.empty();

            int heightDifference = maxHeight - height;
            int maxGrowth = this.countAirAboveUpToMax(level,pos, heightDifference);

            OccurrencesAndDuration result = MathUtils.getOccurrences(level, state, pos, GameUtils.getTime(level), timePassed, this.advanceProbability, this.requiresRain, maxGrowth, randomPickOdds, false, random, groupSimulateData);

            int totalGrowth = 0;

            for(int i=0;i<result.occurrences();i++) {
                bambooBlock.performBonemeal(level, random, pos, state);

                int grew = bambooBlock.getHeightAboveUpToMax(level, pos);

                totalGrowth += grew;

                pos = pos.above(grew);
                state = level.getBlockState(pos);

                if (!this.canDoMore(state, level, pos)) {
                    return null;
                }

                // If it has successfully grown, the isPropertyFinished check should've passed.
                // The following checks are failed growths and doesn't need to calculate the duration.

                if (totalGrowth >= maxGrowth) return null;

                if (i + 1 == result.occurrences()) return null;

                if (!this.canSimulate(state, level, pos)) return null;
            }
        }
        return null;
    }
}
