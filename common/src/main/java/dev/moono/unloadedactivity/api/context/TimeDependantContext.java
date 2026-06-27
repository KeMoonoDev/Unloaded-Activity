package dev.moono.unloadedactivity.api.context;

public interface TimeDependantContext extends FixedContext {
    long getCurrentTime();
}
