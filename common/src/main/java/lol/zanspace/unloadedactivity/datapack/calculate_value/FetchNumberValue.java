package lol.zanspace.unloadedactivity.datapack.calculate_value;

#if MC_VER >= MC_1_21_11
import net.minecraft.world.level.gamerules.GameRules;
#else
import net.minecraft.world.level.GameRules;
#endif

import lol.zanspace.unloadedactivity.GameUtils;
import lol.zanspace.unloadedactivity.MathUtils;
import lol.zanspace.unloadedactivity.platform.IPlatformHelper;
import lol.zanspace.unloadedactivity.datapack.CalculateValue;
import lol.zanspace.unloadedactivity.datapack.CalculationData;
import lol.zanspace.unloadedactivity.mixin.CropBlockInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;

import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.SnowLayerBlock;

import java.util.Optional;
import java.util.function.Function;

public enum FetchNumberValue implements CalculateValue<Number> {
    GROWTH_SPEED {
        @Override
        public Number calculateValue(CalculationData data) {
            #if MC_VER >= MC_1_21_1
            return IPlatformHelper.INSTANCE.getGrowthSpeed(data.state, data.level, data.pos);
            #else
            return CropBlockInvoker.invokeGetGrowthSpeed(data.state.getBlock(), data.level, data.pos);
            #endif
        }
    },

    AVAILABLE_SPACE_FOR_GOURD {
        @Override
        public Number calculateValue(CalculationData data) {
            return (GameUtils.isValidGourdPosition(Direction.NORTH, data.pos, data.state, data.level) ? 1 : 0)
                + (GameUtils.isValidGourdPosition(Direction.EAST, data.pos, data.state, data.level) ? 1 : 0)
                + (GameUtils.isValidGourdPosition(Direction.SOUTH, data.pos, data.state, data.level) ? 1 : 0)
                + (GameUtils.isValidGourdPosition(Direction.WEST, data.pos, data.state, data.level) ? 1 : 0);

        }
    },

    RAW_BRIGHTNESS {
        @Override
        public Number calculateValue(CalculationData data) {
            return data.level.getRawBrightness(data.pos, 0);
        }
    },

    RAW_BRIGHTNESS_ABOVE {
        @Override
        public Number calculateValue(CalculationData data) {
            return data.level.getRawBrightness(data.pos.above(), 0);
        }
    },

    BLOCK_BRIGHTNESS {
        @Override
        public Number calculateValue(CalculationData data) {
            return data.level.getBrightness(LightLayer.BLOCK, data.pos);
        }
    },

    BLOCK_BRIGHTNESS_ABOVE {
        @Override
        public Number calculateValue(CalculationData data) {
            return data.level.getBrightness(LightLayer.BLOCK, data.pos.above());
        }
    },

    IS_SAND_BELOW {
        @Override
        public Number calculateValue(CalculationData data) {
            return data.level.getBlockState(data.pos.below()).is(BlockTags.SAND) ? 1 : 0;
        }
    },

    SHOULD_SNOW {
        @Override
        public Number calculateValue(CalculationData data) {
            // This is already checked by shouldSnow, but according to the spark profiler (1.19.2),
            // getBiome and shouldSnow takes a bit of time to run. Check the cheap stuff first.
            if (data.level.getBrightness(LightLayer.BLOCK, data.pos) >= 10) {
                return 0;
            }
            BlockPos samplePos = data.state.isAir() ? data.pos : data.pos.above();
            Biome biome = data.level.getBiome(samplePos).value();
            return biome.shouldSnow(data.level, data.pos) ? 1 : 0;
        }
    },

    SHOULD_FREEZE {
        @Override
        public Number calculateValue(CalculationData data) {
            // This is already checked by shouldFreeze, but according to the spark profiler (1.19.2),
            // getBiome and shouldFreeze takes a bit of time to run. Check the cheap stuff first.
            if (data.level.getBrightness(LightLayer.BLOCK, data.pos) >= 10) {
                return 0;
            }
            BlockPos samplePos = data.state.isAir() ? data.pos : data.pos.above();
            Biome biome = data.level.getBiome(samplePos).value();
            return biome.shouldFreeze(data.level, data.pos, false) ? 1 : 0;
        }
    },

    MAX_SNOW_HEIGHT {
        @Override
        public Number calculateValue(CalculationData data) {
            #if MC_VER >= MC_1_21_11
            int maxSnowHeight = data.level.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
            #elif MC_VER >= MC_1_19_4
            int maxSnowHeight = data.level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
            #else
            int maxSnowHeight = 1;
            #endif

            return Math.min(maxSnowHeight, SnowLayerBlock.MAX_HEIGHT);
        }
    },

    IS_SNOW_PRECIPITATION {
        @Override
        public Number calculateValue(CalculationData data) {
            BlockPos samplePos = data.state.isAir() ? data.pos : data.pos.above();
            Biome biome = data.level.getBiome(samplePos).value();
            #if MC_VER >= MC_1_21_3
            Biome.Precipitation precipitation = biome.getPrecipitationAt(samplePos, data.level.getSeaLevel());
            #elif MC_VER >= MC_1_19_4
            Biome.Precipitation precipitation = biome.getPrecipitationAt(samplePos);
            #else
            Biome.Precipitation precipitation = biome.getPrecipitation()
            #endif;
            boolean isSnow = precipitation == Biome.Precipitation.SNOW;
            return isSnow ? 1 : 0;
        }
    },

    IS_RAIN_PRECIPITATION {
        @Override
        public Number calculateValue(CalculationData data) {
            BlockPos samplePos = data.state.isAir() ? data.pos : data.pos.above();
            Biome biome = data.level.getBiome(samplePos).value();
            #if MC_VER >= MC_1_21_3
            Biome.Precipitation precipitation = biome.getPrecipitationAt(samplePos, data.level.getSeaLevel());
            #elif MC_VER >= MC_1_19_4
            Biome.Precipitation precipitation = biome.getPrecipitationAt(samplePos);
            #else
            Biome.Precipitation precipitation = biome.getPrecipitation()
            #endif;
            boolean isRain = precipitation == Biome.Precipitation.RAIN;
            return isRain ? 1 : 0;
        }
    },

    GROUP_SUM {
        @Override
        public Number calculateValue(CalculationData data) {
            float sum = 0;

            if (data.activeGroupSimulateData == null)
                return sum;

            sum += data.activeGroupSimulateData.groupMemberInfo.value;

            for (var surrounding : data.activeGroupSimulateData.surroundingData) {
                sum += surrounding.groupMemberInfo.value;
            }

            return sum;
        }
    },

    SUPER {
        @Override
        public boolean isSuper() {
            return true;
        }

        @Override
        public Number calculateValue(CalculationData data) {
            return 1;
        }
    };

    @Override
    public boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    public boolean canBeAffectedByTime() {
        return false;
    }

    @Override
    public long getNextValueSwitchDuration(CalculationData data) {
        return Long.MAX_VALUE;
    }

    @Override
    public CalculateValue<Number> replicate() {
        return this;
    }

    @Override
    public void replaceSuper(CalculateValue<Number> superValue) {}

    @Override
    public <U> CalculateValue<U> map(Function<Number, U> mapFunction) {
        throw new RuntimeException("Map function not supported on this type.");
    }

    public static Optional<FetchNumberValue> fromString(String variableName) {
        switch (variableName.toLowerCase()) {
            case "growth_speed" -> {
                return Optional.of(GROWTH_SPEED);
            }
            case "available_space_for_gourd" -> {
                return Optional.of(AVAILABLE_SPACE_FOR_GOURD);
            }
            case "raw_brightness" -> {
                return Optional.of(RAW_BRIGHTNESS);
            }
            case "raw_brightness_above" -> {
                return Optional.of(RAW_BRIGHTNESS_ABOVE);
            }
            case "block_brightness" -> {
                return Optional.of(BLOCK_BRIGHTNESS);
            }
            case "block_brightness_above" -> {
                return Optional.of(BLOCK_BRIGHTNESS_ABOVE);
            }
            case "is_sand_below" -> {
                return Optional.of(IS_SAND_BELOW);
            }
            case "should_snow" -> {
                return Optional.of(SHOULD_SNOW);
            }
            case "should_freeze" -> {
                return Optional.of(SHOULD_FREEZE);
            }
            case "max_snow_height" -> {
                return Optional.of(MAX_SNOW_HEIGHT);
            }
            case "is_snow_precipitation" -> {
                return Optional.of(IS_SNOW_PRECIPITATION);
            }
            case "is_rain_precipitation" -> {
                return Optional.of(IS_RAIN_PRECIPITATION);
            }
            case "group_sum" -> {
                return Optional.of(GROUP_SUM);
            }
            case "super" -> {
                return Optional.of(SUPER);
            }
        }

        return Optional.empty();
    };
}