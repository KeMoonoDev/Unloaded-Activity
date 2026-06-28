package dev.moono.unloadedactivity.impl.simulation_methods;

#if MC_VER >= MC_1_21_11
import net.minecraft.world.attribute.EnvironmentAttributes;
#endif

import dev.moono.unloadedactivity.*;
import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class SpeleothemMethod extends SimulationMethod {
    @Nullable public final CauldronFillConfig cauldronFillConfig;

    public record CauldronFillConfig(int maxDistanceBetweenTipAndCauldron, float waterFillProbability, float lavaFillProbability) {
        public CauldronFillConfig(SimulationConfig config) {
            this(
                config.getNumber("max_distance_between_tip_and_cauldron").intValue(),
                config.getNumber("water_fill_probability").floatValue(),
                config.getNumber("lava_fill_probability").floatValue()
            );
        }

        private float getCauldronDripOdds(Fluid fluid) {
            if (fluid == Fluids.WATER) {
                return this.waterFillProbability;
            } else if (fluid == Fluids.LAVA) {
                return this.lavaFillProbability;
            }
            return 0.0f;
        }
    }

    public SpeleothemMethod(SimulationConfig config) {
        super(config);
        SimulationConfig cauldronFillConfig = config.getConfigNullable("cauldron_fill");
        this.cauldronFillConfig = cauldronFillConfig != null ? new CauldronFillConfig(cauldronFillConfig) : null;
    }

    public int getMaxGrowthLength(#if MC_VER >= MC_26_2 SpeleothemBlock thisBlock #else PointedDripstoneBlock thisBlock #endif) {
        #if MC_VER >= MC_26_2
        return thisBlock.getMaxGrowthLength();
        #else
        return PointedDripstoneBlock.MAX_GROWTH_LENGTH;
        #endif
    }

    public int getBottomSearchRange() {
        #if MC_VER >= MC_26_2
        return SpeleothemBlock.MAX_STALAGMITE_SEARCH_RANGE_WHEN_GROWING;
        #else
        return PointedDripstoneBlock.MAX_STALAGMITE_SEARCH_RANGE_WHEN_GROWING;
        #endif
    }

    private int getBottomStalagmiteDistance(ServerLevel level, BlockPos tipPos, int currentLength, #if MC_VER >= MC_26_2 SpeleothemBlock thisBlock #else PointedDripstoneBlock thisBlock #endif) {
        BlockPos.MutableBlockPos mutable = tipPos.mutable();

        int potentialGrowth = getMaxGrowthLength(thisBlock)-currentLength;
        int maxPossibleReach = getBottomSearchRange()+potentialGrowth;

        for(int i = 0; i < maxPossibleReach; ++i) {
            mutable.move(Direction.DOWN);
            BlockState blockState = level.getBlockState(mutable);
            if (!blockState.getFluidState().isEmpty()) {
                return -1;
            }

            if (GameUtils.isUnmergedTipWithDirection(blockState, Direction.UP, thisBlock) && GameUtils.canTipGrow(blockState, level, mutable, thisBlock)) {
                return i+1;
            }

            if (GameUtils.isValidSpeleothemPlacement(level, mutable, Direction.UP, thisBlock) && !level.isWaterAt(mutable.below())) {
                return i+1;
            }

            if (GameUtils.blocksStalagmiteScan(level, mutable, blockState, thisBlock)) {
                return -1;
            }
        }
        return -1;
    }

    private @Nullable BlockPos getExtendedCauldronPos(Level world, BlockPos pos, Fluid fluid) {
        if (this.cauldronFillConfig == null) return null;
        BlockPos cauldronPos = PointedDripstoneBlock.findFillableCauldronBelowStalactiteTip(world, pos, fluid);
        if (cauldronPos == null) {
            cauldronPos = PointedDripstoneBlock.findFillableCauldronBelowStalactiteTip(world, pos.below(this.cauldronFillConfig.maxDistanceBetweenTipAndCauldron()-1), fluid);
        }
        return cauldronPos;
    }

    @Override
    public boolean canDoMore(BlockState state, ServerLevel level, BlockPos pos) {
        return GameUtils.isStalactiteStartPos(state, level, pos);
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public @Nullable DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, SimulatedTime simulatedTime, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData) {
        #if MC_VER >= MC_26_2
        SpeleothemBlock thisBlock;
        Block gotBlock = state.getBlock();
        if (gotBlock instanceof SpeleothemBlock speleothemBlock) {
            thisBlock = speleothemBlock;
        } else {
            return DeferredBlockPlacer.empty();
        }
        #else
        PointedDripstoneBlock thisBlock;
        Block gotBlock = state.getBlock();
        if (gotBlock instanceof PointedDripstoneBlock pointedDripstoneBlock) {
            thisBlock = pointedDripstoneBlock;
        } else {
            return DeferredBlockPlacer.empty();
        }
        #endif

        int maxGrowthLength = getMaxGrowthLength(thisBlock);

        BlockPos tipPos = GameUtils.findTip(state, level, pos, 12, false);

        if (tipPos == null)
            return DeferredBlockPlacer.empty();

        BlockState tip = level.getBlockState(tipPos);

        BlockState liquidState = level.getBlockState(pos.above(2));

        int currentLength = pos.getY()-tipPos.getY();
        int lengthDifference = maxGrowthLength-currentLength;
        int bottomStalagmiteDistance = getBottomStalagmiteDistance(level, tipPos, currentLength, thisBlock);

        Fluid dripstoneFluid = liquidState.getFluidState().getType();

        BlockPos cauldronPos = getExtendedCauldronPos(level, tipPos, dripstoneFluid);

        int cauldronGroundDistance = -1;

        if (cauldronPos != null)
            cauldronGroundDistance = tipPos.getY() - cauldronPos.getY();

        int totalUpperDripGrowth = 0;
        int totalLowerDripGrowth = 0;

        int bottomSearchRange = this.getBottomSearchRange();

        int successesUntilReachGround = Math.max(bottomStalagmiteDistance-bottomSearchRange, 0);
        int successesUntilReachCauldron;

        if (this.cauldronFillConfig != null) {
            successesUntilReachCauldron = Math.max(cauldronGroundDistance-this.cauldronFillConfig.maxDistanceBetweenTipAndCauldron(), 0);
        } else {
            successesUntilReachCauldron = 0;
        }

        OccurrencesAndTimings upperResult = OccurrencesAndTimings.empty(simulatedTime);

        if (currentLength < maxGrowthLength) {
            if (GameUtils.canGrow(level, pos, thisBlock)) {
                if (GameUtils.isFreeHangingStalactite(tip) && GameUtils.canTipGrow(tip, level, tipPos, thisBlock)) {

                    boolean calculateDuration = successesUntilReachGround > 0 || successesUntilReachCauldron > 0;

                    upperResult = MathUtils.getOccurrences(level, state, pos, simulatedTime, this.advanceProbability, this.requiresRain, lengthDifference, randomPickOdds, calculateDuration, random, groupSimulateData);
                    totalUpperDripGrowth = upperResult.occurrences();

                    if (bottomStalagmiteDistance != -1) {
                        if (totalUpperDripGrowth >= successesUntilReachGround) {
                            SimulatedTime newTime = upperResult.getTimeAtOccurrence(successesUntilReachGround);
                            int maxGroundGrowth = Math.min(bottomStalagmiteDistance, bottomSearchRange);
                            var lowerResult = MathUtils.getOccurrences(level, state, pos, newTime, this.advanceProbability, this.requiresRain, maxGroundGrowth, randomPickOdds, false, random, groupSimulateData);
                            totalLowerDripGrowth = lowerResult.occurrences();
                        }
                    }
                }
            }
        }

        if (this.cauldronFillConfig != null) {
            // todo seperate this into a different config
            boolean ultraWarm = #if MC_VER >= MC_1_21_11
                level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)
            #else
                level.dimensionType().ultraWarm()
            #endif;

            if (liquidState.is(Blocks.MUD) && !ultraWarm) {
                float totalDripOdds = this.cauldronFillConfig.waterFillProbability * randomPickOdds;
                int dripOccurrences = MathUtils.getOccurrencesBinomial(simulatedTime.remainingTime(), totalDripOdds, 1, random);
                if (dripOccurrences != 0) {
                    BlockState clay = Blocks.CLAY.defaultBlockState();
                    level.setBlockAndUpdate(pos.above(2), clay);
                    Block.pushEntitiesUp(liquidState, clay, level, pos.above(2));
                }
            }

            if (cauldronPos != null && totalUpperDripGrowth >= successesUntilReachCauldron) {
                BlockState cauldronState = level.getBlockState(cauldronPos);
                if (cauldronState.getBlock() instanceof AbstractCauldronBlock cauldronBlock) {

                    if (!cauldronBlock.isFull(cauldronState) && cauldronBlock.canReceiveStalactiteDrip(dripstoneFluid)) {
                        float totalDripOdds = this.cauldronFillConfig.getCauldronDripOdds(dripstoneFluid) * randomPickOdds;

                        SimulatedTime newTime = upperResult.getTimeAtOccurrence(successesUntilReachCauldron);

                        int dripOccurrences = MathUtils.getOccurrencesBinomial(newTime.remainingTime(), totalDripOdds, LayeredCauldronBlock.MAX_FILL_LEVEL, random);
                        while (dripOccurrences > 0) {
                            --dripOccurrences;
                            cauldronBlock.receiveStalactiteDrip(cauldronState, level, cauldronPos, dripstoneFluid);

                            //The block has changed and so the state and invoker needs updating.
                            cauldronState = level.getBlockState(cauldronPos);
                            cauldronBlock = (AbstractCauldronBlock) cauldronState.getBlock();
                        }
                    }
                }
            }
        }

        while (successesUntilReachGround > 0 && totalUpperDripGrowth > 0) {
            --successesUntilReachGround;
            --totalUpperDripGrowth;
            thisBlock.grow(level, tipPos, Direction.DOWN);
            //recalculate tip so if tryGrow fails, we wont grow past any blocking blocks. Simply doing tipPos.down() doesn't account for fail.
            tipPos = GameUtils.findTip(level.getBlockState(pos), level, pos, 12, false);
            if (tipPos == null) return null;
        }

        while (totalUpperDripGrowth+totalLowerDripGrowth > 0) {
            if (totalUpperDripGrowth == 0) {

                if (pos.getY()-tipPos.getY() >= maxGrowthLength)
                    return null;

                --totalLowerDripGrowth;
                thisBlock.growStalagmiteBelow(level, tipPos);
            } else if (totalLowerDripGrowth == 0) {
                --totalUpperDripGrowth;
                thisBlock.grow(level, tipPos, Direction.DOWN);
                //recalculate tip so if tryGrow fails, we wont grow past any blocking blocks. Simply doing tipPos.down() doesnt account for fail.
                tipPos = GameUtils.findTip(level.getBlockState(pos), level, pos, 12, false);
                if (tipPos == null) return null;

            } else if (random.nextBoolean()) {
                --totalLowerDripGrowth;
                thisBlock.growStalagmiteBelow(level, tipPos);
            } else {
                --totalUpperDripGrowth;
                thisBlock.grow(level, tipPos, Direction.DOWN);
                //recalculate tip so if tryGrow fails, we wont grow past any blocking blocks. Simply doing tipPos.down() doesnt account for fail.
                tipPos = GameUtils.findTip(level.getBlockState(pos), level, pos, 12, false);
                if (tipPos == null) return null;
            }
        }

        return null;
    }
}
