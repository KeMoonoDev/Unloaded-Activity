package dev.moono.unloadedactivity.api;

public record SimulatedTime (long remainingTicks, long endTick, long endMs){
    public static final SimulatedTime PLACEHOLDER = new SimulatedTime(0, -1, -1);

    public static SimulatedTime fromLastMs(long lastMs, long endTick, long endMs) {
        return new SimulatedTime(Math.max(endMs - lastMs, 0) / 50, endTick, endMs);
    }

    public static SimulatedTime fromLastTick(long lastTick, long endTick, long endMs) {
        return new SimulatedTime(Math.max(endTick - lastTick, 0), endTick, endMs);
    }

    public static SimulatedTime fromRemainingMs(long remainingMs, long endTick, long endMs) {
        return new SimulatedTime(remainingMs / 50, endTick, endMs);
    }

    public static SimulatedTime fromRemainingTicks(long remainingTicks, long endTick, long endMs) {
        return new SimulatedTime(remainingTicks, endTick, endMs);
    }

    public long remainingMs() {
        return remainingTicks() * 50;
    };
    public long currentMs() {
        return endMs-remainingMs();
    }
    public long currentTick()  {
        return endTick()-remainingTicks();
    }

    // Creates a new SimulatedTime which starts n ticks after this one.
    public SimulatedTime passTicks(long ticks) {
        return SimulatedTime.fromRemainingTicks(remainingTicks - ticks, endTick, endMs);
    }

    // Creates a new SimulatedTime which only has the first n ticks from this one.
    public SimulatedTime subTicks(long ticks) {
        return SimulatedTime.fromRemainingTicks(ticks, currentTick() + ticks, currentMs() + ticks * 50);
    }
}
