package dev.moono.unloadedactivity.mixin;

#if MC_VER >= MC_1_21_11
#else
import net.minecraft.resources.ResourceLocation;
#endif

import dev.moono.unloadedactivity.GroupChunkIndex;
import dev.moono.unloadedactivity.interfaces.ChunkTimeData;
        import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;

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
    private ArrayList<GroupChunkIndex> groupIndexes = new ArrayList<>();

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


    public ArrayList<GroupChunkIndex> getGroupIndexes() {return groupIndexes;};

    public void setGroupIndexes(ArrayList<GroupChunkIndex> groupIndexes) {
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
