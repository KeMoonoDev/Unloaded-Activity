package lol.zanspace.unloadedactivity.fabric.platform;

import lol.zanspace.unloadedactivity.platform.IPlatformHelper;
import org.jetbrains.annotations.Nullable;

#if MC_VER >= MC_1_21_1
import lol.zanspace.unloadedactivity.fabric.mixin.CropBlockInvoker;
#endif

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


import lol.zanspace.unloadedactivity.fabric.mixin.AbstractFurnaceBlockEntityInvoker;
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
        AbstractFurnaceBlockEntity furnace
    ) {
        return AbstractFurnaceBlockEntityInvoker.invokeBurn(
            #if MC_VER >= MC_1_19_4 registryAccess, #endif
            recipe,
            #if MC_VER >= MC_1_21_3 input, #endif
            slots,
            count
        );
    }
}
