package lol.zanspace.unloadedactivity.mixin;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GoalSelector.class)
public interface GoalSelectorAccessor {
    @Accessor("newGoalRate")
    int unloadedactivity$getNewGoalRate();
}
