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
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

/// A bunch of functions that are used frequently with version specific logic.
/// Separated into here to reduce clutter elsewhere.
public class GameUtils {
    public static DefaultedRegistry<EntityType<?>> getEntityTypeRegistry() {
        #if MC_VER >= MC_1_19_4
        return BuiltInRegistries.ENTITY_TYPE;
        #else
        return Registry.ENTITY_TYPE;
        #endif
    }

    public static DefaultedRegistry<Block> getBlockRegistry() {
        #if MC_VER >= MC_1_19_4
        return BuiltInRegistries.BLOCK;
        #else
        return Registry.BLOCK;
        #endif
    }

    public static #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif parseId(String unparsedId) {
        #if MC_VER >= MC_1_21_11
        return Identifier.parse(unparsedId);
        #elif MC_VER >= MC_1_21_1
        return ResourceLocation.parse(unparsedId);
        #else
        return new ResourceLocation(unparsedId);
        #endif
    }

    @Nullable
    public static EntityType<?> getEntityType(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif blockId) {
        return getEntityTypeRegistry().getOptional(blockId).orElse(null);
    }

    public static Stream<TagKey<Block>> getBlockTags(Block block) {
        return block.defaultBlockState()
            #if MC_VER <= MC_1_21_11
            .getTags();
            #else
            .typeHolder()
            .tags();
            #endif
    }

    @Nullable
    public static Block getBlock(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif blockId) {
        return getBlockRegistry().getOptional(blockId).orElse(null);
    }

    public static #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif getBlockId(Block block) {
        return getBlockRegistry().getKey(block);
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

    public static boolean canGrow(ServerLevel level, BlockPos basePos, #if MC_VER >= MC_26_2 SpeleothemBlock speleothemBlock #else PointedDripstoneBlock pointedDripstoneBlock #endif) {
        #if MC_VER >= MC_26_2
        return speleothemBlock.canGrow(level, basePos);
        #else
        BlockState rootState = level.getBlockState(basePos.above(1));
        BlockState aboveState = level.getBlockState(basePos.above(2));
        return PointedDripstoneBlock.canGrow(rootState, aboveState);
        #endif
    }

    public static boolean isFreeHangingStalactite(final BlockState tipState) {
        #if MC_VER >= MC_26_2
        return SpeleothemBlock.isFreeHangingStalactite(tipState);
        #else
        return PointedDripstoneBlock.canDrip(tipState);
        #endif
    }

    public static boolean blocksStalagmiteScan(final LevelReader level, final BlockPos pos, final BlockState state, #if MC_VER >= MC_26_2 SpeleothemBlock speleothemBlock #else PointedDripstoneBlock pointedDripstoneBlock #endif) {
        #if MC_VER >= MC_26_2
        return speleothemBlock.blocksStalagmiteScan(level, pos, state);
        #else
        return !PointedDripstoneBlock.canDripThrough(level, pos, state);
        #endif
    }

    public static boolean isUnmergedTipWithDirection(BlockState state, Direction direction, #if MC_VER >= MC_26_2 SpeleothemBlock speleothemBlock #else PointedDripstoneBlock pointedDripstoneBlock #endif) {
        #if MC_VER >= MC_26_2
        return speleothemBlock.isUnmergedTipWithDirection(state, direction);
        #else
        return PointedDripstoneBlock.isUnmergedTipWithDirection(state, direction);
        #endif
    }

    public static boolean canTipGrow(final BlockState tipState, final ServerLevel level, final BlockPos tipPos, #if MC_VER >= MC_26_2 SpeleothemBlock speleothemBlock #else PointedDripstoneBlock pointedDripstoneBlock #endif ) {
        #if MC_VER >= MC_26_2
        return speleothemBlock.canTipGrow(tipState, level, tipPos);
        #else
        return PointedDripstoneBlock.canTipGrow(tipState, level, tipPos);
        #endif
    }

    public static boolean isValidSpeleothemPlacement(final ServerLevel level, final BlockPos pos, final Direction tipDirection, #if MC_VER >= MC_26_2 SpeleothemBlock speleothemBlock #else PointedDripstoneBlock pointedDripstoneBlock #endif ) {
        #if MC_VER >= MC_26_2
        return speleothemBlock.isValidSpeleothemPlacement(level, pos, tipDirection);
        #else
        return PointedDripstoneBlock.isValidPointedDripstonePlacement(level, pos, tipDirection);
        #endif
    }

    public static boolean isStalactiteStartPos(BlockState state, ServerLevel level, BlockPos pos) {
        #if MC_VER >= MC_26_2
        return SpeleothemBlock.isStalactiteStartPos(state, level, pos);
        #else
        return PointedDripstoneBlock.isStalactiteStartPos(state, level, pos);
        #endif
    }

    public static @Nullable BlockPos findTip(BlockState state, ServerLevel level, BlockPos pos, int maxSearchLength, boolean includeMergedTip) {
        #if MC_VER >= MC_26_2
        return SpeleothemBlock.findTip(state, level, pos, maxSearchLength, includeMergedTip);
        #else
        return PointedDripstoneBlock.findTip(state, level, pos, maxSearchLength, includeMergedTip);
        #endif
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
