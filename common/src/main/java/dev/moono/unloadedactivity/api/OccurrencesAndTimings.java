package dev.moono.unloadedactivity.api;

import java.util.List;

public record OccurrencesAndTimings(int occurrences, List<Long> remainingTimes, long endTime) {
    public static OccurrencesAndTimings empty(SimulatedTime simulatedTime) {
        return OccurrencesAndTimings.fastDuration(0, simulatedTime);
    }

    public static OccurrencesAndTimings fastDuration(int occurrences, SimulatedTime simulatedTime) {
        return new OccurrencesAndTimings(occurrences, List.of(simulatedTime.remainingTime()), simulatedTime.endTime());
    }

    public SimulatedTime getTimeAtOccurrence(int occurrence) {
        // todo interpolate the remaining time if occurrences is larger than remainingTimes size.
        if (occurrence >= remainingTimes.size()) return new SimulatedTime(0, this.endTime());
        return new SimulatedTime(remainingTimes.get(occurrence), this.endTime());
    }

    public SimulatedTime getFinalTime() {
        return getTimeAtOccurrence(occurrences);
    }
}
