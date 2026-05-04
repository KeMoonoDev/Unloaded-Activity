package lol.zanspace.unloadedactivity.mixin;

import com.mojang.datafixers.util.Pair;
import lol.zanspace.unloadedactivity.GroupChunkIndex;
import lol.zanspace.unloadedactivity.interfaces.ChunkTimeData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements ChunkTimeData {

    // Last time the chunk was ticked.
    @Unique
    private long lastTick = 0;

    // If the simulationVersion mismatches with the version in the mod then simulationBlocks will be reset.
    // This is so that if one version of the mod doesn't support a specific block but the next one does,
    // the block will get included once simulationBlocks is reset.
    @Unique
    private long simulationVersion = 0;

    // List of block positions that can be simulated.
    @Unique
    private ArrayList<Long> simulationBlocks = new ArrayList<>();

    // All groups in the chunk with their blocks and when they were last ticked.
    @Unique
    private HashMap<ResourceLocation, GroupChunkIndex> groupIndexes = new HashMap<>();

    @Override
    public long getLastTick() {
        return this.lastTick;
    }

    @Override
    public void setLastTick(long tick) {
        this.lastTick = tick;
    }

    @Override
    public long getSimulationVersion() {
        return this.simulationVersion;
    }

    @Override
    public void setSimulationVersion(long ver) {
        this.simulationVersion = ver;
    }


    public HashMap<ResourceLocation, GroupChunkIndex> getGroupIndexes() {return groupIndexes;};

    public void setGroupIndexes(HashMap<ResourceLocation, GroupChunkIndex> groupIndexes) {
        this.groupIndexes = groupIndexes;
    };

    @Override
    public ArrayList<Long> getSimulationBlocks() {
        return simulationBlocks;
    }

    @Override
    public void setSimulationBlocks(ArrayList<Long> positions) {
        this.simulationBlocks = positions;
    }

    @Override
    public void addSimulationBlock(long blockPos) {

        if (this.simulationBlocks.contains(blockPos))
            return;

        this.simulationBlocks.add(blockPos);
    }

    @Override
    public void removeSimulationBlock(long blockPos) {

        int blockPosIndex = this.simulationBlocks.indexOf(blockPos);

        if (blockPosIndex < 0)
            return;

        this.simulationBlocks.remove(blockPosIndex);
    }
}
