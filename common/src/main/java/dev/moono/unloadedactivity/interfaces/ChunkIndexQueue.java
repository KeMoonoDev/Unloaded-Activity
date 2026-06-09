package dev.moono.unloadedactivity.interfaces;

import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Optional;

public interface ChunkIndexQueue {
    void addChunkToQueueFront(LevelChunk chunk);
    void addChunkToQueue(LevelChunk chunk);
}
