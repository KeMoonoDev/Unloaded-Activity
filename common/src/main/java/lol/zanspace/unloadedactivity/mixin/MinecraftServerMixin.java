package lol.zanspace.unloadedactivity.mixin;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import lol.zanspace.unloadedactivity.TimeMachine;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.interfaces.ChunkIndexQueue;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements ChunkIndexQueue {

    @Shadow @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;

    @Shadow
    private ProfilerFiller profiler;

    @Unique
    private final ArrayDeque<Pair<ResourceKey<Level>, ChunkPos>> chunkQueue = new ArrayDeque<>();

    @Unique
    private Optional<LevelChunk> getNextValidChunk() {
        while (true) {
            Pair<ResourceKey<Level>, ChunkPos> next = chunkQueue.poll();

            if (next == null) {
                return Optional.empty();
            }

            ResourceKey<Level> levelKey = next.getFirst();
            ChunkPos chunkPos = next.getSecond();

            ServerLevel level = levels.get(levelKey);

            if (level == null) {
                continue;
            }

            boolean chunkIsLoaded = level.hasChunk(chunkPos.x, chunkPos.z);
            if (!chunkIsLoaded) {
                continue;
            }

            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            return Optional.of(chunk);
        }
    }

    @Override
    public void addChunkToQueueFront(LevelChunk chunk) {
        if (TimeMachine.isChunkIndexed(chunk))
            return;

        chunkQueue.addFirst(Pair.of(chunk.getLevel().dimension(), chunk.getPos()));
    }

    @Override
    public void addChunkToQueue(LevelChunk chunk) {
        if (TimeMachine.isChunkIndexed(chunk))
            return;

        chunkQueue.add(Pair.of(chunk.getLevel().dimension(), chunk.getPos()));
    }

    @Inject(method = "tickChildren", at = @At("HEAD"))
    public void tickChildren(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        profiler.push("indexingChunks");
        int budget = UnloadedActivity.config.maxChunksIndexedPerTick;

        while (budget > 0) {
            Optional<LevelChunk> nextChunk = getNextValidChunk();

            if (nextChunk.isEmpty()) {
                break;
            }

            LevelChunk chunk = nextChunk.get();

            if (TimeMachine.isChunkIndexed(chunk)) {
                continue;
            }

            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Indexing chunk at " + chunk.getLevel().dimension() + " " + chunk.getPos());

            TimeMachine.indexChunk(chunk);
            budget--;
        }
        profiler.pop();
    }
}
