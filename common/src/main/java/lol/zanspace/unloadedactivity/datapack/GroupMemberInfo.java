package lol.zanspace.unloadedactivity.datapack;

import net.minecraft.core.Vec3i;

import java.util.List;

public class GroupMemberInfo {
    public float value;
    public GroupInfo groupInfo;
    public List<Vec3i> ignoredOffsets;

    public GroupMemberInfo(IncompleteGroupMemberInfo incomplete, GroupInfo groupInfo) {
        if (incomplete.value.isEmpty())
            throw new RuntimeException("value has not been set.");

        this.value = incomplete.value.get();
        this.groupInfo = groupInfo;
        this.ignoredOffsets = incomplete.ignoredOffsets.stream().toList();
    }
}
