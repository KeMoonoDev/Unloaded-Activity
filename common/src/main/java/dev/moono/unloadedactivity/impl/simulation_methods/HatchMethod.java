package dev.moono.unloadedactivity.impl.simulation_methods;

#if MC_VER >= MC_1_21_3
import net.minecraft.world.entity.EntitySpawnReason;
#endif

#if MC_VER >= MC_1_21_11
import net.minecraft.world.entity.animal.turtle.Turtle;
#else
import net.minecraft.world.entity.animal.Turtle;
#endif

import dev.moono.unloadedactivity.api.ActiveGroupSimulateData;
import dev.moono.unloadedactivity.api.OccurrencesAndTimings;
import dev.moono.unloadedactivity.api.SimulatedTime;
import dev.moono.unloadedactivity.DeferredBlockPlacer;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.simulation_method.GroupableSimulationMethod;
import dev.moono.unloadedactivity.api.value_expression.RandomizedValueExpression;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HatchMethod extends GroupableSimulationMethod {
    public final boolean dropsResources;
    public final EntityType<?> hatchEntity;
    public final RandomizedValueExpression<Number> hatchCount;
    @Nullable
    public final Integer startingAge;

    public HatchMethod(SimulationConfig config, Block block, boolean hasDependants) {
        super(config, hasDependants);
        this.dropsResources = config.getBooleanOrDefault("drops_resources", true);
        this.hatchEntity = config.getEntityType("hatch_entity");
        this.hatchCount = config.getRandomizedNumberExpression("hatch_count");
        Number startingAgeNumber = config.getNumberNullable("starting_age");
        this.startingAge = startingAgeNumber == null ? null : startingAgeNumber.intValue();
    }

    @Override
    public boolean isDependable() {
        return false;
    }

    @Override
    public boolean shouldCalculateDuration(BlockState state, ServerLevel level, BlockPos pos) {
        return true;
    }

    @Override
    public int getMaxUpdateCount(BlockState state, ServerLevel level, BlockPos pos) {
        return 1;
    }

    @Override
    public DeferredBlockPlacer.SingleBlockPlacement getNewBlockState(BlockState state, ServerLevel level, BlockPos pos, OccurrencesAndTimings occurrencesAndTimings, @Nullable ActiveGroupSimulateData groupSimulateData) {
        if (this.dropsResources) {
            Block.dropResources(state, level, pos);
        }

        SimulatedTime finalTime = occurrencesAndTimings.getFinalTime();
        long finishedAtTime = finalTime.currentTime();
        int hatchCount = this.hatchCount.evaluateRandomized(level, state, pos, finishedAtTime).intValue();

        if (hatchCount > 0) {
            for(int i = 0; i < hatchCount; i++) {
                Entity hatchedEntity = this.hatchEntity.create(
                    level
                    #if MC_VER >= MC_1_21_3 , EntitySpawnReason.BREEDING #endif
                );
                if (hatchedEntity == null)
                    continue;

                #if MC_VER >= MC_1_21_5
                hatchedEntity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                #else
                hatchedEntity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                #endif

                if (this.startingAge != null && hatchedEntity instanceof AgeableMob ageableMob) {
                    ageableMob.setAge(this.startingAge);
                }

                if (hatchedEntity instanceof Turtle turtle) {
                    turtle.setHomePos(pos);
                }

                level.addFreshEntity(hatchedEntity);
                hatchedEntity.unloadedactivity$simulateTime(finalTime.remainingTime());
            }
        }

        return new DeferredBlockPlacer.SingleBlockPlacement(state.getFluidState().createLegacyBlock(), finalTime);
    }
}
