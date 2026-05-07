package lol.zanspace.unloadedactivity.mixin;

import lol.zanspace.unloadedactivity.GroupChunkIndex;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.datapack.GroupInfoResource;
import lol.zanspace.unloadedactivity.datapack.GroupMemberInfo;
import lol.zanspace.unloadedactivity.datapack.SimulateProperty;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccess {

    @Shadow @Final
    Level level;

    #if MC_VER >= MC_1_21_10

    public LevelChunkMixin(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, PalettedContainerFactory palettedContainerFactory, long l, @Nullable LevelChunkSection[] levelChunkSections, @Nullable BlendingData blendingData) {
        super(chunkPos, upgradeData, levelHeightAccessor, palettedContainerFactory, l, levelChunkSections, blendingData);
    }
    #else
    public LevelChunkMixin(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, Registry<Biome> registry, long l, @Nullable LevelChunkSection[] levelChunkSections, @Nullable BlendingData blendingData) {
        super(chunkPos, upgradeData, levelHeightAccessor, registry, l, levelChunkSections, blendingData);
    }
    #endif

    @Inject(
            at = @At(
                    value="INVOKE",
                    target="net/minecraft/world/level/chunk/LevelChunkSection.setBlockState (IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            method = "setBlockState"
    )
    #if MC_VER >= MC_1_21_5
    public void blockChanged(BlockPos blockPos, BlockState blockState, int i, CallbackInfoReturnable<BlockState> cir) {
    #else
    public void blockChanged(BlockPos blockPos, BlockState blockState, boolean bl, CallbackInfoReturnable<BlockState> cir) {
    #endif
        if (level.isClientSide())
            return;

        Block block = blockState.getBlock();

        if (UnloadedActivity.config.debugLogs)
            UnloadedActivity.LOGGER.info("Placed "+block+" at "+blockPos);

        List<GroupMemberInfo> memberInfoList = GroupInfoResource.getBlockMemberInfo(block);

        if (!memberInfoList.isEmpty()) {
            var groupIndexes = this.getGroupIndexes();

            for (var memberInfo : memberInfoList) {
                var groupId = memberInfo.groupInfo.id;
                if (UnloadedActivity.config.debugLogs)
                    UnloadedActivity.LOGGER.info("Adding position to group list " + groupId + " " + blockPos.asLong());

                var positions = groupIndexes
                        .computeIfAbsent(groupId, (id) -> new GroupChunkIndex(new ArrayList<>(), this.getLastTick(), id))
                        .getPositions();

                if (positions.contains(blockPos.asLong()))
                    continue;

                positions.add(blockPos.asLong());
            }
        }

        if (block.getSimulationData().hasRandTicksWithoutGroup) {
            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Adding position to chunk list "+blockPos.asLong());

            this.addSimulationBlock(blockPos.asLong());
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("RETURN"))
    private void initLevelChunk(ServerLevel level, ProtoChunk protoChunk, LevelChunk.PostLoadProcessor postLoadProcessor, CallbackInfo ci) {
        if (protoChunk.getLastTick() == 0) {
            this.setLastTick(level.getDayTime());
        } else {
            this.setLastTick(protoChunk.getLastTick());
        }
        this.setGroupIndexes(protoChunk.getGroupIndexes());
        this.setSimulationVersion(protoChunk.getSimulationVersion());
        this.setSimulationBlocks(protoChunk.getSimulationBlocks());
    }
}
