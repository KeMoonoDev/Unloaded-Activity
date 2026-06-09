package dev.moono.unloadedactivity.mixin.entities;

import dev.moono.unloadedactivity.UnloadedActivity;
import net.minecraft.world.entity.AgeableMob;
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

    #if MC_VER >= MC_26_1_2
    @Shadow
    public boolean canAgeUp() {
        throw new RuntimeException("Shadow no work.");
    }
    #else
    @Unique
    private boolean canAgeUp() {
        return this.getAge() < 0;
    }
    #endif

    @Unique
    private boolean canAgeDown() {
        return this.getAge() > 0;
    }

    @Unique
    private boolean shouldSimulate() {
        if (!UnloadedActivity.config.simulateEntitiesAgeing) return false;
        if (this.isRemoved()) return false;
        if (!this.isAlive()) return false;
        return canAgeUp() || canAgeDown();
    }

    @Override
    public void unloadedactivity$simulateTime(long timeDifference) {
        super.unloadedactivity$simulateTime(timeDifference);

        if (!shouldSimulate())
            return;

        if (canAgeUp()) {
            this.setAge((int)min(0,this.getAge()+timeDifference));
        } else if (canAgeDown()) {
            this.setAge((int)max(0,this.getAge()-timeDifference));
        }
    }
}
