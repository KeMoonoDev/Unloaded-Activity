package dev.moono.unloadedactivity;

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

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Optional;

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

    public static int getBlockIntId(Block block) {
        #if MC_VER >= MC_1_19_4
        var blockRegistry = BuiltInRegistries.BLOCK;
        #else
        var blockRegistry = Registry.BLOCK;
        #endif
        return blockRegistry.getId(block);
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

    public static long toLong(ChunkPos chunkPos) {
        #if MC_VER >= MC_26_1_2
        return chunkPos.pack();
        #else
        return chunkPos.toLong();
        #endif
    }

    public static ChunkPos chunkPosFromLong(long longPos) {
        #if MC_VER >= MC_26_1_2
        return ChunkPos.unpack(longPos);
        #else
        return new ChunkPos(longPos);
        #endif
    }

    public static boolean isValidGourdPosition(Direction direction, BlockPos pos, BlockState state, BlockGetter level) {
        BlockPos blockPos = pos.relative(direction);

        BlockState growAtState = level.getBlockState(blockPos);
        boolean isAir = growAtState.isAir();

        if (!isAir)
            return false;

        #if MC_VER >= MC_26_1_2
        Block block = state.getBlock();
        boolean isGrowableOn;
        if (block instanceof StemBlock stemBlock) {
            BlockState growOnState = level.getBlockState(blockPos.below());
            isGrowableOn = growOnState.is(stemBlock.fruitSupportBlocks);
        } else {
            isGrowableOn = true;
        }
        #else
        BlockState belowBlockState = level.getBlockState(blockPos.below());
        boolean isGrowableOn = (belowBlockState.is(Blocks.FARMLAND) || belowBlockState.is(BlockTags.DIRT));
        #endif

        return isGrowableOn;
    }

    public static Optional<Property<?>> getProperty(BlockState state, String propertyName) {
        for (var property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return Optional.of(property);
            }
        }
        return Optional.empty();
    }


    public static <R> DataResult<R> returnError(DataResult<?> dataResult) {
        #if MC_VER >= MC_1_19_4
        return DataResult.error(() -> dataResult.error().get().message());
        #else
        return DataResult.error(dataResult.error().get().message());
        #endif
    }

    public static <R> DataResult<R> returnError(String info, DataResult<?> dataResult) {
        #if MC_VER >= MC_1_19_4
        return DataResult.error(() -> info + dataResult.error().get().message());
        #else
        return DataResult.error(info + "\n" + dataResult.error().get().message());
        #endif
    }

    public static <R> DataResult<R> returnError(String info) {
        #if MC_VER >= MC_1_19_4
        return DataResult.error(() -> info);
        #else
        return DataResult.error(info);
        #endif
    }
}
