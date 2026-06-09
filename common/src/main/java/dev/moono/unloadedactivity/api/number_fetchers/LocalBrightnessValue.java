package dev.moono.unloadedactivity.api.number_fetchers;

import dev.moono.unloadedactivity.api.NumberFetcher;
import dev.moono.unloadedactivity.datapack.ValueContext;
import dev.moono.unloadedactivity.datapack.Comparison;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;

public class LocalBrightnessValue implements NumberFetcher {

    Vec3i offset;

    public LocalBrightnessValue() {
        this.offset = Vec3i.ZERO;
    }

    public LocalBrightnessValue(Vec3i offset) {
        this.offset = offset;
    }

    // https://www.desmos.com/calculator/bl10cndxzq
    public final static long[] NORMAL_SKY_DARKNESS_START = {
            12041L, // 1
            12210L, // 2
            12377L, // 3
            12541L, // 4
            12704L, // 5
            12866L, // 6
            13027L, // 7
            13188L, // 8
            13348L, // 9
            13509L, // 10
            13670L, // 11
    };

    public final static long[] NORMAL_SKY_DARKNESS_END = {
            23959L, // 1
            23790L, // 2
            23623L, // 3
            23459L, // 4
            23295L, // 5
            23134L, // 6
            22973L, // 7
            22812L, // 8
            22652L, // 9
            22491L, // 10
            22330L, // 11
    };

    public final static long[] RAIN_SKY_DARKNESS_START = {
            12009L, // 4
            12256L, // 5
            12497L, // 6
            12734L, // 7
            12969L, // 8
            13202L, // 9
            13436L, // 10
            13670L, // 11
    };

    public final static long[] RAIN_SKY_DARKNESS_END = {
            23991L, // 4
            23744L, // 5
            23503L, // 6
            23266L, // 7
            23031L, // 8
            22798L, // 9
            22564L, // 10
            22330L, // 11
    };

    public final static long[] RAIN_THUNDER_SKY_DARKNESS_START = {
            11941L, // 6
            12300L, // 7
            12648L, // 8
            12990L, // 9
            13330L, // 10
            13670L, // 11
    };

    public final static long[] RAIN_THUNDER_SKY_DARKNESS_END = {
            56L, // 6
            23700L, // 7
            23352L, // 8
            23010L, // 9
            22670L, // 10
            22330L, // 11
    };

    static {
        assert NORMAL_SKY_DARKNESS_START.length == NORMAL_SKY_DARKNESS_END.length;
        assert RAIN_SKY_DARKNESS_START.length == RAIN_SKY_DARKNESS_END.length;
        assert RAIN_THUNDER_SKY_DARKNESS_START.length == RAIN_THUNDER_SKY_DARKNESS_END.length;
        new LocalBrightnessValue(Vec3i.ZERO).test();
    }

    public void test() {
        assert getNextValueSwitchDurationFromLights(15, 0, 13000, false, false) == Long.MAX_VALUE;
        assert getNextValueSwitchDurationFromLights(5, 15, 13600, false, false) == NORMAL_SKY_DARKNESS_END[9] - 13600;
        assert getNextValueSwitchDurationFromLights(4, 15, 13600, false, false) == NORMAL_SKY_DARKNESS_START[10] - 13600;
    }

    public final static int MAX_DARKNESS = 11;

    @Override
    public Number evaluate(ValueContext context) {
        int blockLight = context.level.getBrightness(LightLayer.BLOCK, context.pos.offset(offset));
        int skyLight = context.level.getBrightness(LightLayer.SKY, context.pos.offset(offset));

        int currentDarken = getCurrentSkyDarken(context.currentTime, context.isRaining, context.isThundering);

        return Math.max(blockLight, skyLight - currentDarken);
    }

    @Override
    public boolean canBeAffectedByWeather() {
        return true;
    }

    @Override
    public boolean canBeAffectedByTime() {
        return true;
    }

    @Override
    public boolean isRandom() {
        return false;
    }

    @Override
    public long getNextValueSwitchDuration(ValueContext context) {
        int blockLight = context.level.getBrightness(LightLayer.BLOCK, context.pos.offset(offset));
        int skyLight = context.level.getBrightness(LightLayer.SKY, context.pos.offset(offset));

        return getNextValueSwitchDurationFromLights(blockLight, skyLight, context.currentTime, context.isRaining, context.isThundering);
    }

    public long getNextValueSwitchDurationFromLights(int blockLight, int skyLight, long currentTime, boolean isRaining, boolean isThundering) {
        int maxDarkenBeforeUnaffected = Math.min(MAX_DARKNESS, Math.max(skyLight - blockLight, 0));

        int currentDarken = getCurrentSkyDarken(currentTime, isRaining, isThundering);

        int darkenStartCheck = Math.min(currentDarken + 1, maxDarkenBeforeUnaffected);
        int darkenStopCheck = Math.min(currentDarken, maxDarkenBeforeUnaffected);

        long nextDarkenStart = getNextSkyDarkenStartDuration(darkenStartCheck, currentTime, isRaining, isThundering);
        long currentDarkenEnd = getNextSkyDarkenStopDuration(darkenStopCheck, currentTime, isRaining, isThundering);

        long finalDuration =  Math.min(nextDarkenStart, currentDarkenEnd);

        if (finalDuration <= 0) {
            throw new RuntimeException("Duration is 0 or negative.");
        }

        return finalDuration;
    }

    @Override
    public long getNextConditionSwitchDuration(ValueContext context, float target, Comparison comparison) {
        ServerLevel level = context.level;
        BlockPos pos = context.pos;
        long currentTime = context.currentTime;
        boolean isRaining = context.isRaining;
        boolean isThundering = context.isThundering;

        int blockLight = level.getBrightness(LightLayer.BLOCK, pos.offset(offset));
        int skyLight = level.getBrightness(LightLayer.SKY, pos.offset(offset));

        int targetBrightness = (int)target;

        switch (comparison) {
            case NE, EQ -> {
                if (blockLight > targetBrightness)
                    return Long.MAX_VALUE;

                int neededDarken = skyLight - targetBrightness;

                if (neededDarken > MAX_DARKNESS) {
                    return Long.MAX_VALUE;
                }

                int currentDarken = getCurrentSkyDarken(currentTime, isRaining, isThundering);

                if (currentDarken > neededDarken) {
                    return getNextSkyDarkenStopDuration(neededDarken+1, currentTime, isRaining, isThundering);
                }

                if (currentDarken < neededDarken) {
                    return getNextSkyDarkenStartDuration(neededDarken, currentTime, isRaining, isThundering);
                }

                return getNextSkyDarkenStartDuration(neededDarken+1, currentTime, isRaining, isThundering);
            }
            case LT -> {
                // Now we do the checks for Less or Equal with newTargetBrightness.
                int newTargetBrightness = targetBrightness - 1;

                if (blockLight > newTargetBrightness)
                    return Long.MAX_VALUE;

                int neededDarken = skyLight - newTargetBrightness;

                if (neededDarken > MAX_DARKNESS) {
                    return Long.MAX_VALUE;
                }

                int currentDarken = getCurrentSkyDarken(currentTime, isRaining, isThundering);

                if (currentDarken < neededDarken) {
                    return getNextSkyDarkenStartDuration(neededDarken, currentTime, isRaining, isThundering);
                }

                return getNextSkyDarkenStopDuration(neededDarken, currentTime, isRaining, isThundering);
            }
            case LE -> {
                if (blockLight > targetBrightness)
                    return Long.MAX_VALUE;

                int neededDarken = skyLight - targetBrightness;

                if (neededDarken > MAX_DARKNESS) {
                    return Long.MAX_VALUE;
                }

                int currentDarken = getCurrentSkyDarken(currentTime, isRaining, isThundering);

                if (currentDarken < neededDarken) {
                    return getNextSkyDarkenStartDuration(neededDarken, currentTime, isRaining, isThundering);
                }

                return getNextSkyDarkenStopDuration(neededDarken, currentTime, isRaining, isThundering);
            }
            case GT -> {
                // Now we do the checks for Greater or Equal with newTargetBrightness.
                int newTargetBrightness = targetBrightness + 1;

                if (blockLight >= newTargetBrightness)
                    return Long.MAX_VALUE;

                int maxAllowedDarken = skyLight - newTargetBrightness;

                if (maxAllowedDarken >= MAX_DARKNESS) {
                    return Long.MAX_VALUE;
                }

                int currentDarken = getCurrentSkyDarken(currentTime, isRaining, isThundering);

                if (currentDarken > maxAllowedDarken) {
                    return getNextSkyDarkenStopDuration(maxAllowedDarken+1, currentTime, isRaining, isThundering);
                }

                return getNextSkyDarkenStartDuration(maxAllowedDarken+1, currentTime, isRaining, isThundering);

            }
            case GE -> {
                if (blockLight >= targetBrightness)
                    return Long.MAX_VALUE;

                int maxAllowedDarken = skyLight - targetBrightness;

                if (maxAllowedDarken >= MAX_DARKNESS) {
                    return Long.MAX_VALUE;
                }

                int currentDarken = getCurrentSkyDarken(currentTime, isRaining, isThundering);

                if (currentDarken > maxAllowedDarken) {
                    return getNextSkyDarkenStopDuration(maxAllowedDarken+1, currentTime, isRaining, isThundering);
                }

                return getNextSkyDarkenStartDuration(maxAllowedDarken+1, currentTime, isRaining, isThundering);
            }
        }

        return Long.MAX_VALUE;
    }

    public long[] getStartArray(boolean isRaining, boolean isThundering) {
        if (isRaining && !isThundering) {
            return RAIN_SKY_DARKNESS_START;
        } else if (isThundering) {
            return RAIN_THUNDER_SKY_DARKNESS_START;
        } else {
            return NORMAL_SKY_DARKNESS_START;
        }
    }

    public long[] getEndArray(boolean isRaining, boolean isThundering) {
        if (isRaining && !isThundering) {
            return RAIN_SKY_DARKNESS_END;
        } else if (isThundering) {
            return RAIN_THUNDER_SKY_DARKNESS_END;
        } else {
            return NORMAL_SKY_DARKNESS_END;
        }
    }

    public int getCurrentSkyDarken(long currentTime, boolean isRaining, boolean isThundering) {
        long[] startTimes = getStartArray(isRaining, isThundering);
        long[] endTimes = getEndArray(isRaining, isThundering);

        int darkenOffset = MAX_DARKNESS - startTimes.length;

        assert startTimes.length == endTimes.length;

        long modTime = Math.floorMod(currentTime, 24000);

        for (int i=startTimes.length-1; i>=0; i--) {
            long darkStart = startTimes[i];
            long darkEnd = endTimes[i];

            if (modTime >= darkStart && modTime < darkEnd) {
                return i + darkenOffset + 1;
            }

            if (darkStart > darkEnd) {
                if (modTime >= darkStart || modTime < darkEnd) {
                    return i + darkenOffset + 1;
                }
            }
        }
        return darkenOffset;
    }

    public long getNextSkyDarkenStartDuration(int darken, long currentTime, boolean isRaining, boolean isThundering) {
        long[] startTimes = getStartArray(isRaining, isThundering);

        int darkenOffset = MAX_DARKNESS - startTimes.length;

        int index = darken - darkenOffset - 1;

        if (index < 0) {
            return Long.MAX_VALUE;
        }

        long startTime = startTimes[index];

        long daysPassed = Math.floorDiv(currentTime, 24000);

        startTime += daysPassed * 24000;

        if (currentTime >= startTime) {
            startTime += 24000;
        }

        return startTime - currentTime;
    }

    public long getNextSkyDarkenStopDuration(int darken, long currentTime, boolean isRaining, boolean isThundering) {
        long[] endTimes = getEndArray(isRaining, isThundering);

        int darkenOffset = MAX_DARKNESS - endTimes.length;

        int index = darken - darkenOffset - 1;

        if (index < 0) {
            return Long.MAX_VALUE;
        }

        long endTime = endTimes[index];

        long daysPassed = Math.floorDiv(currentTime, 24000);

        endTime += daysPassed * 24000;

        if (currentTime >= endTime) {
            endTime += 24000;
        }

        return endTime - currentTime;
    }
}
