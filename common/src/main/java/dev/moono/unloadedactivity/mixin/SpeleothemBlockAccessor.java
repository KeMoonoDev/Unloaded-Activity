package dev.moono.unloadedactivity.mixin;

#if MC_VER >= MC_26_2
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpeleothemBlock.class)
public interface SpeleothemBlockAccessor {
   @Accessor("MAX_STALAGMITE_SEARCH_RANGE_WHEN_GROWING")
   static int unloadedactivity$getMaxStalagmiteSearchRangeWhenGrowing() {
       throw new AssertionError();
   }

   @Invoker("getMaxGrowthLength")
   int unloadedactivity$invokeGetMaxGrowthLength();

   @Invoker("isUnmergedTipWithDirection")
   boolean unloadedactivity$invokeIsUnmergedTipWithDirection(BlockState state, Direction tipDirection);

   @Invoker("isValidSpeleothemPlacement")
   boolean unloadedactivity$invokeIsValidSpeleothemPlacement(LevelReader level, BlockPos pos, Direction tipDirection);

   @Invoker("blocksStalagmiteScan")
   boolean unloadedactivity$invokeBlocksStalagmiteScan(LevelReader level, BlockPos pos, BlockState state);

   @Invoker("isStalactiteStartPos")
   static boolean unloadedactivity$invokeIsStalactiteStartPos(BlockState state, LevelReader level, BlockPos pos) {
       throw new AssertionError();
   }

   @Invoker("findTip")
   static @Nullable BlockPos unloadedactivity$invokeFindTip(BlockState dripstoneState, LevelAccessor level, BlockPos dripstonePos, int maxSearchLength, boolean includeMergedTip) {
       throw new AssertionError();
   }

   @Invoker("canGrow")
   boolean unloadedactivity$invokeCanGrow(LevelReader level, BlockPos pos);

   @Invoker("canTipGrow")
   boolean unloadedactivity$invokeCanTipGrow(BlockState tipState, ServerLevel level, BlockPos tipPos);

   @Invoker("grow")
   void unloadedactivity$invokeGrow(ServerLevel level, BlockPos growFromPos, Direction growToDirection);

   @Invoker("growStalagmiteBelow")
   void unloadedactivity$invokeGrowStalagmiteBelow(ServerLevel level, BlockPos posAboveStalagmite);

   @Invoker("isFreeHangingStalactite")
   static boolean unloadedactivity$invokeIsFreeHangingStalactite(BlockState state) {
       throw new AssertionError();
   }
}
#else
import net.minecraft.world.level.block.AirBlock;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(AirBlock.class)
public interface SpeleothemBlockAccessor { }
#endif