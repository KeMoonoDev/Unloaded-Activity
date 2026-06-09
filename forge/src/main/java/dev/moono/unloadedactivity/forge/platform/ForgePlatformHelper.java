package dev.moono.unloadedactivity.forge.platform;

#if MC_VER >= MC_1_19_4
import net.minecraft.core.RegistryAccess;
#endif

import dev.moono.unloadedactivity.forge.mixin.AbstractFurnaceBlockEntityInvoker;
import dev.moono.unloadedactivity.platform.IPlatformHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper {
    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean burn(
        #if MC_VER >= MC_1_19_4
        RegistryAccess registryAccess,
        #endif
        @Nullable Recipe<?> recipe,
        NonNullList<ItemStack> slots,
        int count,
        AbstractFurnaceBlockEntity furnace
    )  {
        AbstractFurnaceBlockEntityInvoker furnaceInvoker = (AbstractFurnaceBlockEntityInvoker) furnace;
        return furnaceInvoker.invokeBurn(#if MC_VER >= MC_1_19_4 registryAccess, #endif recipe, slots, count);
    }
}
