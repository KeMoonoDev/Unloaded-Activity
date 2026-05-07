package lol.zanspace.unloadedactivity;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

#if MC_VER >= MC_1_19_4
import net.minecraft.core.registries.BuiltInRegistries;
#else
import net.minecraft.core.Registry;
#endif

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class GameUtils {
    public static #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif getBlockId(Block block) {
        #if MC_VER >= MC_1_19_4
        var blockRegistry = BuiltInRegistries.BLOCK;
        #else
        var blockRegistry = Registry.BLOCK;
        #endif
        return blockRegistry.getKey(block);
    }

    public static boolean isValidGourdPosition(Direction direction, BlockPos pos, ServerLevel level) {
        BlockPos blockPos = pos.relative(direction);
        BlockState blockState = level.getBlockState(blockPos.below());
        return level.getBlockState(blockPos).isAir() && (blockState.is(Blocks.FARMLAND) || blockState.is(BlockTags.DIRT));
    }
}
