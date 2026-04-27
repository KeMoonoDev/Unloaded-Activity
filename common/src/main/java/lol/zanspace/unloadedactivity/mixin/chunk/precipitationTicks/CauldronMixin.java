package lol.zanspace.unloadedactivity.mixin.chunk.precipitationTicks;

import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

import static java.lang.Math.min;

@Mixin(CauldronBlock.class)
public abstract class CauldronMixin extends AbstractCauldronBlock {

    #if MC_VER >= MC_1_20_4
    public CauldronMixin(Properties properties, CauldronInteraction.InteractionMap interactionMap) {
        super(properties, interactionMap);
    }
    #else
    public CauldronMixin(Properties properties, Map<Item, CauldronInteraction> map) {
        super(properties, map);
    }
    #endif

    @Shadow @Final private static float RAIN_FILL_CHANCE;
    @Shadow @Final private static float POWDER_SNOW_FILL_CHANCE;

    @Override
    public boolean implementsSimulatePrecTicks() {
        return true;
    }

    @Unique
    private float getFillOdds(Biome.Precipitation precipitation) {
        if (precipitation == Biome.Precipitation.RAIN) {
            return RAIN_FILL_CHANCE;
        } else if (precipitation == Biome.Precipitation.SNOW) {
            return POWDER_SNOW_FILL_CHANCE;
        } else {
            return 0.0F;
        }
    }

    @Override
    public boolean canSimulatePrecTicks(BlockState state, ServerLevel level, BlockPos pos, long timeInWeather, Biome.Precipitation precipitation) {
        if (!UnloadedActivity.config.weatherFillCauldron) return false;
        if (timeInWeather == 0) return false;
        if (getFillOdds(precipitation) == 0.0F) return false;
        return true;
    }

    @Override
    public void simulatePrecTicks(BlockState state, ServerLevel level, BlockPos pos, long timeInWeather, long timePassed, Biome.Precipitation precipitation, float precipitationPickChance) {

        int maxCauldronLevel = LayeredCauldronBlock.MAX_FILL_LEVEL;

        float totalOdds = precipitationPickChance*getFillOdds(precipitation);

        int fill = Utils.getOccurrencesBinomial(timeInWeather, totalOdds, maxCauldronLevel,  level.random);

        if (fill == 0)
            return;

        if (precipitation == Biome.Precipitation.RAIN) {
            level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, fill));
        } else {
            level.setBlockAndUpdate(pos, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, fill));
        }

        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
    }
}
