package dev.moono.unloadedactivity;


import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.api.WorldWeatherForecast;
import dev.moono.unloadedactivity.api.context.UpdatingContext;
import dev.moono.unloadedactivity.api.value_expression.UpdatingValueExpression;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;
import static net.minecraft.util.Mth.sign;


public class MathUtils {
    public static float getRandomPickOdds(int randomTickSpeed) {
        return (float) (1F-pow(1F - 1F / 4096F, randomTickSpeed));
    }

    public static float getPrecipitationPickOdds(int randomTickSpeed) {
        #if MC_VER >= MC_1_20_2
        return (float) (1F-pow(1F - 1F / (48F * 256F), randomTickSpeed));
        #else
        return 1F/4096F;
        #endif
    }

    public static OccurrencesAndTimings getOccurrences(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, UpdatingValueExpression<Number> probability, boolean requiresRain, int maxOccurrences, float randomPickOdds, boolean calculateDuration, RandomSource random, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (calculateDuration) {
            return getOccurrencesWithAccurateDuration(level, state, pos, simulatedTime, probability, requiresRain, maxOccurrences, randomPickOdds, random, groupSimulateData);
        } else {
            return getOccurrencesWithFastDuration(level, state, pos, simulatedTime, probability, requiresRain, maxOccurrences, randomPickOdds, random, groupSimulateData);
        }
    }

    public static OccurrencesAndTimings getOccurrencesWithFastDuration(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, UpdatingValueExpression<Number> probability, boolean requiresRain, int maxOccurrences, float randomPickOdds, RandomSource random, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (maxOccurrences <= 0)
            return OccurrencesAndTimings.empty(simulatedTime);

        int successes = 0;

        long startingAvailableTime = simulatedTime.remainingTime();
        long remainingTime = startingAvailableTime;

        WorldWeatherForecast weatherData = level.getWeatherForecast();

        while (remainingTime > 0) {
            long currentTime = simulatedTime.currentTime();
            boolean isRaining = weatherData.getWeatherAtTime(currentTime);

            if (requiresRain && !isRaining) {
                long nextWeatherSwitchDuration = weatherData.getNextWeatherChangeDuration(currentTime);
                remainingTime -= nextWeatherSwitchDuration;
                continue;
            }

            UpdatingContext context = UpdatingContext.of(level, state, pos, currentTime, groupSimulateData);

            long nextOddsSwitchDuration = probability.getNextValueSwitchDuration(context);
            if (requiresRain || probability.canBeAffectedByWeather) {
                long nextWeatherSwitchDuration = weatherData.getNextWeatherChangeDuration(currentTime);
                nextOddsSwitchDuration = Math.min(nextOddsSwitchDuration, nextWeatherSwitchDuration);
            }

            long simulateForCycles = Math.min(nextOddsSwitchDuration, remainingTime);

            float odds = probability.evaluate(context).floatValue();
            float totalOdds = odds * randomPickOdds;

            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Simulating from " + currentTime + " to " + (currentTime + simulateForCycles) + " (difference: " + simulateForCycles + ") with odds " + odds);

            int successesThisRound = getOccurrencesBinomial(simulateForCycles, totalOdds, maxOccurrences - successes, random);

            successes += successesThisRound;

            if (successes >= maxOccurrences) {
                //long quickDuration = startingAvailableTime - remainingTime + simulateForCycles;
                return OccurrencesAndTimings.fastDuration(successes, simulatedTime);
            }

            remainingTime -= simulateForCycles;
        }

        return OccurrencesAndTimings.fastDuration(successes, simulatedTime);
    }

    public static OccurrencesAndTimings getOccurrencesWithAccurateDuration(ServerLevel level, BlockState state, BlockPos pos, SimulatedTime simulatedTime, UpdatingValueExpression<Number> probability, boolean requiresRain, int maxOccurrences, float randomPickOdds, RandomSource random, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (maxOccurrences <= 0)
            return OccurrencesAndTimings.empty(simulatedTime);

        long startingAvailableTime = simulatedTime.remainingTime();
        long remainingTime = startingAvailableTime;

        ArrayList<Long> remainingTimes = new ArrayList<>();
        remainingTimes.add(remainingTime);


        int successes = 0;

        WorldWeatherForecast weatherData = level.getWeatherForecast();

        while (remainingTime > 0) {
            long currentTime = simulatedTime.endTime() - remainingTime;
            boolean isRaining = weatherData.getWeatherAtTime(currentTime);

            if (requiresRain && !isRaining) {
                long nextWeatherSwitchDuration = weatherData.getNextWeatherChangeDuration(currentTime);
                remainingTime -= nextWeatherSwitchDuration;
                continue;
            }

            UpdatingContext context = UpdatingContext.of(level, state, pos, currentTime, groupSimulateData);

            long nextOddsSwitchDuration = probability.getNextValueSwitchDuration(context);
            if (requiresRain || probability.canBeAffectedByWeather) {
                long nextWeatherSwitchDuration = weatherData.getNextWeatherChangeDuration(currentTime);
                nextOddsSwitchDuration = Math.min(nextOddsSwitchDuration, nextWeatherSwitchDuration);
            }

            long simulatingTimeForOdds = Math.min(nextOddsSwitchDuration, remainingTime);
            long availableTimeForOdds = simulatingTimeForOdds;

            float odds = probability.evaluate(context).floatValue();
            float totalOdds = odds * randomPickOdds;

            if (UnloadedActivity.config.debugLogs)
                UnloadedActivity.LOGGER.info("Simulating from " + currentTime + " to " + (currentTime + availableTimeForOdds) + " (difference: " + availableTimeForOdds + ") with odds " + odds);

            long tempRemainingTime = remainingTime;

            while (availableTimeForOdds > 0 && successes < maxOccurrences) {
                long successDuration = sampleNegativeBinomial(1, totalOdds, random);
                availableTimeForOdds -= successDuration;
                tempRemainingTime -= successDuration;
                if (availableTimeForOdds >= 0) {
                    successes++;
                    remainingTimes.add(tempRemainingTime);
                }
            }

            if (successes >= maxOccurrences) break;

            remainingTime -= simulatingTimeForOdds;
        }

        return new OccurrencesAndTimings(successes, remainingTimes, simulatedTime.endTime());

    }

    //41ms, 200 chunks
    public static int getOccurrencesBinomial(long cycles, double odds, int maxOccurrences, RandomSource random) {

        if (odds <= 0)
            return 0;

        if (maxOccurrences <= 0)
            return 0;

        double invertedOdds = 1-odds;

        if (maxOccurrences == 1) {
            return random.nextDouble() < 1 - pow(invertedOdds, cycles) ? 1 : 0;
        }

        double choose = 1;

        double totalProbability = 0;

        double randomDouble = random.nextDouble();

        for (int i = 0; i<maxOccurrences;i++) {

            if (i == cycles) return i;

            if (i != 0) {
                choose *= (cycles - (i - 1.0))/i;
            }

            double finalProbability = choose * pow(odds, i) * pow(invertedOdds, cycles-i); //Probability of it happening "i" times

            totalProbability += finalProbability;

            if (randomDouble < totalProbability) {
                return i;
            }
        }
        return maxOccurrences;
    }

    public static long sampleNegativeBinomial(int successes, double odds, RandomSource random) {
        return samplePoisson(sampleGamma(successes, (1.0-odds)/odds, random), random);
    }

    public static long sampleNegativeBinomialWithMax(long cycles, int successes, double odds, RandomSource random) {
        long failedTrials = Long.MAX_VALUE;
        long attempts = 0;
        while (failedTrials > cycles && attempts < UnloadedActivity.config.maxNegativeBinomialAttempts) {

            failedTrials = sampleNegativeBinomial(successes, odds, random);
            attempts++;
        }

        if (failedTrials > cycles) {
            //we have attempted this many times and the probability of this happening is probably very low.
            //So we'll just pretend this was the output even though it's not accurate at all
            failedTrials = (long) (random.nextDouble() * cycles);
        }
        return failedTrials;
    }

    public static final double[] logFactorials = new double[]{
            0.0,
            0.0,
            0.6931471805599453,
            1.791759469228055,
            3.1780538303479458,
            4.787491742782046,
            6.579251212010101,
            8.525161361065415,
            10.60460290274525,
            12.801827480081469
    };

    // lambda >= 10 uses algorithm PTRD found here: https://research.wu.ac.at/ws/portalfiles/portal/18953249/document.pdf
    // lambda < 10 uses the algorithm found here: https://math.stackexchange.com/questions/785188/simple-algorithm-for-generating-poisson-distribution
    public static long samplePoisson(double lambda, RandomSource random) {

        if (lambda < 10) {
            long k = 0;
            double p = random.nextDouble();
            while (p > exp(-lambda)) {
                p *= random.nextDouble();
                k += 1;
            }
            return k;
        }

        double smu = sqrt(lambda);
        double b = 0.931 + 2.53*smu;
        double a = 0.059 + 0.02483*b;
        double vr = 0.9277 - 3.6224/(b-2.0);
        double oneDivAlpha = 1.1239 + 1.1328/(b-3.4);

        double logSqrtPi2 = 0.9189385332046727; //result from log(sqrt(PI*2))

        while (true) {

            double v = random.nextDouble();
            double u;

            if (v <= 0.86*vr) {
                u = v/vr-0.43;
                return (long)floor((2.0*a/(0.5-abs(u))+b)*u+lambda+0.445);
            }

            if (v >= vr) {
                u = random.nextDouble()-0.5;
            } else {
                u = v/vr-0.93;
                u = sign(u)*0.5 - u;
                v = random.nextDouble()*vr;
            }

            double us = 0.5-abs(u);

            if (us < 0.013 && v > us)
                continue;

            int k = (int)((2.0*a/us+b)*u+lambda+0.445);
            v = v*oneDivAlpha/(a/(us*us)+b);

            if (k >= 10 && log(v*smu) <= (k + 0.5) * log(lambda/k)-lambda-logSqrtPi2+k-(1.0/12.0 - 1.0/(360.0*k*k))/k)
                return k;

            if (0 <= k && k <= 9 && log(v) <= k*log(lambda) - lambda - logFactorials[k])
                return k;

        }
    }


    // Algorithm from https://dl.acm.org/doi/pdf/10.1145/358407.358414
    // Since we only accept integers anyway we don't need to have a separate algorithm for 0 < shape < 1
    public static double sampleGamma(int shape, double scale, RandomSource random) {

        if (shape <= 0)
            return 0;

        double d = shape-1.0/3.0;
        double c = 1.0/sqrt(d*9.0);

        while (true) {

            double x = random.nextGaussian();
            double v = pow(1.0+c*x, 3.0);

            if (v > 0) {

                double u = random.nextDouble();

                if (u < 1.0-0.0331*x*x*x*x)
                    return d*v*scale;

                if (log(u) < 0.5*x*x+d*(1.0-v+log(v)))
                    return d*v*scale;
            }
        }
    }
}

