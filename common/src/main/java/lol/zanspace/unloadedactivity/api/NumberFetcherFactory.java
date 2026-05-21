package lol.zanspace.unloadedactivity.api;

import net.minecraft.resources.Identifier;

public interface NumberFetcherFactory {
    String namespace();
    boolean matches(String path);
    NumberFetcher create(String path);
}
