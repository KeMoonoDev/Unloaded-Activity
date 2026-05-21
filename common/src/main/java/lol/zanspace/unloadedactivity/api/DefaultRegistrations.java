package lol.zanspace.unloadedactivity.api;

import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.api.number_fetchers.*;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class DefaultRegistrations implements UnloadedActivityApi {
    @Override
    public void registerNumberFetchers(NumberFetcherRegistry registry) {
        registry.register(
            UnloadedActivity.id("growth_speed"),
            new GrowthSpeedValue()
        );

        registry.register(
            UnloadedActivity.id("available_space_for_gourd"),
            new AvailableSpaceForGourdValue()
        );

        registry.register(
            UnloadedActivity.id("grass_can_stay_alive"),
            new GrassCanStayAliveValue()
        );

        registry.register(
            UnloadedActivity.id("grass_can_grow"),
            new GrassCanGrowValue()
        );

        registry.register(
            UnloadedActivity.id("max_snow_height"),
            new MaxSnowHeightValue()
        );

        registry.register(
            UnloadedActivity.id("is_upper_door"),
            new IsDoorHalfValue(DoubleBlockHalf.UPPER)
        );

        registry.register(
            UnloadedActivity.id("is_lower_door"),
            new IsDoorHalfValue(DoubleBlockHalf.LOWER)
        );

        registry.register(
            UnloadedActivity.id("is_sand_below"),
            new IsBlockTagValue(BlockTags.SAND, new Vec3i(0, -1, 0))
        );

        registry.register(
            UnloadedActivity.id("is_sand_above"),
            new IsBlockTagValue(BlockTags.SAND, new Vec3i(0, 1, 0))
        );

        registry.register(
            UnloadedActivity.id("is_snow_below"),
            new IsBlockTagValue(BlockTags.SNOW, new Vec3i(0, -1, 0))
        );

        registry.register(
            UnloadedActivity.id("is_snow_above"),
            new IsBlockTagValue(BlockTags.SNOW, new Vec3i(0, 1, 0))
        );

        registry.register(
            UnloadedActivity.id("block_brightness"),
            new BlockBrightnessValue()
        );

        registry.register(
            UnloadedActivity.id("block_brightness_above"),
            new BlockBrightnessValue(new Vec3i(0, 1, 0))
        );

        registry.register(
            UnloadedActivity.id("raw_brightness"),
            new RawBrightnessValue()
        );

        registry.register(
            UnloadedActivity.id("raw_brightness_above"),
            new RawBrightnessValue(new Vec3i(0, 1, 0))
        );

        registry.register(
            UnloadedActivity.id("local_brightness"),
            new LocalBrightnessValue()
        );

        registry.register(
            UnloadedActivity.id("local_brightness_above"),
            new LocalBrightnessValue(new Vec3i(0, 1, 0))
        );

        registry.register(
            UnloadedActivity.id("is_snow_precipitation"),
            new IsPrecipitationValue(Biome.Precipitation.SNOW)
        );

        registry.register(
            UnloadedActivity.id("is_rain_precipitation"),
            new IsPrecipitationValue(Biome.Precipitation.RAIN)
        );

        registry.register(
            UnloadedActivity.id("should_freeze"),
            new ShouldFreezeValue()
        );

        registry.register(
            UnloadedActivity.id("should_snow"),
            new ShouldSnowValue()
        );

        registry.register(
            UnloadedActivity.id("random"),
            new RandomValue()
        );

        registry.registerDynamic(
            UnloadedActivity.MOD_ID,
            s -> s.startsWith("property/"),
            s -> {
                String propertyName = s.substring("property/".length());
                return new PropertyValue(propertyName);
            }
        );

        GroupFetchValue.register(registry);
    }
}
