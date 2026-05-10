package lol.zanspace.unloadedactivity.platform;

import java.util.ServiceLoader;

public class ImplLoader {
    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }
}