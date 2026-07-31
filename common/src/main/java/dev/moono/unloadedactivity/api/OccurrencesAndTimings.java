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
        if (occurrence >= remainingTimes.size()) {
            if (occurrence >= occurrences) return new SimulatedTime(0, this.endTime());
            int missingOccurrences = occurrences - remainingTimes.size();
            int whichMissingOccurrenceToUse = occurrence - remainingTimes.size();
            long lastRemainingTime = remainingTimes.get(remainingTimes.size() - 1);
            long occurrenceSize = lastRemainingTime / (missingOccurrences + 1);
            long multiply = missingOccurrences - whichMissingOccurrenceToUse;
            return new SimulatedTime(occurrenceSize * multiply, this.endTime());
        }
        return new SimulatedTime(remainingTimes.get(occurrence), this.endTime());
    }

    public SimulatedTime getFinalTime() {
        return getTimeAtOccurrence(occurrences);
    }
}
