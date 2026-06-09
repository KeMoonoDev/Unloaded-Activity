package dev.moono.unloadedactivity.fabric.platform;

import dev.moono.unloadedactivity.platform.IPlatformHelper;

#if MC_VER >= MC_1_21_1
import dev.moono.unloadedactivity.fabric.mixin.CropBlockInvoker;
#endif

#if MC_VER >= MC_1_19_4
#endif
#if MC_VER >= MC_1_21_3
#elif MC_VER >= MC_1_20_2
import net.minecraft.world.item.crafting.RecipeHolder;
#else
import net.minecraft.world.item.crafting.Recipe;
#endif
#if MC_VER >= MC_1_21_3
#endif


import dev.moono.unloadedactivity.fabric.mixin.AbstractFurnaceBlockEntityInvoker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
    #if MC_VER >= MC_1_21_1
    @Override
    public float getGrowthSpeed(BlockState blockState, BlockGetter blockGetter, BlockPos pos) {
        return CropBlockInvoker.invokeGetGrowthSpeed(blockState.getBlock(), blockGetter, pos);
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
        return AbstractFurnaceBlockEntityInvoker.invokeBurn(
            #if MC_VER >= MC_1_19_4 registryAccess, #endif
            recipe,
            #if MC_VER >= MC_1_21_3 input, #endif
            slots,
            count
        );
        #endif
    }
}
