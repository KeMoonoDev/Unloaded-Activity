package lol.zanspace.unloadedactivity.mixin.entities;

import lol.zanspace.unloadedactivity.UnloadedActivity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static java.lang.Math.max;
import static java.lang.Math.min;


@Mixin(AgeableMob.class)
public abstract class AgeableMobMixin extends PathfinderMob {

    protected AgeableMobMixin(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public int getAge() {
        return 0;
    }

    @Shadow
    public void setAge(int i) {}

    @Unique
    private boolean shouldSimulate() {
        if (!UnloadedActivity.config.simulateEntitiesAgeing) return false;
        if (this.isRemoved()) return false;
        if (!this.isAlive()) return false;
        if (this.getAge() == 0) return false;
        return true;
    }

    @Override
    public void unloadedactivity$simulateTime(long timeDifference) {
        super.unloadedactivity$simulateTime(timeDifference);

        if (!shouldSimulate())
            return;

        int age = this.getAge();
        if (age < 0) {
            this.setAge((int)min(0,age+timeDifference));
        } else if (age > 0) {
            this.setAge((int)max(0,age-timeDifference));
        }
    }
}
