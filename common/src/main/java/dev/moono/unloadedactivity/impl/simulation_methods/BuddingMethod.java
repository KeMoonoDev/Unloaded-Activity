package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.*;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BuddingMethod extends SimulationMethod {
    public final List<Pair<Block, @Nullable #if MC_VER >= MC_1_21_3 EnumProperty<Direction> #else DirectionProperty #endif>> buddingBlocks;
    public final List<Direction> ignoreBuddingDirections;

    @Nullable public final String buddingDirectionPropertyName;

    public final boolean supportsWaterlogged;

    public BuddingMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, hasDependants);
        List<Block> onlyBuddingBlocks = config.getBlockList("budding_blocks");

        this.ignoreBuddingDirections = config.getStringList("ignore_budding_directions").stream().map(directionString -> {
            for (Direction direction : Direction.values()) {
                if (direction.name().equalsIgnoreCase(directionString)) {
                    return direction;
                }
            }
            throw new RuntimeException(directionString + " is not a valid Direction in field ignore_budding_directions.");
        }).toList();

        ArrayList<Direction> supportedBuddingDirections = new ArrayList<>();

        for (Direction direction : Direction.values())
            if (!this.ignoreBuddingDirections.contains(direction))
                supportedBuddingDirections.add(direction);

        this.buddingDirectionPropertyName = config.getStringNullable("budding_direction_property_name");

        int waterloggedCount = 0;

        List<Pair<Block, @Nullable #if MC_VER >= MC_1_21_3 EnumProperty<Direction> #else DirectionProperty #endif>> newBuddingBlocks = new ArrayList<>();

        for (Block buddingBlock : onlyBuddingBlocks) {
            if (buddingBlock.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)) waterloggedCount++;

            if (this.buddingDirectionPropertyName == null) {
                newBuddingBlocks.add(Pair.of(buddingBlock, null));
            } else {
                Optional<Property<?>> maybeProperty = GameUtils.getProperty(buddingBlock.defaultBlockState(), this.buddingDirectionPropertyName);
                if (maybeProperty.isEmpty()) throw new RuntimeException("The property " + buddingDirectionPropertyName + " does not exist for all budding blocks.");
                Property<?> property = maybeProperty.get();
                Collection<?> possibleValues = property.getPossibleValues();
                for (Direction supportedDirection : supportedBuddingDirections) {
                    if (!possibleValues.contains(supportedDirection)) {
                        throw new RuntimeException("The property " + buddingDirectionPropertyName + " on block " + buddingBlock + " does not support the direction " + supportedDirection + ".");
                    }
                }
                @SuppressWarnings("unchecked")
                var buddingProperty = (#if MC_VER >= MC_1_21_3 EnumProperty<Direction> #else DirectionProperty #endif)property;
                newBuddingBlocks.add(Pair.of(buddingBlock, buddingProperty));
            }
        }

        this.buddingBlocks = List.copyOf(newBuddingBlocks);
        this.supportsWaterlogged = waterloggedCount == this.buddingBlocks.size();

        if (this.buddingBlocks.isEmpty())
            throw new RuntimeException("The field \"budding_blocks\" is not defined or is an empty array.");
    }

    @Override
    public boolean canDoMore(BlockState state, ServerLevel level, BlockPos pos) {
        Block finalBlock = buddingBlocks.get(buddingBlocks.size()-1).getLeft();

        List<Direction> availableDirections = Arrays.stream(Direction.values()).filter(direction -> !this.ignoreBuddingDirections.contains(direction)).toList();

        for (Direction direction : availableDirections) {
            BlockPos dirPos = pos.relative(direction);
            BlockState dirBlockState = level.getBlockState(dirPos);
            if (dirBlockState.is(finalBlock)) continue;

            if (dirBlockState.isAir()) return true; // Empty block can be grown into.

            if (this.supportsWaterlogged) {
                if (dirBlockState.is(Blocks.WATER)) {
                    if (dirBlockState.getFluidState().getAmount() >= 8) return true; // Water block can be grown into.
                }
            }

            for (var buddingStagePair : buddingBlocks) {
                Block buddingStageBlock = buddingStagePair.getLeft();
                if (!dirBlockState.is(buddingStageBlock)) continue;

                if (buddingStageBlock == finalBlock) continue;

                var buddingStageDirProperty = buddingStagePair.getRight();

                if (buddingStageDirProperty == null) return true; // Block is a budding block but not the final block, and it's not limited by alignment to grow.

                Direction blockDirection = dirBlockState.getValue(buddingStageDirProperty);
                if (blockDirection == direction) return true; // Block is a budding block but not the final block, and it's properly aligned to grow.

            }

        }

        return false;
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public @Nullable DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, SimulatedTime simulatedTime, float randomPickProbability) {
        List<Direction> availableDirections = Arrays.stream(Direction.values()).filter(direction -> !this.ignoreBuddingDirections.contains(direction)).toList();

        DeferredBlockPlacer blockPlacer = DeferredBlockPlacer.empty();

        for(Direction direction : availableDirections) {
            BlockPos budPos = pos.relative(direction);
            BlockState budState = level.getBlockState(budPos);

            int stage = 0;


            boolean doContinue = false;

            for (int i=0;i<this.buddingBlocks.size();i++) {
                var buddingPairStage = this.buddingBlocks.get(i);
                Block buddingBlockStage = buddingPairStage.getLeft();
                var buddingDirProperty = buddingPairStage.getRight();

                if (budState.is(buddingBlockStage)) {
                    if (buddingDirProperty != null) {
                        Direction budDirection = budState.getValue(buddingDirProperty);
                        if (budDirection != direction) {
                            doContinue = true;
                            break;
                        }
                    }
                    stage = i+1;
                    break;
                }
            }

            if (doContinue)
                continue;


            if (stage == this.buddingBlocks.size())
                continue;


            if (stage == 0) {
                if (!budState.isAir()) {
                    if (!this.supportsWaterlogged) {
                        continue;
                    }

                    if (!budState.is(Blocks.WATER))
                        continue;

                    if (budState.getFluidState().getAmount() < 8)
                        continue;
                }
                // It's either air or water.
            }

            int maxOccurrences = this.buddingBlocks.size() - stage;

            OccurrencesAndTimings result = MathUtils.getOccurrences(level, state, pos, simulatedTime, this, maxOccurrences, randomPickProbability);

            if (result.occurrences() == 0) {
                continue;
            }

            int newStage = stage + result.occurrences();

            var newBudPair = this.buddingBlocks.get(newStage - 1);
            Block newBudBlock = newBudPair.getLeft();
            var newBudProperty = newBudPair.getRight();

            BlockState newBudState = newBudBlock.defaultBlockState();

            if (newBudProperty != null) {
                newBudState = newBudState.setValue(newBudProperty, direction);
            }


            if (this.supportsWaterlogged) {
                newBudState = newBudState.setValue(BlockStateProperties.WATERLOGGED, budState.getFluidState().getType() == Fluids.WATER);
            }

            blockPlacer.setBlock(budPos, newBudState, result.getFinalTime());
        }

        return blockPlacer;
    }
}
