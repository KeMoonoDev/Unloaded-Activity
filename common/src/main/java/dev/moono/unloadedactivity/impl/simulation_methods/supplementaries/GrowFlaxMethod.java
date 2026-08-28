package dev.moono.unloadedactivity.impl.simulation_methods.supplementaries;

import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SeparableSimulationMethod;
import net.mehvahdjukaar.supplementaries.common.block.blocks.FlaxBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class GrowFlaxMethod extends SeparableSimulationMethod {
    // For some reason on fabric trying to getAge the FlaxBlock crashes the game...
    // ... But for some reason if I cast it to a CropBlock, then it works??
    private final CropBlock flaxBlock;
    private final FlaxBlock actualFlaxBlock;
    public GrowFlaxMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);
        if (block instanceof FlaxBlock) {
            this.flaxBlock = (CropBlock)block;
            this.actualFlaxBlock = (FlaxBlock)block;
        } else {
            throw new RuntimeException("The block " + block + " cannot have this simulation method.");
        }
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        int currentAge = GameUtils.getCropAge(flaxBlock, state);
        int currentMaxAge = actualFlaxBlock.canGrowUp(level, pos) ? flaxBlock.getMaxAge() : (FlaxBlock.DOUBLE_AGE - 1);
        return Math.max(0, currentMaxAge - currentAge);
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings) {
        DeferredBlockPlacer blockPlacer = new DeferredBlockPlacer();

        int currentAge = GameUtils.getCropAge(flaxBlock, state);
        int newAge = currentAge + occurrencesAndTimings.occurrences();

        SimulatedTime finalTime = occurrencesAndTimings.getFinalTime();

        if (newAge >= FlaxBlock.DOUBLE_AGE) {
            blockPlacer.setBlock(pos.above(), flaxBlock.getStateForAge(newAge).setValue(FlaxBlock.HALF, DoubleBlockHalf.UPPER), finalTime);
        }
        blockPlacer.setBlock(pos, flaxBlock.getStateForAge(newAge), finalTime);
        return blockPlacer;
    }

    @Override
    public boolean isDependable() {
        return false;
    }
}
