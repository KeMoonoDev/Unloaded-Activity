package lol.zanspace.unloadedactivity.mixin.chunk.precipitationTicks;

import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.Utils;
import lol.zanspace.unloadedactivity.mixin.CauldronBlockInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.function.Predicate;


@Mixin(LayeredCauldronBlock.class)
public abstract class LayeredCauldronMixin extends AbstractCauldronBlock {
    #if MC_VER >= MC_1_20_4
    public LayeredCauldronMixin(Properties properties, CauldronInteraction.InteractionMap interactionMap) {
        super(properties, interactionMap);
    }

    @Shadow @Final private Biome.Precipitation precipitationType;
    #else
    public LayeredCauldronMixin(Properties properties, Map<net.minecraft.world.item.Item, CauldronInteraction> map) {
        super(properties, map);
    }

    @Shadow @Final private Predicate<Biome.Precipitation> fillPredicate;
    #endif

    @Override
    public boolean implementsSimulatePrecTicks() {
        return true;
    }

    @Unique
    private float getFillOdds(Biome.Precipitation precipitation) {
        if (precipitation == Biome.Precipitation.RAIN) {
            return CauldronBlockInvoker.getRainFillChance();
        } else if (precipitation == Biome.Precipitation.SNOW) {
            return CauldronBlockInvoker.getSnowFillChance();
        } else {
            return 0.0F;
        }
    }

    @Override
    public boolean canSimulatePrecTicks(BlockState state, ServerLevel level, BlockPos pos, long timeInWeather, Biome.Precipitation precipitation) {
        if (!UnloadedActivity.config.weatherFillCauldron) return false;
        if (timeInWeather == 0) return false;
        if (getFillOdds(precipitation) == 0.0F) return false;
        #if MC_VER >= MC_1_20_4
        if (precipitation != this.precipitationType) return false;
        #else
        if (!this.fillPredicate.test(precipitation)) return false;
        #endif
        if (state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL) return false;
        return true;
    }

    @Override
    public void simulatePrecTicks(BlockState state, ServerLevel level, BlockPos pos, long timeInWeather, long timePassed, Biome.Precipitation precipitation, float precipitationPickChance) {

        int maxCauldronLevel = LayeredCauldronBlock.MAX_FILL_LEVEL;
        int currentCauldronLevel = state.getValue(LayeredCauldronBlock.LEVEL);
        int levelDifference = maxCauldronLevel-currentCauldronLevel;

        float totalOdds = precipitationPickChance*getFillOdds(precipitation);

        int fill = Utils.getOccurrencesBinomial(timeInWeather, totalOdds, levelDifference, level.random);

        if (fill == 0)
            return;

        BlockState newState = state.setValue(LayeredCauldronBlock.LEVEL, currentCauldronLevel+fill);
        level.setBlockAndUpdate(pos, newState);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
    }
}
