package dev.moono.unloadedactivity.api.number_fetcher;

public interface NumberFetcherFactory {
    String namespace();
    boolean matches(String path);
    NumberFetcher create(String path);
}
