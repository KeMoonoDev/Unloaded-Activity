package lol.zanspace.unloadedactivity.mixin.chunk.randomTicks;


import lol.zanspace.unloadedactivity.ActiveGroupSimulateData;
import lol.zanspace.unloadedactivity.GameUtils;
import lol.zanspace.unloadedactivity.OccurrencesAndDuration;
import lol.zanspace.unloadedactivity.MathUtils;
import lol.zanspace.unloadedactivity.datapack.SimulateProperty;

#if MC_VER >= MC_1_20_4
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
#endif
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Optional;

import static java.lang.Math.min;

@Mixin(StemBlock.class)
public abstract class StemMixin extends #if MC_VER >= MC_1_21_5 VegetationBlock #else BushBlock #endif {

    protected StemMixin(Properties properties) {
        super(properties);
    }

    @Final
    @Shadow
    public static IntegerProperty AGE;

    #if MC_VER >= MC_1_20_4
    @Shadow @Final
    private ResourceKey<Block> fruit;
    @Shadow @Final
    private ResourceKey<Block> attachedStem;
    #else
    @Final
    @Shadow
    private StemGrownBlock fruit;
    #endif

    @Override
    public boolean isPropertyFinished(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty) {
        if (simulateProperty.isAction("grow_fruit")) {
            // When growing fruit, the stem block gets entirely replaced. There is nothing to check.
            return false;
        }

        return super.isPropertyFinished(state, level, pos, simulateProperty);
    }

    @Override
    public @Nullable Triple<BlockState, OccurrencesAndDuration, BlockPos> simulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulateProperty simulateProperty, RandomSource random, long timePassed, float randomPickOdds, boolean calculateDuration, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (simulateProperty.isAction("grow_fruit")) {
            OccurrencesAndDuration result = MathUtils.getOccurrences(level, state, pos, GameUtils.getTime(level), timePassed, simulateProperty, 1, randomPickOdds, calculateDuration, random, groupSimulateData);

            if (result.occurrences() == 0)
                return Triple.of(state, result, pos);

            List<Direction> directions = Direction.Plane.HORIZONTAL.shuffledCopy(random);

            for (int i = 0; i < directions.size(); i++) {

                Direction direction = directions.get(i);

                if (!GameUtils.isValidGourdPosition(direction, pos, level)) continue;

                BlockPos blockPos = pos.relative(direction);
                #if MC_VER >= MC_1_20_4
                    #if MC_VER >= MC_1_21_3
                        Registry<Block> blockRegistry = level.registryAccess().lookupOrThrow(Registries.BLOCK);
                    #else
                        Registry<Block> blockRegistry = level.registryAccess().registryOrThrow(Registries.BLOCK);
                    #endif
                    Optional<Block> fruitBlock = blockRegistry.getOptional(this.fruit);
                    Optional<Block> attachedStemBlock = blockRegistry.getOptional(this.attachedStem);

                    if (fruitBlock.isPresent() && attachedStemBlock.isPresent()) {
                        level.setBlockAndUpdate(blockPos, fruitBlock.get().defaultBlockState());
                        state = attachedStemBlock.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction);
                        level.setBlockAndUpdate(pos, state);
                    }
                #else
                    level.setBlockAndUpdate(blockPos, this.fruit.defaultBlockState());

                    state = this.fruit.getAttachedStem().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction);
                    level.setBlockAndUpdate(pos, state);
                #endif
                break;
            }
            return Triple.of(state, result, pos);
        }
        return super.simulateProperty(state, level, pos, simulateProperty, random, timePassed, randomPickOdds, calculateDuration, groupSimulateData);
    }
}
