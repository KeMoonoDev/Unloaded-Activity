package dev.moono.unloadedactivity.impl.simulation_methods.snowrealmagic;

#if MC_VER >= MC_1_20_1
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import snownee.snow.CoreModule;
import snownee.snow.Hooks;
import snownee.snow.SnowCommonConfig;
import snownee.snow.block.SnowVariant;

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
        mutablePos.move(Direction.DOWN);
        #if MC_VER >= MC_1_21_11
        int blizzard = SnowCommonConfig.snowGravity ? level.getGameRules().get(CoreModule.BLIZZARD_STRENGTH) : 0;
        #else
        int blizzard = SnowCommonConfig.snowGravity ? level.getGameRules().getInt(CoreModule.BLIZZARD_STRENGTH) : 0;
        #endif
        if (blizzard > 0) {
            //doBlizzard(level, mutablePos, blizzard);
            return null;
        }

        if (SnowCommonConfig.snowAccumulationMaxLayers <= 0) {
            return null;
        }

        #if MC_VER <= MC_1_20_1
        BlockState blockState = level.getBlockState(mutablePos);
        if (!Hooks.canContainState(blockState)) {
            if (blockState.is(BlockTags.SNOW)) {
                return null;
            }
            blockState = level.getBlockState(mutablePos.move(Direction.UP));
            if (!blockState.isAir() && !Hooks.canContainState(blockState)) {
                return null;
            }
        }

        if (blockState.isAir() && !Hooks.canSnowSurvive(Blocks.SNOW.defaultBlockState(), level, mutablePos)) {
            return null;
        }
        if (level.getBrightness(LightLayer.BLOCK, mutablePos.move(Direction.UP)) > SnowCommonConfig.snowSpawnMaxLightLevel) {
            return null;
        }
        int layers = occurrencesAndTimings.occurrences();
        if (layers == 8 && SnowCommonConfig.snowAccumulationMaxLayers == 8) {
            if (!blockState.is(BlockTags.SNOW)) {
                layers = 7;
            }
        }
        boolean success = Hooks.convert(level, mutablePos.move(Direction.DOWN), blockState, layers, 3, SnowCommonConfig.placeSnowOnBlockNaturally);
        if (!success) {
            return null;
        }
        #else
        BlockState blockState = level.getBlockState(mutablePos);
        if (!snowHereIfPossible(level, mutablePos, blockState, occurrencesAndTimings.occurrences())) {
            if (!snowHereIfPossible(level, mutablePos, blockState = level.getBlockState(mutablePos.move(Direction.DOWN)), occurrencesAndTimings.occurrences())) {
                return null;
            }
        }
        #endif

        for (int i = 0; i < 5; i++) {
            if (blockState.is(BlockTags.SLABS) || blockState.is(BlockTags.STAIRS)) {
                break;
            }
            blockState = level.getBlockState(mutablePos.move(Direction.DOWN));
            if (!blockState.isAir() && !Hooks.canContainState(blockState)) {
                break;
            }
            if (Hooks.canSnowSurvive(#if MC_VER <= MC_1_20_1 Blocks.SNOW.defaultBlockState(), #endif level, mutablePos)) {
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
#else
public class SnowMethod {}
#endif