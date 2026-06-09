package lol.zanspace.unloadedactivity.api.simulation_methods;

import lol.zanspace.unloadedactivity.ActiveGroupSimulateData;
import lol.zanspace.unloadedactivity.DeferredBlockPlacer;
import lol.zanspace.unloadedactivity.GameUtils;
import lol.zanspace.unloadedactivity.api.SimulationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GrowFruitMethod extends SeparableSimulationMethod{
    public final Block stemBlock;
    public final Block fruitBlock;

    public GrowFruitMethod(SimulationConfig config) {
        super(config);
        this.stemBlock = config.getBlock("stem_block");
        this.fruitBlock = config.getBlock("fruit_block");
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        return 1;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, int occurrences, long simulationDuration, long timePassed, @Nullable ActiveGroupSimulateData groupSimulateData) {
        List<Direction> directions = Direction.Plane.HORIZONTAL.shuffledCopy(GameUtils.getRand(level));

        DeferredBlockPlacer blockPlacer = DeferredBlockPlacer.empty();

        for (int i = 0; i < directions.size(); i++) {
            Direction direction = directions.get(i);

            if (!GameUtils.isValidGourdPosition(direction, pos, state, level)) continue;

            BlockPos blockPos = pos.relative(direction);
            blockPlacer.setBlock(blockPos, this.fruitBlock.defaultBlockState(), simulationDuration);
            state = this.stemBlock.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction);
            blockPlacer.setBlock(pos, state, simulationDuration);
            break;
        }
        return blockPlacer;
    }

    @Override
    public boolean isFinished(BlockState state, ServerLevel level, BlockPos pos) {
        return false;
    }
}
