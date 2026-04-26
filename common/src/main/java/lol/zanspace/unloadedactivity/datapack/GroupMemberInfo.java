package lol.zanspace.unloadedactivity.datapack;

public class GroupMemberInfo {
    public double value;
    public GroupInfo groupInfo;

    public GroupMemberInfo(IncompleteGroupMemberInfo incomplete, GroupInfo groupInfo) {
        if (incomplete.value.isEmpty())
            throw new RuntimeException("value has not been set.");

        this.value = incomplete.value.get();
        this.groupInfo = groupInfo;
    }
}
