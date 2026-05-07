package lol.zanspace.unloadedactivity.forge;

#if MC_VER >= MC_1_19_4
import net.minecraft.core.RegistryAccess;
#endif
import lol.zanspace.unloadedactivity.UnloadedActivity;

import lol.zanspace.unloadedactivity.forge.mixin.AbstractFurnaceBlockEntityInvoker;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class ExpectPlatformImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
    public static boolean burn(
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
