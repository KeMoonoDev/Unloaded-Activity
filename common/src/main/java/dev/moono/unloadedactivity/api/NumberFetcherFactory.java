package dev.moono.unloadedactivity.api;

public interface NumberFetcherFactory {
    String namespace();
    boolean matches(String path);
    NumberFetcher create(String path);
}
