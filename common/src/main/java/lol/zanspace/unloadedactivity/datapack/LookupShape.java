package lol.zanspace.unloadedactivity.datapack;

import java.util.Optional;

public enum LookupShape {
    BLOCK,
    DIAMOND;

    static public Optional<LookupShape> fromString(String name) {
        switch (name.toLowerCase()) {
            case "diamond" -> {
                return Optional.of(DIAMOND);
            }
            case "block" -> {
                return Optional.of(BLOCK);
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
