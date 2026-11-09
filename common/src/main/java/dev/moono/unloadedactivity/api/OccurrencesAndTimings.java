package dev.moono.unloadedactivity.api;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record OccurrencesAndTimings(int occurrences, List<Long> remainingTicks, long endDayTick, long endGameTick, long endMs) {
    public static OccurrencesAndTimings empty(SimulatedTime simulatedTime) {
        return OccurrencesAndTimings.fastDuration(0, simulatedTime);
    }

    public static OccurrencesAndTimings fastDuration(int occurrences, SimulatedTime simulatedTime) {
        return new OccurrencesAndTimings(
                occurrences,
                List.of(simulatedTime.remainingTicks()),
                simulatedTime.endDayTick(),
                simulatedTime.endGameTick(),
                simulatedTime.endMs()
        );
    }

    public SimulatedTime getTimeAtOccurrence(int occurrence) {
        if (occurrence >= remainingTicks.size()) {
            if (occurrence >= occurrences) return new SimulatedTime(0, endDayTick, endGameTick, endMs);
            int missingOccurrences = occurrences - remainingTicks.size();
            int whichMissingOccurrenceToUse = occurrence - remainingTicks.size();
            long lastRemainingTime = remainingTicks.get(remainingTicks.size() - 1);
            long occurrenceSize = lastRemainingTime / (missingOccurrences + 1);
            long multiply = missingOccurrences - whichMissingOccurrenceToUse;

            long remainingTicks = occurrenceSize * multiply;

            return new SimulatedTime(remainingTicks, endDayTick, endGameTick, endMs);
        }

        return new SimulatedTime(remainingTicks.get(occurrence), endDayTick, endGameTick, endMs);
    }

    public SimulatedTime getFinalTime() {
        return getTimeAtOccurrence(occurrences);
    }
}
