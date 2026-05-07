package lol.zanspace.unloadedactivity.mixin.chunk.randomTicks;

import lol.zanspace.unloadedactivity.OccurrencesAndDuration;
import lol.zanspace.unloadedactivity.datapack.SimulationData;
import org.spongepowered.asm.mixin.Mixin;

#if MC_VER >= MC_1_21_10
import lol.zanspace.unloadedactivity.UnloadedActivity;
import net.minecraft.world.level.block.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Iterator;
import java.util.Optional;

@Mixin(WeatheringCopperBarsBlock.class)
public abstract class WeatheringCopperBarsMixin extends IronBarsBlock implements WeatheringCopper {

    protected WeatheringCopperBarsMixin(Properties properties) {
        super(properties);
    }

    /*

    @Override
    public double getOdds(ServerLevel level, BlockState state, BlockPos pos, SimulationData.SimulateProperty simulateProperty, String propertyName) {
        return 0.05688889f;
    }
    @Override
    public boolean hasRandTicks() {return true;}
    @Shadow
    public WeatherState getAge() {
        return null;
    }
    @Override public boolean canSimulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulationData.SimulateProperty simulateProperty, String propertyName) {
        if (!UnloadedActivity.config.ageCopper) return false;
        int currentAge = getCurrentAgeUA(state);
        if (currentAge == getMaxAgeUA()) return false;
        return true;
    }

    @Override public int getCurrentAgeUA(BlockState state) {
        return this.getAge().ordinal();
    }

    @Override public int getMaxAgeUA() {
        return 3;
    }

    @Override
    public Triple<BlockState, OccurrencesAndDuration, BlockPos> simulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulationData.SimulateProperty simulateProperty, String propertyName, RandomSource random, long timePassed, int randomTickSpeed, boolean calculateDuration) {

        double randomPickChance = Utils.getRandomPickOdds(randomTickSpeed);

        double tryDegradeOdds = getOdds(level, state, pos, simulateProperty, propertyName);

        BlockPos blockPos;
        float nearbyBlocks = 0;
        Iterator<BlockPos> iterator = BlockPos.withinManhattan(pos, 4, 4, 4).iterator();
        while (iterator.hasNext() && (blockPos = iterator.next()).distManhattan(pos) <= 4) {
            if (blockPos.equals(pos) || !((level.getBlockState(blockPos).getBlock()) instanceof ChangeOverTimeBlock))
                continue;
            nearbyBlocks++;
        }
        float degradeOdds = 1 / (nearbyBlocks + 1);
        float degradeOdds2 = degradeOdds * degradeOdds * 0.75f;

        double totalOdds = randomPickChance * tryDegradeOdds * degradeOdds2;
        int currentAge = getCurrentAgeUA(state);
        int ageDifference = getMaxAgeUA() - currentAge;

        int ageAmount = Utils.getOccurrences(timePassed, totalOdds, ageDifference, random);

        if (ageAmount == 0)
            return state;

        state = getDegradeResult(ageAmount, state, level, pos);
        level.setBlockAndUpdate(pos, state);

        return null;
    }

    @Unique
    private BlockState getDegradeResult(int steps, BlockState state, ServerLevel level, BlockPos pos) {

        steps--;

        Optional<BlockState> optionalState = this.getNext(state);

        if (optionalState.isEmpty())
            return state;

        if (steps != 0) {
            return getDegradeResult(steps, optionalState.get(), level, pos);
            //im too lazy to see how getDegradationResult actually degrades the thing
        }

        return optionalState.get();
    }

     */
}

#else
// Empty mixin to the air block whenever this block isn't in the current version.
import net.minecraft.world.level.block.AirBlock;
@Mixin(AirBlock.class)
public class WeatheringCopperBarsMixin {}
#endif