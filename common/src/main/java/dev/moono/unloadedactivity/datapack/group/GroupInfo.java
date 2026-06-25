package dev.moono.unloadedactivity.datapack.group;

#if MC_VER >= MC_1_21_11
import dev.moono.unloadedactivity.impl.LookupShape;
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class GroupInfo {
    public #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id;
    // It's incomplete because it will be combined later because blocks may have multiple tags and multiple member infos assigned.
    // This should only be used for constructing GroupMemberInfos for blocks.
    public HashMap<Pair<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, Boolean>, IncompleteGroupMemberInfo> values;
    private LookupShape shape;
    private int width;
    private int height;
    private List<Vec3i> offsetsWithoutZero;

    public GroupInfo(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id, IncompleteGroupInfo incomplete) {
        this.id = id;

        if (incomplete.width.isEmpty())
            throw new RuntimeException("width has not been set.");

        this.width = incomplete.width.get();

        if (this.width < 0) {
            throw new RuntimeException("width cannot be less than 0.");
        }

        if (incomplete.height.isEmpty())
            throw new RuntimeException("height has not been set.");

        this.height = incomplete.height.get();

        if (this.height < 0) {
            throw new RuntimeException("height cannot be less than 0.");
        }

        if (incomplete.shape.isEmpty())
            throw new RuntimeException("shape has not been set.");

        this.shape = incomplete.shape.get();

        this.values = new HashMap<>(incomplete.values.size());
        for (var entry : incomplete.values.entrySet()) {
            var key = entry.getKey();
            IncompleteGroupMemberInfo incompleteGroupMemberInfo = entry.getValue();
            this.values.put(key, incompleteGroupMemberInfo);
        }


    }

    public List<Vec3i> getOffsetsWithoutZero() {
        if (this.offsetsWithoutZero == null) {
            ArrayList<Vec3i> offsetsWithoutZero = new ArrayList<>();
            for (Vec3i offset : this.iterateOffsets()) {
                if (offset.equals(Vec3i.ZERO))
                    continue;
                offsetsWithoutZero.add(new Vec3i(offset.getX(), offset.getY(), offset.getZ()));
            }
            this.offsetsWithoutZero = ImmutableList.copyOf(offsetsWithoutZero);
        }
        return this.offsetsWithoutZero;
    }

    private Iterable<Vec3i> iterateOffsets() {
        switch (this.shape) {
            case DIAMOND -> {
                return () -> new AbstractIterator<>() {
                    private final Iterator<BlockPos> iterator = BlockPos.withinManhattan(BlockPos.ZERO, width, height, width).iterator();
                    @Override
                    protected Vec3i computeNext() {
                        if (!iterator.hasNext())
                            return this.endOfData();

                        BlockPos nextPos = iterator.next();

                        int manhattanDistance = nextPos.distManhattan(BlockPos.ZERO);
                        int maxDist = Math.max(width, height);

                        if (manhattanDistance > maxDist)
                            return this.endOfData();

                        return nextPos;
                    }
                };
            }
            case BLOCK -> {
                return () -> new AbstractIterator<>() {
                    private final Iterator<BlockPos> iterator = BlockPos.withinManhattan(BlockPos.ZERO, width, height, width).iterator();
                    @Override
                    protected Vec3i computeNext() {
                        if (!iterator.hasNext())
                            return this.endOfData();

                        return iterator.next();
                    }
                };
            }
        }

        return () -> new AbstractIterator<>() {
            @Override
            protected Vec3i computeNext() {
                return this.endOfData();
            }
        };
    }
}
