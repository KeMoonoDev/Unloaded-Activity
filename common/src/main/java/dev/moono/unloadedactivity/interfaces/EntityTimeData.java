package dev.moono.unloadedactivity.interfaces;

public interface EntityTimeData {
    long getLastDayTick();

    void setLastDayTick(long tick);

    long getLastGameTick();

    void setLastGameTick(long tick);

    long getLastMs();

    void setLastMs(long ms);
}
