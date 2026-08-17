package dev.moono.unloadedactivity.impl.simulation_methods.nomansland;

import com.farcr.nomansland.common.block.cauldrons.FourLayeredCauldronBlock;
import com.farcr.nomansland.common.block.tap.TapBlock;
import com.farcr.nomansland.common.block.tap.TapInteraction;
import com.farcr.nomansland.common.registry.NMLRegistries;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.MathUtils;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.SeparableSimulationMethod;
import dev.moono.unloadedactivity.api.simulation_method.SimulationMethod;
import dev.moono.unloadedactivity.mixin.IntegerPropertyAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TapMethod extends SimulationMethod {
    public TapMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, block, hasDependants);
    }

    @Override
    public boolean canDoMore(BlockState state, ServerLevel level, BlockPos pos) {
        // The result of this is only ever useful if isDependable returns true.
        // If isDependable returns false, it's fine to be lazy and always return true.
        return true;
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    private @Nullable TapInteraction getTapInteraction(BlockState state, ServerLevel level, BlockPos pos) {
        BlockState stateBehind = TapBlock.getBlockStateBehind(level, pos, state);
        List<Holder.Reference<TapInteraction>> allTapInteractions = level.registryAccess().registryOrThrow(NMLRegistries.TAP_INTERACTION_KEY).holders().filter(tapInteractionReference -> tapInteractionReference.value().particleType().isPresent()).toList();

        for (Holder.Reference<TapInteraction> tapInteractionReference : allTapInteractions) {
            boolean hasBlock = false;
            for (BlockStateProvider blockStateProvider : tapInteractionReference.value().sources()) {
                // blockStateProvider.getState takes a RandomSource.
                // This function assumes that the result won't be random.
                // If getState gives a random result, the simulation result will be incorrect.
                BlockState neededState = blockStateProvider.getState(level.random, pos.relative(state.getValue(TapBlock.FACING).getOpposite()));
                if (stateBehind == neededState) {
                    if (neededState.is(BlockTags.LOGS)) {
                        if (stateBehind == TapBlock.getBlockStateBehind(level, pos.above(), state) && stateBehind == TapBlock.getBlockStateBehind(level, pos.below(), state))
                            hasBlock = true;
                    } else hasBlock = true;
                    break;
                }
            }

            if (hasBlock) return tapInteractionReference.value();
        }
        return null;
    }

    @Override
    public @Nullable DeferredBlockPlacer simulate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, SimulatedTime simulatedTime, float randomPickProbability) {
        BlockPos cauldronPos = TapBlock.getCauldronPos(level, pos);
        if (cauldronPos == null) return DeferredBlockPlacer.empty();

        TapInteraction tapInteraction = this.getTapInteraction(state, level, pos);
        if (tapInteraction == null) return DeferredBlockPlacer.empty();

        BlockState cauldronState = level.getBlockState(cauldronPos);

        int currentLevel;
        int maxLevel;

        if (cauldronState.getBlock() == tapInteraction.cauldron() && cauldronState.getBlock() instanceof AbstractCauldronBlock cauldron && !cauldron.isFull(cauldronState)) {
            if (cauldronState.getBlock() instanceof FourLayeredCauldronBlock) {
                currentLevel = cauldronState.getValue(FourLayeredCauldronBlock.LEVEL);
                maxLevel = ((IntegerPropertyAccessor)FourLayeredCauldronBlock.LEVEL).unloaded_activity$getMax();
            } else {
                currentLevel = cauldronState.getValue(LayeredCauldronBlock.LEVEL);
                maxLevel = ((IntegerPropertyAccessor)LayeredCauldronBlock.LEVEL).unloaded_activity$getMax();
            }
        } else if (cauldronState.is(Blocks.CAULDRON)) {
            currentLevel = 0;
            BlockState newCauldronState = tapInteraction.cauldron().defaultBlockState();
            if (newCauldronState.getBlock() instanceof FourLayeredCauldronBlock) {
                maxLevel = ((IntegerPropertyAccessor)FourLayeredCauldronBlock.LEVEL).unloaded_activity$getMax();
            } else {
                maxLevel = ((IntegerPropertyAccessor)LayeredCauldronBlock.LEVEL).unloaded_activity$getMax();
            }
        } else {
            return DeferredBlockPlacer.empty();
        }

        int updateCount = maxLevel - currentLevel;
        if (updateCount <= 0) return DeferredBlockPlacer.empty();

        OccurrencesAndTimings result = MathUtils.getOccurrences(level, state, pos, simulatedTime, this, updateCount, randomPickProbability * tapInteraction.rate());

        if (result.occurrences() == 0) return DeferredBlockPlacer.empty();

        int occurrences = result.occurrences();

        BlockState newState = null;

        if (cauldronState.getBlock() == tapInteraction.cauldron() && cauldronState.getBlock() instanceof AbstractCauldronBlock cauldron && !cauldron.isFull(cauldronState)) {
            if (cauldronState.getBlock() instanceof FourLayeredCauldronBlock)
                newState = cauldronState.setValue(FourLayeredCauldronBlock.LEVEL, cauldronState.getValue(FourLayeredCauldronBlock.LEVEL) + occurrences);
            else
                newState = cauldronState.setValue(LayeredCauldronBlock.LEVEL, cauldronState.getValue(LayeredCauldronBlock.LEVEL) + occurrences);


        } else if (cauldronState.is(Blocks.CAULDRON)) {
            cauldronState = tapInteraction.cauldron().defaultBlockState();
            if (cauldronState.getBlock() instanceof AbstractCauldronBlock cauldron && !cauldron.isFull(cauldronState)) {
                if (cauldronState.getBlock() instanceof FourLayeredCauldronBlock)
                    newState = cauldronState.setValue(FourLayeredCauldronBlock.LEVEL, cauldronState.getValue(FourLayeredCauldronBlock.LEVEL) + occurrences - 1);
                else
                    newState = cauldronState.setValue(LayeredCauldronBlock.LEVEL, cauldronState.getValue(LayeredCauldronBlock.LEVEL) + occurrences - 1);
            }
        }

        if (newState == null) return DeferredBlockPlacer.empty();

        // I don't use DeferredBlockPlacer here because it doesn't support gameEvent.
        // Also because I doubt cauldrons will ever need more simulating after being filled.
        level.setBlockAndUpdate(cauldronPos, newState);
        level.gameEvent(GameEvent.BLOCK_CHANGE, cauldronPos, GameEvent.Context.of(newState));

        return DeferredBlockPlacer.empty();
    }
}
