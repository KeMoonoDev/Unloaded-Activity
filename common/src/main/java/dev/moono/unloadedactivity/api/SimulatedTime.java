package dev.moono.unloadedactivity.api;

public record SimulatedTime (long remainingTicks, long endDayTick, long endGameTick, long endMs){
    public static final SimulatedTime PLACEHOLDER = new SimulatedTime(0, -1, -1, -1);

    public static SimulatedTime fromLastMs(long lastMs, long endDayTick, long endGameTick, long endMs) {
        return new SimulatedTime(Math.max(endMs - lastMs, 0) / 50, endDayTick, endGameTick, endMs);
    }

    public static SimulatedTime fromLastDayTick(long lastDayTick, long endDayTick, long endGameTick, long endMs) {
        return new SimulatedTime(Math.max(endDayTick - lastDayTick, 0), endDayTick, endGameTick, endMs);
    }

    public static SimulatedTime fromLastGameTick(long lastGameTick, long endDayTick, long endGameTick, long endMs) {
        return new SimulatedTime(Math.max(endGameTick - lastGameTick, 0), endDayTick, endGameTick, endMs);
    }

    public static SimulatedTime fromRemainingTicks(long remainingTicks, long endDayTick, long endGameTick, long endMs) {
        return new SimulatedTime(remainingTicks, endDayTick, endGameTick, endMs);
    }

    public long currentDayTicks()  {
        return endDayTick()-remainingTicks();
    }
    public long currentGameTicks() {
        return endGameTick()-remainingTicks();
    }
    public long remainingMs() {
        return remainingTicks() * 50;
    };
    public long currentMs() {
        return endMs-remainingMs();
    }

    // Creates a new SimulatedTime which starts n ticks after this one.
    public SimulatedTime passTicks(long ticks) {
        return SimulatedTime.fromRemainingTicks(remainingTicks - ticks, endDayTick, endGameTick, endMs);
    }

    // Creates a new SimulatedTime which only has the first n ticks from this one.
    public SimulatedTime subTicks(long ticks) {
        return SimulatedTime.fromRemainingTicks(ticks, currentDayTicks() + ticks, currentGameTicks() + ticks, currentMs() + ticks * 50);
    }
}
