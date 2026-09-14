package dev.moono.unloadedactivity.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiPredicate;

@Mixin(PointedDripstoneBlock.class)
public interface PointedDripstoneBlockAccessor {
    #if MC_VER <= MC_26_1_2
    @Accessor("MAX_GROWTH_LENGTH")
    static int unloadedactivity$getMaxGrowthLength() {
        throw new AssertionError();
    }

    @Accessor("MAX_STALAGMITE_SEARCH_RANGE_WHEN_GROWING")
    static int unloadedactivity$getMaxStalagmiteSearchRangeWhenGrowing() {
        throw new AssertionError();
    }

    @Invoker("isUnmergedTipWithDirection")
    static boolean unloadedactivity$invokeIsUnmergedTipWithDirection(BlockState state, Direction tipDirection) {
        throw new AssertionError();
    }

    @Invoker("isValidPointedDripstonePlacement")
    static boolean unloadedactivity$invokeIsValidPointedDripstonePlacement(LevelReader level, BlockPos pos, Direction tipDirection) {
        throw new AssertionError();
    }

    @Invoker("canDripThrough")
    static boolean unloadedactivity$invokeCanDripThrough(BlockGetter level, BlockPos pos, BlockState state) {
        throw new AssertionError();
    }

    @Invoker("isStalactiteStartPos")
    static boolean unloadedactivity$invokeIsStalactiteStartPos(BlockState state, LevelReader level, BlockPos pos) {
        throw new AssertionError();
    }

    @Invoker("findTip")
    static @Nullable BlockPos unloadedactivity$invokeFindTip(BlockState dripstoneState, LevelAccessor level, BlockPos dripstonePos, int maxSearchLength, boolean includeMergedTip) {
        throw new AssertionError();
    }

    @Invoker("canGrow")
    static boolean unloadedactivity$invokeCanGrow(BlockState rootState, BlockState aboveState) {
        throw new AssertionError();
    }

    @Invoker("canTipGrow")
    static boolean unloadedactivity$invokeCanTipGrow(BlockState tipState, ServerLevel level, BlockPos tipPos) {
        throw new AssertionError();
    }

    @Invoker("grow")
    static void unloadedactivity$invokeGrow(ServerLevel level, BlockPos growFromPos, Direction growToDirection) {
        throw new AssertionError();
    }

    @Invoker("growStalagmiteBelow")
    static void unloadedactivity$invokeGrowStalagmiteBelow(ServerLevel level, BlockPos posAboveStalagmite) {
        throw new AssertionError();
    }
    #endif

    @Invoker("findFillableCauldronBelowStalactiteTip")
    static @Nullable BlockPos unloadedactivity$invokeFindFillableCauldronBelowStalactiteTip(Level level, BlockPos stalactiteTipPos, Fluid fluid) {
        throw new AssertionError();
    }
}