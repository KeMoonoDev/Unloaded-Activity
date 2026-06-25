package dev.moono.unloadedactivity.api;

import dev.moono.unloadedactivity.MathUtils;
import net.minecraft.util.RandomSource;

public record OccurrencesAndDuration (int occurrences, long duration, float averageProbability) {
    public static OccurrencesAndDuration empty() {
        return new OccurrencesAndDuration(0, 0, 0F);
    }

    public static OccurrencesAndDuration recalculatedDuration(int occurrences, long cycles, float odds, RandomSource random) {
        return new OccurrencesAndDuration(occurrences, MathUtils.sampleNegativeBinomialWithMax(cycles, occurrences, odds, random), odds);
    }
}
