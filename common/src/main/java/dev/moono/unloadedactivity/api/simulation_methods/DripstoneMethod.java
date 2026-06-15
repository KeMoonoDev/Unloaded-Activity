package dev.moono.unloadedactivity.api.simulation_methods;

import dev.moono.unloadedactivity.*;
import dev.moono.unloadedactivity.*;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.SimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class DripstoneMethod extends SimulationMethod {
    public final int maxBottomGrowthSearchLength;
    public final int maxGrowthLength;
    public final int maxDistanceBetweenTipAndCauldron;
    public final float waterFillProbability;
    public final float lavaFillProbability;

    public DripstoneMethod(SimulationConfig config) {
        super(config);
        this.maxBottomGrowthSearchLength = config.getNumber("max_bottom_growth_search_length").intValue();
        this.maxGrowthLength = config.getNumber("max_growth_length").intValue();
        this.maxDistanceBetweenTipAndCauldron = config.getNumber("max_distance_between_tip_and_cauldron").intValue();
        this.waterFillProbability = config.getNumber("water_fill_probability").floatValue();
        this.lavaFillProbability = config.getNumber("lava_fill_probability").floatValue();
    }

    private int getStalagmiteGrowthDistance(ServerLevel level, BlockPos tipPos) {
        BlockPos.MutableBlockPos mutable = tipPos.mutable();

        for(int i = 0; i < maxBottomGrowthSearchLength+maxGrowthLength; ++i) {
            mutable.move(Direction.DOWN);
            BlockState blockState = level.getBlockState(mutable);
            if (!blockState.getFluidState().isEmpty()) {
                return -1;
            }

            if (PointedDripstoneBlock.isUnmergedTipWithDirection(blockState, Direction.UP) && PointedDripstoneBlock.canTipGrow(blockState, level, mutable)) {
                return i+1;
            }

            if (PointedDripstoneBlock.isValidPointedDripstonePlacement(level, mutable, Direction.UP) && !level.isWaterAt(mutable.below())) {
                return i+1;
            }

            if (!PointedDripstoneBlock.canDripThrough(level, mutable, blockState)) {
                return -1;
            }
        }
        return -1;
    }

    private BlockPos getExtendedCauldronPos(Level world, BlockPos pos, Fluid fluid) {
        BlockPos cauldronPos = PointedDripstoneBlock.findFillableCauldronBelowStalactiteTip(world, pos, fluid);
        if (cauldronPos == null) {
            cauldronPos = PointedDripstoneBlock.findFillableCauldronBelowStalactiteTip(world, pos.below(maxDistanceBetweenTipAndCauldron-1), fluid);
        }
        return cauldronPos;
    }

    private float getCauldronDripOdds(Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return this.waterFillProbability;
        } else if (fluid == Fluids.LAVA) {
            return this.lavaFillProbability;
        }
        return 0.0f;
    }

    @Override
    public boolean canDoMore(BlockState state, ServerLevel level, BlockPos pos) {
        return PointedDripstoneBlock.isStalactiteStartPos(state, level, pos);
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public @Nullable DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, long timePassed, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData) {
        BlockPos tipPos = PointedDripstoneBlock.findTip(state, level, pos, 12, false);

        if (tipPos == null)
            return DeferredBlockPlacer.empty();

        BlockState tip = level.getBlockState(tipPos);

        BlockState dripstoneBlockState = level.getBlockState(pos.above(1));
        BlockState liquidState = level.getBlockState(pos.above(2));

        int currentLength = pos.getY()-tipPos.getY();
        int lengthDifference = this.maxGrowthLength-currentLength;
        int stalagmiteGroundDistance = getStalagmiteGrowthDistance(level, tipPos);

        Fluid dripstoneFluid = liquidState.getFluidState().getType();

        BlockPos cauldronPos = getExtendedCauldronPos(level, tipPos, dripstoneFluid);

        int cauldronGroundDistance = -1;

        if (cauldronPos != null)
            cauldronGroundDistance = tipPos.getY() - cauldronPos.getY();

        int totalUpperDripGrowth = 0;
        int totalLowerDripGrowth = 0;

        int successesUntilReachGround = Math.max(stalagmiteGroundDistance-maxBottomGrowthSearchLength, 0);
        int successesUntilReachCauldron = Math.max(cauldronGroundDistance-maxDistanceBetweenTipAndCauldron, 0);

        float averageUpperProbability = -1F;

        if (currentLength < this.maxGrowthLength) {
            if (PointedDripstoneBlock.canGrow(dripstoneBlockState, liquidState)) {
                if (PointedDripstoneBlock.canDrip(tip) && PointedDripstoneBlock.canTipGrow(tip, level, tipPos)) {

                    var upperResult = MathUtils.getOccurrences(level, state, pos, GameUtils.getTime(level), timePassed, this.advanceProbability, this.requiresRain, lengthDifference, randomPickOdds, false, random, groupSimulateData);
                    averageUpperProbability = upperResult.averageProbability();
                    totalUpperDripGrowth = upperResult.occurrences();

                    if (stalagmiteGroundDistance != -1) {
                        if (totalUpperDripGrowth >= successesUntilReachGround) {
                            var recalculatedDuration = OccurrencesAndDuration.recalculatedDuration(successesUntilReachGround, timePassed, averageUpperProbability, random);
                            long leftover = timePassed - recalculatedDuration.duration();
                            int maxGroundGrowth = Math.min(stalagmiteGroundDistance, maxBottomGrowthSearchLength);
                            var lowerResult = MathUtils.getOccurrences(level, state, pos, GameUtils.getTime(level), leftover, this.advanceProbability, this.requiresRain, maxGroundGrowth, randomPickOdds, false, random, groupSimulateData);
                            totalLowerDripGrowth = lowerResult.occurrences();
                        }
                    }
                }
            }
        }

        boolean ultraWarm = #if MC_VER >= MC_1_21_11
                level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)
        #else
            level.dimensionType().ultraWarm()
        #endif;

        if (liquidState.is(Blocks.MUD) && !ultraWarm) {
            float totalDripOdds = this.waterFillProbability * randomPickOdds;
            int dripOccurrences = MathUtils.getOccurrencesBinomial(timePassed, totalDripOdds, 1, random);
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
                    float totalDripOdds = getCauldronDripOdds(dripstoneFluid) * randomPickOdds;

                    long leftover = timePassed;

                    // if successesUntilReachCauldron is 0, that means it didn't need to grow to reach the cauldron.
                    // If it wasn't 0, it did grow to reach this point, and averageUpperProbability is now a valid value.
                    if (successesUntilReachCauldron > 0)
                        leftover = timePassed - (MathUtils.sampleNegativeBinomialWithMax(timePassed, successesUntilReachCauldron, averageUpperProbability, random));

                    int dripOccurrences = MathUtils.getOccurrencesBinomial(leftover, totalDripOdds, LayeredCauldronBlock.MAX_FILL_LEVEL, random);
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

        while (successesUntilReachGround > 0 && totalUpperDripGrowth > 0) {
            --successesUntilReachGround;
            --totalUpperDripGrowth;
            PointedDripstoneBlock.grow(level, tipPos, Direction.DOWN);
            //recalculate tip so if tryGrow fails, we wont grow past any blocking blocks. Simply doing tipPos.down() doesnt account for fail.
            tipPos = PointedDripstoneBlock.findTip(level.getBlockState(pos), level, pos, 12, false);
            if (tipPos == null) return null;
        }

        while (totalUpperDripGrowth+totalLowerDripGrowth > 0) {
            if (totalUpperDripGrowth == 0) {

                if (pos.getY()-tipPos.getY() >= this.maxGrowthLength)
                    return null;

                --totalLowerDripGrowth;
                PointedDripstoneBlock.growStalagmiteBelow(level, tipPos);
            } else if (totalLowerDripGrowth == 0) {
                --totalUpperDripGrowth;
                PointedDripstoneBlock.grow(level, tipPos, Direction.DOWN);
                //recalculate tip so if tryGrow fails, we wont grow past any blocking blocks. Simply doing tipPos.down() doesnt account for fail.
                tipPos = PointedDripstoneBlock.findTip(level.getBlockState(pos), level, pos, 12, false);
                if (tipPos == null) return null;

            } else if (random.nextBoolean()) {
                --totalLowerDripGrowth;
                PointedDripstoneBlock.growStalagmiteBelow(level, tipPos);
            } else {
                --totalUpperDripGrowth;
                PointedDripstoneBlock.grow(level, tipPos, Direction.DOWN);
                //recalculate tip so if tryGrow fails, we wont grow past any blocking blocks. Simply doing tipPos.down() doesnt account for fail.
                tipPos = PointedDripstoneBlock.findTip(level.getBlockState(pos), level, pos, 12, false);
                if (tipPos == null) return null;
            }
        }

        return null;
    }
}
