package dev.moono.unloadedactivity.impl.entity_simulations;

import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Unique;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class AgeableMobSimulation {
    public static void simulate(AgeableMob ageableMob, long ticksPassed, double multiplier) {
        if (ageableMob.isRemoved()) return;
        if (!ageableMob.isAlive()) return;

        ticksPassed = (long) (ticksPassed * multiplier);


        int age = ageableMob.getAge();
        boolean canAgeUp = #if MC_VER >= MC_26_1_2 ageableMob.canAgeUp() #else age < 0 #endif;
        boolean canAgeDown = age > 0;

        if (canAgeUp) {
            ageableMob.setAge((int)min(0, age + ticksPassed));
        } else if (canAgeDown) {
            ageableMob.setAge((int)max(0, age - ticksPassed));
        }
    }
}
