package dev.moono.unloadedactivity.impl.simulation_methods.snowrealmagic;

import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SeparableSimulationMethod;
import dev.moono.unloadedactivity.impl.number_fetchers.snowrealmagic.MaxSimulationLayersValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import snownee.snow.CoreModule;
import snownee.snow.Hooks;
import snownee.snow.SnowCommonConfig;

public class SnowMethod extends SeparableSimulationMethod {
    public SnowMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        return MaxSimulationLayersValue.maxSimulationLayers();
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings) {
        BlockPos.MutableBlockPos mutablePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).mutable();

        int blizzard = SnowCommonConfig.snowGravity ? level.getGameRules().get(CoreModule.BLIZZARD_STRENGTH) : 0;
        if (blizzard > 0) {
            //doBlizzard(level, mutablePos, blizzard);
            return null;
        }

        if (SnowCommonConfig.snowAccumulationMaxLayers <= 0) {
            return null;
        }

        BlockState blockState = level.getBlockState(mutablePos);
        if (!snowHereIfPossible(level, mutablePos, blockState, occurrencesAndTimings.occurrences())) {
            if (!snowHereIfPossible(level, mutablePos, blockState = level.getBlockState(mutablePos.move(Direction.DOWN)), occurrencesAndTimings.occurrences())) {
                return null;
            }
        }

        for (int i = 0; i < 5; i++) {
            if (blockState.is(BlockTags.SLABS) || blockState.is(BlockTags.STAIRS)) {
                break;
            }
            blockState = level.getBlockState(mutablePos.move(Direction.DOWN));
            if (!blockState.isAir() && !Hooks.canContainState(blockState)) {
                break;
            }
            if (Hooks.canSnowSurvive(level, mutablePos)) {
                mutablePos.move(Direction.UP);
                if (level.getBlockState(mutablePos).getBlock() instanceof SnowLayerBlock || level.getBrightness(LightLayer.BLOCK, pos) >
                        SnowCommonConfig.snowSpawnMaxLightLevel) {
                    break;
                }
                Hooks.convert(level, mutablePos.move(Direction.DOWN), blockState, 1, Block.UPDATE_ALL, SnowCommonConfig.placeSnowOnBlockNaturally);
            }
        }
        return null;
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean snowHereIfPossible(ServerLevel level, BlockPos.MutableBlockPos pos, BlockState blockState, int layers) {
        if (level.getBrightness(LightLayer.BLOCK, pos.move(Direction.UP)) > SnowCommonConfig.snowSpawnMaxLightLevel) {
            pos.move(Direction.DOWN);
            return false;
        }

        if (layers == 8 && SnowCommonConfig.snowAccumulationMaxLayers == 8) {
            if (!blockState.is(BlockTags.SNOW)) {
                layers = 7;
            }
        }
        return Hooks.convert(level, pos.move(Direction.DOWN), blockState, layers, Block.UPDATE_ALL, SnowCommonConfig.placeSnowOnBlockNaturally);
    }
}
