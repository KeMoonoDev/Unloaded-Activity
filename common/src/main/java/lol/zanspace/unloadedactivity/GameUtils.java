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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/// A bunch of functions that are used frequently with version specific logic.
/// Separated into here to reduce clutter elsewhere.
public class GameUtils {
    public static #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif getBlockId(Block block) {
        #if MC_VER >= MC_1_19_4
        var blockRegistry = BuiltInRegistries.BLOCK;
        #else
        var blockRegistry = Registry.BLOCK;
        #endif
        return blockRegistry.getKey(block);
    }

    public static long getTime(Level level) {
        #if MC_VER >= MC_26_1_2
        return level.getDefaultClockTime();
        #else
        return level.getDayTime();
        #endif
    }

    public static ChunkPos chunkPosFromWorldPos(BlockPos pos) {
        #if MC_VER >= MC_26_1_2
        return ChunkPos.containing(pos);
        #else
        return new ChunkPos(pos);
        #endif
    }

    public static boolean isChunkLoaded(Level level, ChunkPos chunkPos) {
        #if MC_VER >= MC_26_1_2
        return level.hasChunk(chunkPos.x(), chunkPos.z());
        #else
        return level.hasChunk(chunkPos.x, chunkPos.z);
        #endif
    }

    public static LevelChunk getChunk(Level level, ChunkPos chunkPos) {
        #if MC_VER >= MC_26_1_2
        return level.getChunk(chunkPos.x(), chunkPos.z());
        #else
        return level.getChunk(chunkPos.x, chunkPos.z);
        #endif
    }

    public static RandomSource getRand(Level level) {
        #if MC_VER >= MC_26_1_2
        return level.getRandom();
        #else
        return level.random;
        #endif
    }

    public static boolean isValidGourdPosition(Direction direction, BlockPos pos, ServerLevel level) {
        BlockPos blockPos = pos.relative(direction);
        BlockState blockState = level.getBlockState(blockPos.below());
        return level.getBlockState(blockPos).isAir() && (blockState.is(Blocks.FARMLAND) || blockState.is(BlockTags.DIRT));
    }
}
