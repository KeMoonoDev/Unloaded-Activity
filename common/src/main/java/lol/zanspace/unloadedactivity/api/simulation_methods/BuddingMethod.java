package lol.zanspace.unloadedactivity.api.simulation_methods;

import lol.zanspace.unloadedactivity.*;
import lol.zanspace.unloadedactivity.api.SimulationConfig;
import lol.zanspace.unloadedactivity.api.SimulationMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WaterloggedTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.feature.WaterloggedVegetationPatchFeature;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class BuddingMethod extends SimulationMethod {
    public final List<Block> buddingBlocks;
    public final List<Direction> ignoreBuddingDirections;

    @Nullable public final String buddingDirectionPropertyName;

    public final boolean supportsWaterlogged;

    public BuddingMethod(SimulationConfig config) {
        super(config);
        this.buddingBlocks = config.getBlockList("budding_blocks");
        this.ignoreBuddingDirections = config.getStringList("ignore_budding_directions").stream().map(directionString -> {
            for (Direction direction : Direction.values()) {
                if (direction.name().equalsIgnoreCase(directionString)) {
                    return direction;
                }
            }
            throw new RuntimeException(directionString + " is not a valid Direction in field ignore_budding_directions.");
        }).toList();

        this.buddingDirectionPropertyName = config.getStringNullable("budding_direction_property_name");

        int waterloggedCount = 0;

        for (Block buddingBlock : this.buddingBlocks) {
            if (buddingBlock.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)) {
                waterloggedCount++;
            }
        }

        this.supportsWaterlogged = waterloggedCount == this.buddingBlocks.size();

        if (this.buddingBlocks.isEmpty())
            throw new RuntimeException("The field \"budding_blocks\" is not defined or is an empty array.");
    }

    @Override
    public boolean isFinished(BlockState state, ServerLevel level, BlockPos pos) {
        Block finalBlock = buddingBlocks.get(buddingBlocks.size()-1);

        List<Direction> availableDirections = Arrays.stream(Direction.values()).filter(direction -> !this.ignoreBuddingDirections.contains(direction)).toList();

        // todo check if there's progress to be made. Otherwise return false.
        // Right now it only returns true if all budding blocks on all directions are the final block.
        for (Direction direction : availableDirections) {
            BlockPos dirPos = pos.relative(direction);
            BlockState dirBlockState = level.getBlockState(dirPos);
            if (!dirBlockState.is(finalBlock)) {
                return false;
            }
            if (this.buddingDirectionPropertyName != null) {
                var property = (#if MC_VER >= MC_1_21_3 EnumProperty<?> #else DirectionProperty #endif) GameUtils.getProperty(dirBlockState, this.buddingDirectionPropertyName).get();

                Direction blockDirection = (Direction) dirBlockState.getValue(property);
                if (blockDirection != direction) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public @Nullable DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, long timePassed, float randomPickOdds, boolean hasDependents, @Nullable ActiveGroupSimulateData groupSimulateData) {
        List<Direction> availableDirections = Arrays.stream(Direction.values()).filter(direction -> !this.ignoreBuddingDirections.contains(direction)).toList();

        DeferredBlockPlacer blockPlacer = DeferredBlockPlacer.empty();

        for(Direction direction : availableDirections) {
            BlockPos budPos = pos.relative(direction);
            BlockState budState = level.getBlockState(budPos);

            int stage = 0;


            boolean doContinue = false;

            for (int i=0;i<this.buddingBlocks.size();i++) {
                Block buddingBlockStage = this.buddingBlocks.get(i);

                if (budState.is(buddingBlockStage)) {
                    var property = (#if MC_VER >= MC_1_21_3 EnumProperty<?> #else DirectionProperty #endif) GameUtils.getProperty(budState, this.buddingDirectionPropertyName).get();
                    Direction budDirection = (Direction) budState.getValue(property);

                    if (budDirection == direction) {
                        stage = i+1;
                    } else {
                        doContinue = true;
                    }

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

            OccurrencesAndDuration result = MathUtils.getOccurrences(level, state, pos, GameUtils.getTime(level), timePassed, this.advanceProbability, this.requiresRain, maxOccurrences, randomPickOdds, hasDependents, random, groupSimulateData);

            if (result.occurrences() == 0) {
                continue;
            }

            int newStage = stage + result.occurrences();

            Block newBudBlock = this.buddingBlocks.get(newStage - 1);

            BlockState newBudState = newBudBlock.defaultBlockState();

            if (this.buddingDirectionPropertyName != null) {
                @SuppressWarnings("unchecked")
                var property = (#if MC_VER >= MC_1_21_3 EnumProperty<Direction> #else DirectionProperty #endif) GameUtils.getProperty(newBudState, buddingDirectionPropertyName).get();
                newBudState = newBudState.setValue(property, direction);
            }


            if (this.supportsWaterlogged) {
                newBudState = newBudState.setValue(BlockStateProperties.WATERLOGGED, budState.getFluidState().getType() == Fluids.WATER);
            }

            blockPlacer.setBlock(budPos, newBudState, result.duration());
        }

        return blockPlacer;
    }
}
