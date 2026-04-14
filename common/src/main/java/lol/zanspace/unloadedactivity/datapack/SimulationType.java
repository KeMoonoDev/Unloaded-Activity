package lol.zanspace.unloadedactivity.datapack;

import java.util.Optional;

public enum SimulationType {
    PROPERTY,
    BUDDING,
    DECAY,
    ACTION;

    public static Optional<SimulationType> fromString(String string) {
        switch (string.toLowerCase()) {
            case "property" -> {
                return Optional.of(PROPERTY);
            }
            case "budding" -> {
                return Optional.of(BUDDING);
            }
            case "decay" -> {
                return Optional.of(DECAY);
            }
            case "action" -> {
                return Optional.of(ACTION);
            }
        }
        return Optional.empty();
    }
}
