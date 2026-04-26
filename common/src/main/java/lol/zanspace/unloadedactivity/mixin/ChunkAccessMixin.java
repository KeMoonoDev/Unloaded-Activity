package lol.zanspace.unloadedactivity.mixin;

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

    // Last time the chunk was ticked.
    @Unique
    private Map<ResourceLocation, Long> lastGroupTicks = new HashMap<>();

    // If the simulationVersion mismatches with the version in the mod then simulationBlocks will be reset.
    // This is so that if one version of the mod doesn't support a specific block but the next one does,
    // the block will get included once simulationBlocks is reset.
    @Unique
    private long simulationVersion = 0;

    // List of block positions that can be simulated.
    @Unique
    private ArrayList<Long> simulationBlocks = new ArrayList<>();

    // List of block positions that have a group assigned to them.
    @Unique
    private HashMap<ResourceLocation, ArrayList<BlockPos>> groupedBlocks = new HashMap<>();

    @Override
    public long getLastTick() {
        return this.lastTick;
    }

    @Override
    public void setLastTick(long tick) {
        this.lastTick = tick;

        // Reset group ticks because if a tick has been simulated in a chunk, all group ticks in that chunk have also been simulated.
        if (!this.lastGroupTicks.isEmpty())
            this.lastGroupTicks = new HashMap<>();
    }

    @Override
    public Map<ResourceLocation, Long> getLastGroupTicks() {
        return this.lastGroupTicks;
    }

    @Override
    public long getLastGroupTick(ResourceLocation groupId) {
        Long lastGroupTick = lastGroupTicks.get(groupId);

        if (lastGroupTick != null) {
            return Math.max(lastGroupTick, lastTick);
        }

        return lastTick;
    }

    @Override
    public void setLastGroupTick(ResourceLocation groupId, long tick) {
       lastGroupTicks.put(groupId, tick);
    }

    @Override
    public long getSimulationVersion() {
        return this.simulationVersion;
    }

    @Override
    public void setSimulationVersion(long ver) {
        this.simulationVersion = ver;
    }

    @Override
    public HashMap<ResourceLocation, ArrayList<BlockPos>> getGroupedBlocks() {
        return groupedBlocks;
    };

    @Override
    public void setGroupedBlocks(HashMap<ResourceLocation, ArrayList<BlockPos>> groupedBlocks) {
        this.groupedBlocks = groupedBlocks;
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
