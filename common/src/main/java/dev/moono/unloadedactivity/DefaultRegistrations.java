package dev.moono.unloadedactivity;

import dev.moono.unloadedactivity.api.NumberFetcherRegistry;
import dev.moono.unloadedactivity.api.SimulationMethodRegistry;
import dev.moono.unloadedactivity.api.UnloadedActivityApi;
import dev.moono.unloadedactivity.impl.number_fetchers.*;
import dev.moono.unloadedactivity.impl.simulation_methods.*;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
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
            UnloadedActivity.id("has_lava_neighbors_above"),
            new IsBlockNeighborsMatchValue(b -> b.getFluidState().is(FluidTags.LAVA), new Vec3i(0, 1, 0))
        );

        registry.register(
            UnloadedActivity.id("has_lava_neighbors"),
            new IsBlockNeighborsMatchValue(b -> b.getFluidState().is(FluidTags.LAVA))
        );

        registry.register(
            UnloadedActivity.id("has_lava_neighbors_below"),
            new IsBlockNeighborsMatchValue(b -> b.getFluidState().is(FluidTags.LAVA), new Vec3i(0, -1, 0))
        );

        registry.register(
            UnloadedActivity.id("has_solid_neighbors_above"),
            new IsBlockNeighborsMatchValue(b -> b #if MC_VER < MC_1_20_1 .getMaterial() #endif .isSolid(), new Vec3i(0, 1, 0))
        );

        registry.register(
            UnloadedActivity.id("has_solid_neighbors"),
            new IsBlockNeighborsMatchValue(b -> b #if MC_VER < MC_1_20_1 .getMaterial() #endif .isSolid())
        );

        registry.register(
            UnloadedActivity.id("has_solid_neighbors_below"),
            new IsBlockNeighborsMatchValue(b -> b #if MC_VER < MC_1_20_1 .getMaterial() #endif .isSolid(), new Vec3i(0, -1, 0))
        );

        registry.register(
            UnloadedActivity.id("is_sand_below"),
            new IsBlockMatchValue(b -> b.is(BlockTags.SAND), new Vec3i(0, -1, 0))
        );

        registry.register(
            UnloadedActivity.id("is_sand_above"),
            new IsBlockMatchValue(b -> b.is(BlockTags.SAND), new Vec3i(0, 1, 0))
        );

        registry.register(
            UnloadedActivity.id("is_snow_below"),
            new IsBlockMatchValue(b -> b.is(BlockTags.SNOW), new Vec3i(0, -1, 0))
        );

        registry.register(
            UnloadedActivity.id("is_snow_above"),
            new IsBlockMatchValue(b -> b.is(BlockTags.SNOW), new Vec3i(0, 1, 0))
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
                UnloadedActivity.id("water_evaporates"),
                new WaterEvaporatesValue()
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

        registry.registerDynamic(
                UnloadedActivity.MOD_ID,
                s -> s.startsWith("custom/"),
                s -> {
                    String valueKey = s.substring("custom/".length());
                    return new CustomValue(valueKey);
                }
        );

        registry.registerNumber(UnloadedActivity.id("update_clients"), Block.UPDATE_CLIENTS);
        registry.registerNumber(UnloadedActivity.id("update_invisible"), Block.UPDATE_INVISIBLE);
        registry.registerNumber(UnloadedActivity.id("update_all"), Block.UPDATE_ALL);
        registry.registerNumber(UnloadedActivity.id("update_none"), Block.UPDATE_NONE);

        GroupFetchValue.register(registry);
    }

    @Override
    public void registerSimulationMethods(SimulationMethodRegistry registry) {
        registry.register(UnloadedActivity.id("property"), PropertyMethod::new);
        registry.register(UnloadedActivity.id("max_property_growth"), MaxPropertyGrowthMethod::new);
        registry.register(UnloadedActivity.id("increment_property_growth"), IncrementPropertyGrowthMethod::new);

        registry.register(UnloadedActivity.id("decay"), DecayMethod::new);
        registry.register(UnloadedActivity.id("replace"), ReplaceMethod::new);
        registry.register(UnloadedActivity.id("hatch"), HatchMethod::new);

        registry.register(UnloadedActivity.id("budding"), BuddingMethod::new);

        registry.register(UnloadedActivity.id("grow_tree"), GrowTreeMethod::new);

        registry.register(UnloadedActivity.id("grow_speleothem"), SpeleothemMethod::new);

        registry.register(UnloadedActivity.id("grow_fruit"), GrowFruitMethod::new);

        registry.register(UnloadedActivity.id("grow_bamboo"), GrowBambooMethod::new);
    }
}
