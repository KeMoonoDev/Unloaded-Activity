package dev.moono.unloadedactivity.api;

public record SimulatedTime(long remainingTime, long endTime) {
    public long currentTime() {
        return endTime()-remainingTime();
    }
}
