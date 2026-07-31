package dev.moono.unloadedactivity;

import com.google.gson.JsonElement;
import dev.moono.unloadedactivity.api.NumberFetcherRegistry;
import dev.moono.unloadedactivity.api.SimulationMethodRegistry;
import dev.moono.unloadedactivity.api.UnloadedActivityApi;
import dev.moono.unloadedactivity.impl.number_fetchers.*;
import dev.moono.unloadedactivity.impl.simulation_methods.*;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Arrays;
import java.util.Optional;

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

        registry.register(
            UnloadedActivity.id("is_precipitation"),
            data -> {
                JsonElement unparsedPrecipitationName = data.get("precipitation");
                if (unparsedPrecipitationName == null)
                    throw new RuntimeException("Required field \"precipitation\" is missing.");

                String precipitationName = unparsedPrecipitationName.getAsString();
                for (Biome.Precipitation precipitation : Biome.Precipitation.values()) {
                    if (precipitation.getSerializedName().equals(precipitationName)) {
                        return new IsPrecipitationValue(precipitation);
                    }
                }
                StringBuilder errorMessage = new StringBuilder("Failed to find precipitation with name \"" + precipitationName + "\". Currently available precipitations are: ");
                for (Biome.Precipitation precipitation : Biome.Precipitation.values()) {
                    if (precipitation == Biome.Precipitation.NONE) continue;
                    errorMessage.append("\"").append(precipitation.getSerializedName()).append("\", ");
                }
                errorMessage.append("and \"").append(Biome.Precipitation.NONE.getSerializedName()).append("\".");
                throw new RuntimeException(errorMessage.toString());
            }
        );


        registry.register(
            UnloadedActivity.id("has_lava_neighbors"),
            data -> {
                JsonElement offsetUnparsed = data.get("offset");
                if (offsetUnparsed == null) return new IsBlockNeighborsMatchValue(b -> b.getFluidState().is(FluidTags.LAVA));
                return new IsBlockNeighborsMatchValue(b -> b.getFluidState().is(FluidTags.LAVA), GameUtils.parseOffset(offsetUnparsed));
            }
        );


        registry.register(
            UnloadedActivity.id("has_solid_neighbors"),
            data -> {
                JsonElement offsetUnparsed = data.get("offset");
                if (offsetUnparsed == null) return new IsBlockNeighborsMatchValue(b -> b #if MC_VER < MC_1_20_1 .getMaterial() #endif .isSolid());
                return new IsBlockNeighborsMatchValue(b -> b #if MC_VER < MC_1_20_1 .getMaterial() #endif .isSolid(), GameUtils.parseOffset(offsetUnparsed));
            }
        );

        registry.register(
            UnloadedActivity.id("has_tag"),
            data -> {
                JsonElement tagNameUnparsed = data.get("tag");
                if (tagNameUnparsed == null)
                    throw new RuntimeException("Required field \"tag\" is missing.");

                var tagId = GameUtils.parseId(tagNameUnparsed.getAsString());

                JsonElement offsetUnparsed = data.get("offset");
                if (offsetUnparsed == null) return new IsBlockMatchValue(b -> GameUtils.getBlockTags(b).anyMatch(tagKey -> tagKey.location().equals(tagId)));
                return new IsBlockMatchValue(b -> GameUtils.getBlockTags(b).anyMatch(tagKey -> tagKey.location().equals(tagId)), GameUtils.parseOffset(offsetUnparsed));
            }
        );

        registry.register(
            UnloadedActivity.id("is_block"),
            data -> {
                JsonElement blockNameUnparsed = data.get("block");
                if (blockNameUnparsed == null)
                    throw new RuntimeException("Required field \"block\" is missing.");

                var blockId = GameUtils.parseId(blockNameUnparsed.getAsString());
                Block block = GameUtils.getBlock(blockId);

                if (block == null) throw new RuntimeException("\""+blockId+"\" is not a valid block.");

                JsonElement offsetUnparsed = data.get("offset");
                if (offsetUnparsed == null) return new IsBlockMatchValue(b -> b.getBlock() == block);
                return new IsBlockMatchValue(b -> b.getBlock() == block, GameUtils.parseOffset(offsetUnparsed));
            }
        );

        registry.register(
            UnloadedActivity.id("block_brightness"),
            data -> {
                JsonElement offsetUnparsed = data.get("offset");
                if (offsetUnparsed == null) return new BlockBrightnessValue();
                return new BlockBrightnessValue(GameUtils.parseOffset(offsetUnparsed));
            }
        );

        registry.register(
            UnloadedActivity.id("raw_brightness"),
            data -> {
                JsonElement offsetUnparsed = data.get("offset");
                if (offsetUnparsed == null) return new RawBrightnessValue();
                return new RawBrightnessValue(GameUtils.parseOffset(offsetUnparsed));
            }
        );

        registry.register(
            UnloadedActivity.id("local_brightness"),
            data -> {
                JsonElement offsetUnparsed = data.get("offset");
                if (offsetUnparsed == null) return new LocalBrightnessValue();
                return new LocalBrightnessValue(GameUtils.parseOffset(offsetUnparsed));
            }
        );

        registry.register(
            UnloadedActivity.id("property"),
            data -> {
                JsonElement unparsedPropertyName = data.get("property_name");
                if (unparsedPropertyName == null)
                    throw new RuntimeException("Required field \"property_name\" is missing.");
                return new PropertyValue(unparsedPropertyName.getAsString());
            }
        );

        registry.register(
            UnloadedActivity.id("provided"),
            data -> new CustomValue(data.get("value").getAsString())
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
