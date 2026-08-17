package dev.moono.unloadedactivity.impl.simulation_methods;

import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.context.RandomizedContext;
import dev.moono.unloadedactivity.api.simulation_method.SeparableSimulationMethod;
import dev.moono.unloadedactivity.api.value_expression.RandomizedValueExpression;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationData;
import dev.moono.unloadedactivity.datapack.simulation_data.SimulationDataResource;
import dev.moono.unloadedactivity.impl.SimulationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GrowBlockMethod extends SeparableSimulationMethod {
    public final int updateType;
    public final boolean updateNeighbors;
    public final RandomizedValueExpression<Block> growBlock;
    public final @Nullable RandomizedValueExpression<Block> bottomBlockReplacement;

    public final @Nullable RestrictHeightConfig restrictHeight;

    public final boolean replaceAboveBlock;

    public final Map<String, RandomizedValueExpression<Number>> setProperties;
    public final Map<String, RandomizedValueExpression<Number>> setBottomProperties;

    public final Map<String, RandomizedValueExpression<String>> setNamedProperties;
    public final Map<String, RandomizedValueExpression<String>> setBottomNamedProperties;

    public record RestrictHeightConfig(int maxHeight, List<Block> lowerBlocks) {
        public RestrictHeightConfig(SimulationConfig config) {
            this(
                config.getNumber("max_height").intValue(),
                config.getBlockList("lower_blocks")
            );
        }
    }

    public @Nullable Boolean cachedShouldCalculateDuration;

    public GrowBlockMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);

        this.updateType = config.getNumberOrDefault("update_type", Block.UPDATE_ALL).intValue();
        this.updateNeighbors = config.getBooleanOrDefault("update_neighbors", false);

        this.growBlock = config.getRandomizedBlockExpression("grow_block");
        this.bottomBlockReplacement = config.getRandomizedBlockExpressionNullable("bottom_block_replacement");

        SimulationConfig restrictHeightConfig = config.getConfigNullable("restrict_height");;
        this.restrictHeight = restrictHeightConfig == null ? null : new RestrictHeightConfig(restrictHeightConfig);

        this.replaceAboveBlock = config.getBooleanOrDefault("replace_above_block", false);

        this.setProperties = config.getRandomizedNumberExpressionMap("set_properties");
        this.setBottomProperties = config.getRandomizedNumberExpressionMap("set_bottom_properties");

        this.setNamedProperties = config.getRandomizedStringExpressionMap("set_named_properties");
        this.setBottomNamedProperties = config.getRandomizedStringExpressionMap("set_bottom_named_properties");
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public boolean shouldCalculateDuration(BlockState state, ServerLevel level, BlockPos pos) {
        if (this.cachedShouldCalculateDuration == null) {
            this.cachedShouldCalculateDuration =
                SimulationUtils.resultingBlocksMayNeedDuration(this.growBlock.inner) ||
                SimulationUtils.anyNeedsDuration(this.setProperties.values()) ||
                SimulationUtils.anyNeedsDuration(this.setNamedProperties.values()) ||
                SimulationUtils.anyNeedsDuration(this.setBottomProperties.values()) ||
                SimulationUtils.anyNeedsDuration(this.setBottomNamedProperties.values());

            if (!this.cachedShouldCalculateDuration && this.bottomBlockReplacement != null) {
                this.cachedShouldCalculateDuration = SimulationUtils.resultingBlocksMayNeedDuration(this.bottomBlockReplacement.inner);
            }
        }
        return this.cachedShouldCalculateDuration || super.shouldCalculateDuration(state, level, pos);
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        if (this.restrictHeight != null) {
            int height;
            for(height = 1; height <= this.restrictHeight.maxHeight; ++height) {
                boolean doContinue = false;
                for (Block lowerBlock : this.restrictHeight.lowerBlocks) {
                    if (level.getBlockState(pos.below(height)).is(lowerBlock)) {
                        doContinue = true;
                        break;
                    }
                }
                if (doContinue) continue;
                break;
            }
            if (height >= this.restrictHeight.maxHeight) return 0;
        }

        if (replaceAboveBlock) return 1;

        return level.isEmptyBlock(pos.above()) ? 1 : 0;
    }

    @Override
    public DeferredBlockPlacer getNewBlockStates(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings) {
        DeferredBlockPlacer deferredBlockPlacer = DeferredBlockPlacer.empty();

        if (occurrencesAndTimings.occurrences() == 0) return deferredBlockPlacer;

        SimulatedTime finalTime = occurrencesAndTimings.getFinalTime();

        RandomizedContext context = RandomizedContext.of(level, state, pos, finalTime);

        BlockState newBottomBlockState = state;

        if (this.bottomBlockReplacement != null) {
            Block bottomBlock = this.bottomBlockReplacement.evaluate(context);
            newBottomBlockState = bottomBlock.defaultBlockState();
        }

        newBottomBlockState = SimulationUtils.applySetProperties(newBottomBlockState, context, setBottomProperties);
        newBottomBlockState = SimulationUtils.applySetNamedProperties(newBottomBlockState, context, setBottomNamedProperties);
        if (newBottomBlockState != state) {
            deferredBlockPlacer.setBlock(pos, newBottomBlockState, updateNeighbors, updateType, finalTime);
        }

        Block aboveBlock = this.growBlock.evaluate(context);
        BlockState aboveBlockState = aboveBlock.defaultBlockState();
        aboveBlockState = SimulationUtils.applySetProperties(aboveBlockState, context, setProperties);
        aboveBlockState = SimulationUtils.applySetNamedProperties(aboveBlockState, context, setNamedProperties);
        deferredBlockPlacer.setBlock(pos.above(), aboveBlockState, updateNeighbors, updateType, finalTime);

        return deferredBlockPlacer;
    }
}
