package lol.zanspace.unloadedactivity.mixin.entities;

import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.Utils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Mixin(Sheep.class)
abstract public class SheepMixin extends Animal implements Shearable {
    protected SheepMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public boolean isSheared() {return false;};

    @Shadow
    private EatBlockGoal eatBlockGoal;

    @Unique
    private boolean shouldSimulate() {
        if (!UnloadedActivity.config.simulateSheepDesireToEat) return false;
        if (this.isRemoved()) return false;
        if (!this.isAlive()) return false;

        boolean isAlreadyEating = true;
        for (var wrappedGoal : this.goalSelector.getAvailableGoals()) {
            if (wrappedGoal.getGoal().equals(this.eatBlockGoal)) {
                isAlreadyEating = wrappedGoal.isRunning();
                break;
            }
        }
        if (isAlreadyEating) return false;
        // this condition is not very accurate, BUT!!!
        // Sheared sheep may get their grass stolen from unsheared sheep,
        // and because this is a game, and we want the player to be happy,
        // it'd be nicer to only make sheared sheep want to eat.
        if (!this.isSheared()) return false;
        return true;
    }

    @Override
    public void unloadedactivity$simulateTime(long timeDifference) {
        super.unloadedactivity$simulateTime(timeDifference);

        if (!shouldSimulate())
            return;

        RandomSource randomSource = this.getRandom();
        int doesWantToEat = Utils.getOccurrencesBinomial(timeDifference / 3, 1.0f/1000.0f, 1, randomSource);

        if (doesWantToEat == 0) {
            return;
        }

        for (var wrappedGoal : this.goalSelector.getAvailableGoals()) {
            if (wrappedGoal.getGoal().equals(this.eatBlockGoal)) {
                wrappedGoal.start();
                break;
            }
        }
    }
}
