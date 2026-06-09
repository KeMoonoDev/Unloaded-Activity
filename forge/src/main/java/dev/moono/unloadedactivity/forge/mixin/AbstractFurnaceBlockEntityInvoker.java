package dev.moono.unloadedactivity.forge.mixin;

#if MC_VER >= MC_1_19_4
import net.minecraft.core.RegistryAccess;
#endif
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityInvoker {
    @Invoker("burn")
    public boolean invokeBurn(
        #if MC_VER >= MC_1_19_4
        RegistryAccess registryAccess,
        #endif
        @Nullable Recipe<?> recipe,
        NonNullList<ItemStack> slots,
        int count
    );
}