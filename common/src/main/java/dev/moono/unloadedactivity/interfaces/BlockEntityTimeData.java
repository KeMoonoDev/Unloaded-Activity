package dev.moono.unloadedactivity.interfaces;

public interface BlockEntityTimeData {
    long getLastTick();

    void setLastTick(long tick);

    long getLastMs();

    void setLastMs(long ms);
}
