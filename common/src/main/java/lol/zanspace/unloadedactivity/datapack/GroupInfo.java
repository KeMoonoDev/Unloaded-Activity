package lol.zanspace.unloadedactivity.datapack;

#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import com.google.common.collect.AbstractIterator;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Vec3i;

import java.util.HashMap;

public class GroupInfo {
    public #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id;
    // It's incomplete because it will be combined later because blocks may have multiple tags and multiple member infos assigned.
    // This should only be used for constructing GroupMemberInfos for blocks.
    public HashMap<Pair<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, Boolean>, IncompleteGroupMemberInfo> values;
    public LookupShape shape;
    public int width;
    public int height;

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

    public Iterable<Vec3i> iterateOffsets() {
        switch (this.shape) {
            case DIAMOND -> {
                return () -> new AbstractIterator<>() {
                    private Vec3i cursor = new Vec3i(0, -height, -width);
                    @Override
                    protected Vec3i computeNext() {
                        if (cursor == null)
                            return this.endOfData();

                        Vec3i toReturn = cursor;

                        int widthAtLine = width - Math.abs(cursor.getZ());
                        if (cursor.getX() < widthAtLine) {
                            cursor = cursor.offset(new Vec3i(1, 0, 0));
                        } else if (cursor.getZ() < width) {
                            int newZ = cursor.getZ() + 1;
                            int newWidthAtLine = width - Math.abs(newZ);
                            cursor = new Vec3i(-newWidthAtLine, cursor.getY(), newZ);
                        } else if (cursor.getY() < height) {
                            cursor = new Vec3i(0, cursor.getY() + 1, -width);
                        } else {
                            cursor = null;
                        }

                        return toReturn;
                    }
                };
            }
            case BLOCK -> {
                return () -> new AbstractIterator<>() {
                    private Vec3i cursor = new Vec3i(-width, -height, -width);
                    @Override
                    protected Vec3i computeNext() {
                        if (cursor == null)
                            return this.endOfData();

                        Vec3i toReturn = cursor;

                        if (cursor.getX() < width) {
                            cursor = cursor.offset(new Vec3i(1, 0, 0));
                        } else if (cursor.getZ() < width) {
                            int newZ = cursor.getZ() + 1;
                            cursor = new Vec3i(-width, cursor.getY(), newZ);
                        } else if (cursor.getY() < height) {
                            cursor = new Vec3i(-width, cursor.getY() + 1, -width);
                        } else {
                            cursor = null;
                        }

                        return toReturn;
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
