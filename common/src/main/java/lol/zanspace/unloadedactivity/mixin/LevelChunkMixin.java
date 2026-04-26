package lol.zanspace.unloadedactivity.mixin;

import lol.zanspace.unloadedactivity.UnloadedActivity;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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
        if (level.isClientSide() || !UnloadedActivity.config.rememberBlockPositions)
            return;

        if (UnloadedActivity.config.debugLogs)
            UnloadedActivity.LOGGER.info("Placed "+blockState.getBlock().toString()+" at "+blockPos);

        if (!blockState.getBlock().hasRandTicks())
            return;

        if (UnloadedActivity.config.debugLogs)
            UnloadedActivity.LOGGER.info("Adding position to list "+blockPos.asLong());

        this.addSimulationBlock(blockPos.asLong());
    }

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("RETURN"))
    private void initLevelChunk(ServerLevel level, ProtoChunk protoChunk, LevelChunk.PostLoadProcessor postLoadProcessor, CallbackInfo ci) {
        if (protoChunk.getLastTick() == 0) {
            this.setLastTick(level.getDayTime());
        } else {
            this.setLastTick(protoChunk.getLastTick());
            for (var entry : protoChunk.getLastGroupTicks().entrySet()) {
                this.setLastGroupTick(entry.getKey(), entry.getValue());
            }
        }
        this.setSimulationVersion(protoChunk.getSimulationVersion());
        this.setSimulationBlocks(protoChunk.getSimulationBlocks());
    }
}
