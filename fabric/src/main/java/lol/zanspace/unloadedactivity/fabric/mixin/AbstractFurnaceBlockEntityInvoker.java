package lol.zanspace.unloadedactivity.fabric.mixin;

#if MC_VER >= MC_1_19_4
import net.minecraft.core.RegistryAccess;
#endif
#if MC_VER >= MC_1_21_3
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
#elif MC_VER >= MC_1_20_2
import net.minecraft.world.item.crafting.RecipeHolder;
#else
import net.minecraft.world.item.crafting.Recipe;
#endif
#if MC_VER >= MC_1_21_3
import net.minecraft.world.item.crafting.SingleRecipeInput;
#endif

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityInvoker {
    @Invoker("burn")
    #if MC_VER >= MC_26_1_2
    public static void invokeBurn(
        final NonNullList<ItemStack> items,
        final ItemStack inputItemStack,
        final ItemStack result
    )
    #else
    public static boolean invokeBurn(
        #if MC_VER >= MC_1_19_4
        RegistryAccess registryAccess,
        #endif
        #if MC_VER >= MC_1_21_3
        @Nullable RecipeHolder<? extends AbstractCookingRecipe>
        #elif MC_VER >= MC_1_20_2
        @Nullable RecipeHolder<?>
        #else
        @Nullable Recipe<?>
        #endif
        recipe,
        #if MC_VER >= MC_1_21_3
        SingleRecipeInput input,
        #endif
        NonNullList<ItemStack> slots,
        int count
    )
    #endif
    {
        throw new AssertionError();
    }
}