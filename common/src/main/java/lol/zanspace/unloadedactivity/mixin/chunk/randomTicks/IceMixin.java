package lol.zanspace.unloadedactivity.mixin.chunk.randomTicks;


import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.IceBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IceBlock.class)
public abstract class IceMixin extends HalfTransparentBlock {

    protected IceMixin(Properties properties) {
        super(properties);
    }

    /*

    @Override
    public double getOdds(ServerLevel level, BlockState state, BlockPos pos, SimulationData.SimulateProperty simulateProperty, String propertyName) {
        return 1;
    }

    @Shadow protected void melt(BlockState state, Level level, BlockPos pos) {}

    @Override
    public boolean hasRandTicks() {return true;}

    @Override public boolean canSimulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulationData.SimulateProperty simulateProperty, String propertyName) {
        if (!UnloadedActivity.config.meltIce) return false;
        #if MC_VER >= MC_1_21_3
        int opacity = state.getLightBlock();
        #else
        int opacity = state.getLightBlock(level, pos);
        #endif
        if (level.getBrightness(LightLayer.BLOCK, pos) <= 11 - opacity) return false;
        return true;
    }

    @Override
    public Triple<BlockState, OccurrencesAndDuration, BlockPos> simulateProperty(BlockState state, ServerLevel level, BlockPos pos, SimulationData.SimulateProperty simulateProperty, String propertyName, RandomSource random, long timePassed, int randomTickSpeed, boolean calculateDuration) {

        double pickOdds = Utils.getRandomPickOdds(randomTickSpeed) * this.getOdds(level, state, pos, simulateProperty, propertyName);

        if (Utils.getOccurrences(timePassed, pickOdds, 1, random) != 0) {
            this.melt(state, level, pos);
            return null;
        }

        return state;
    }

     */
}
