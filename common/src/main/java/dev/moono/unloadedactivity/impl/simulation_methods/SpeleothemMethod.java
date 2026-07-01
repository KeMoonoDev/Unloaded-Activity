package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.*;
import dev.moono.unloadedactivity.api.condition.FixedCondition;
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

import java.util.List;

public class SpeleothemMethod extends SimulationMethod {
    @Nullable public final CauldronFillConfig cauldronFillConfig;
    public final List<ConvertBlockConfig> convertBlocks;

    public record CauldronFillConfig(int maxDistanceBetweenTipAndCauldron, float waterFillProbability, float lavaFillProbability) {
        public CauldronFillConfig(SimulationConfig config) {
            this(
                config.getNumber("max_distance_between_tip_and_cauldron").intValue(),
                config.getNumber("water_fill_probability").floatValue(),
                config.getNumber("lava_fill_probability").floatValue()
            );
        }

        private float getCauldronDripProbability(Fluid fluid) {
            if (fluid == Fluids.WATER) {
                return this.waterFillProbability;
            } else if (fluid == Fluids.LAVA) {
                return this.lavaFillProbability;
            }
            return 0.0f;
        }
    }

    public record ConvertBlockConfig(Block fromBlock, Block toBlock, float convertProbability, List<FixedCondition> conditions) {
        public ConvertBlockConfig(SimulationConfig config) {
            this(
                config.getBlock("from_block"),
                config.getBlock("to_block"),
                config.getNumber("convert_probability").floatValue(),
                config.getFixedConditionList("conditions")
            );
        }
    }

    public SpeleothemMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, hasDependants);
        SimulationConfig cauldronFillConfig = config.getConfigNullable("cauldron_fill");
        this.cauldronFillConfig = cauldronFillConfig != null ? new CauldronFillConfig(cauldronFillConfig) : null;
        List<SimulationConfig> convertBlocksConfig = config.getConfigList("convert_blocks");
        this.convertBlocks = convertBlocksConfig.stream().map(ConvertBlockConfig::new).toList();
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
    public @Nullable DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, SimulatedTime simulatedTime, float randomPickProbability) {
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

                    upperResult = MathUtils.getOccurrences(level, state, pos, simulatedTime, this, lengthDifference, randomPickProbability, calculateDuration);
                    totalUpperDripGrowth = upperResult.occurrences();

                    if (bottomStalagmiteDistance != -1) {
                        if (totalUpperDripGrowth >= successesUntilReachGround) {
                            SimulatedTime newTime = upperResult.getTimeAtOccurrence(successesUntilReachGround);
                            int maxGroundGrowth = Math.min(bottomStalagmiteDistance, bottomSearchRange);
                            var lowerResult = MathUtils.getOccurrences(level, state, pos, newTime, this, maxGroundGrowth, randomPickProbability);
                            totalLowerDripGrowth = lowerResult.occurrences();
                        }
                    }
                }
            }
        }

        for (ConvertBlockConfig convertBlockConfig : this.convertBlocks) {
            if (!liquidState.is(convertBlockConfig.fromBlock)) continue;

            boolean isValid = true;
            for (FixedCondition condition : convertBlockConfig.conditions) {
                if (!condition.isValidFixed(level, state, pos)) {
                    isValid = false;
                    break;
                }
            }

            if (!isValid) continue;

            float totalDripProbability = convertBlockConfig.convertProbability * randomPickProbability;
            int dripOccurrences = MathUtils.getOccurrencesSimple(simulatedTime.remainingTime(), totalDripProbability, 1, random);
            if (dripOccurrences != 0) {
                BlockState newBlock = convertBlockConfig.toBlock.defaultBlockState();
                level.setBlockAndUpdate(pos.above(2), newBlock);
                Block.pushEntitiesUp(liquidState, newBlock, level, pos.above(2));
            }
        }

        if (this.cauldronFillConfig != null) {
            if (cauldronPos != null && totalUpperDripGrowth >= successesUntilReachCauldron) {
                BlockState cauldronState = level.getBlockState(cauldronPos);
                if (cauldronState.getBlock() instanceof AbstractCauldronBlock cauldronBlock) {

                    if (!cauldronBlock.isFull(cauldronState) && cauldronBlock.canReceiveStalactiteDrip(dripstoneFluid)) {
                        float totalDripProbability = this.cauldronFillConfig.getCauldronDripProbability(dripstoneFluid) * randomPickProbability;

                        SimulatedTime newTime = upperResult.getTimeAtOccurrence(successesUntilReachCauldron);

                        int dripOccurrences = MathUtils.getOccurrencesSimple(newTime.remainingTime(), totalDripProbability, LayeredCauldronBlock.MAX_FILL_LEVEL, random);
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
