package lol.zanspace.unloadedactivity.api;

public record TypeDescriptor(FieldType type, ContainerKind container) {
    public boolean is(FieldType t, ContainerKind c) {
        return type == t && container == c;
    }
}
