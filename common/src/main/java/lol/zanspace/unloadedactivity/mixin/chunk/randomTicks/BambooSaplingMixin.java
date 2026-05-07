package lol.zanspace.unloadedactivity.mixin.chunk.randomTicks;

import lol.zanspace.unloadedactivity.ActiveGroupSimulateData;
import lol.zanspace.unloadedactivity.MathUtils;
import lol.zanspace.unloadedactivity.OccurrencesAndDuration;
import lol.zanspace.unloadedactivity.datapack.SimulateProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BambooSaplingBlock.class)
public abstract class BambooSaplingMixin extends Block {

    public BambooSaplingMixin(Properties properties) {
        super(properties);
    }

    @Shadow protected void growBamboo(Level level, BlockPos pos) {}

    @Override
    public @Nullable Triple<BlockState, OccurrencesAndDuration, BlockPos> simulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty, RandomSource random, long timePassed, float randomPickOdds, boolean calculateDuration, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (simulateProperty.isAction("grow_bamboo")) {

            int maxHeight = simulateProperty.maxHeight.orElse(15);

            if (maxHeight <= 1 || !level.isEmptyBlock(pos.above()))
                return Triple.of(state, OccurrencesAndDuration.empty(), pos);

            OccurrencesAndDuration result = MathUtils.getOccurrences(level, state, pos, level.getDayTime(), timePassed, simulateProperty, 1, randomPickOdds, true, random, groupSimulateData);

            if (result.occurrences() != 0) {
                this.growBamboo(level, pos);
                pos = pos.above();
                state = level.getBlockState(pos);
            }

            return Triple.of(state, result, pos);
        }
        return super.simulateProperty(state, level, pos, simulateProperty, random, timePassed, randomPickOdds, calculateDuration, groupSimulateData);
    }
}
