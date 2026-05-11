package lol.zanspace.unloadedactivity.neoforge.platform;

#if MC_VER >= MC_1_21_1
import lol.zanspace.unloadedactivity.neoforge.mixin.CropBlockInvoker;
#endif

#if MC_VER >= MC_1_19_4
import lol.zanspace.unloadedactivity.platform.IPlatformHelper;
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
import lol.zanspace.unloadedactivity.neoforge.mixin.AbstractFurnaceBlockEntityInvoker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
    #if MC_VER >= MC_1_21_1
    @Override
    public float getGrowthSpeed(BlockState blockState, BlockGetter blockGetter, BlockPos pos) {
        return CropBlockInvoker.invokeGetGrowthSpeed(blockState, blockGetter, pos);
    }
    #endif
    @Override
    public boolean burn(
        #if MC_VER >= MC_26_1_2
            final NonNullList<ItemStack> items,
            final ItemStack inputItemStack,
            final ItemStack result,
        #else
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
            int count,
        #endif
        AbstractFurnaceBlockEntity furnace
    ) {
        #if MC_VER >= MC_26_1_2
        AbstractFurnaceBlockEntityInvoker.invokeBurn(
                items,
                inputItemStack,
                result
        );
        return true;
        #else
        #if MC_VER >= MC_1_20_6
        return AbstractFurnaceBlockEntityInvoker.invokeBurn(
            #if MC_VER >= MC_1_19_4 registryAccess, #endif
            recipe,
            #if MC_VER >= MC_1_21_3 input, #endif
            slots,
            count
            #if MC_VER <= MC_1_21_1 , furnace #endif
        );
        #else
        AbstractFurnaceBlockEntityInvoker furnaceInvoker = (AbstractFurnaceBlockEntityInvoker) furnace;
        return furnaceInvoker.invokeBurn(#if MC_VER >= MC_1_19_4 registryAccess, #endif recipe, slots, count);
        #endif
        #endif
    }
}
