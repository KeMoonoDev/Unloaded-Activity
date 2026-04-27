package lol.zanspace.unloadedactivity;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.RandomSource;

public record OccurrencesAndDuration (int occurrences, long duration, float averageProbability) {
    public static OccurrencesAndDuration empty() {
        return new OccurrencesAndDuration(0, 0, 0F);
    }

    public static OccurrencesAndDuration recalculatedDuration(int occurrences, long cycles, float odds, RandomSource random) {
        return new OccurrencesAndDuration(occurrences, Utils.sampleNegativeBinomialWithMax(cycles, occurrences, odds, random), odds);
    }
}
