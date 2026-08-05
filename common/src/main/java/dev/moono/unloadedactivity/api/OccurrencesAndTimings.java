package dev.moono.unloadedactivity.api;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record OccurrencesAndTimings(int occurrences, List<Long> remainingTicks, long endTick, long endMs) {
    public static OccurrencesAndTimings empty(SimulatedTime simulatedTime) {
        return OccurrencesAndTimings.fastDuration(0, simulatedTime);
    }

    public static OccurrencesAndTimings fastDuration(int occurrences, SimulatedTime simulatedTime) {
        return new OccurrencesAndTimings(
                occurrences,
                List.of(simulatedTime.remainingTicks()),
                simulatedTime.endTick(),
                simulatedTime.endMs()
        );
    }

    public SimulatedTime getTimeAtOccurrence(int occurrence) {
        if (occurrence >= remainingTicks.size()) {
            if (occurrence >= occurrences) return new SimulatedTime(0, endTick, endMs);
            int missingOccurrences = occurrences - remainingTicks.size();
            int whichMissingOccurrenceToUse = occurrence - remainingTicks.size();
            long lastRemainingTime = remainingTicks.get(remainingTicks.size() - 1);
            long occurrenceSize = lastRemainingTime / (missingOccurrences + 1);
            long multiply = missingOccurrences - whichMissingOccurrenceToUse;

            long remainingTicks = occurrenceSize * multiply;

            return new SimulatedTime(remainingTicks, endTick, endMs);
        }

        return new SimulatedTime(remainingTicks.get(occurrence), endTick, endMs);
    }

    public SimulatedTime getFinalTime() {
        return getTimeAtOccurrence(occurrences);
    }
}
