package dev.moono.unloadedactivity.mixin.block_entities;

#if MC_VER >= MC_26_1_2
import net.minecraft.world.item.ItemStackTemplate;
#endif

#if MC_VER >= MC_1_21_3
import net.minecraft.world.level.block.entity.FuelValues;
#endif

#if MC_VER >= MC_1_21_1
#else
import net.minecraft.world.Container;
#endif

import dev.moono.unloadedactivity.interfaces.SimulateBlockEntity;
import dev.moono.unloadedactivity.UnloadedActivity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static java.lang.Math.*;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin extends BaseContainerBlockEntity implements WorldlyContainer, StackedContentsCompatible, SimulateBlockEntity {

    protected AbstractFurnaceBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    #if MC_VER >= MC_1_21_4
    @Shadow int litTimeRemaining;
    @Shadow int litTotalTime;
    @Shadow int cookingTimer;
    @Shadow int cookingTotalTime;


    private int unloadedactivity$getLitTimeRemaining() {
        return this.litTimeRemaining;
    }

    private int unloadedactivity$getLitTotalTime() {
        return this.litTotalTime;
    }

    private int unloadedactivity$getCookingTimer() {
        return this.cookingTimer;
    }

    private int unloadedactivity$getCookingTotalTime() {
        return this.cookingTotalTime;
    }


    private void unloadedactivity$setLitTimeRemaining(int value) {
        this.litTimeRemaining = value;
    }

    private void unloadedactivity$setLitTotalTime(int value) {
        this.litTotalTime = value;
    }

    private void unloadedactivity$setCookingTimer(int value) {
        this.cookingTimer = value;
    }

    private void unloadedactivity$setCookingTotalTime(int value) {
        this.cookingTotalTime = value;
    }
    #else
    @Shadow int litTime;
    @Shadow int litDuration;
    @Shadow int cookingProgress;
    @Shadow int cookingTotalTime;


    private int unloadedactivity$getLitTimeRemaining() {
        return this.litTime;
    }

    private int unloadedactivity$getLitTotalTime() {
        return this.litDuration;
    }

    private int unloadedactivity$getCookingTimer() {return this.cookingProgress;}

    private int unloadedactivity$getCookingTotalTime() {
        return this.cookingTotalTime;
    }


    private void unloadedactivity$setLitTimeRemaining(int value) {
        this.litTime = value;
    }

    private void unloadedactivity$setLitTotalTime(int value) {
        this.litDuration = value;
    }

    private void unloadedactivity$setCookingTimer(int value) {
        this.cookingProgress = value;
    }

    private void unloadedactivity$setCookingTotalTime(int value) {
        this.cookingTotalTime = value;
    }
    #endif
    #if MC_VER >= MC_1_21_1
    @Shadow @Final private RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;
    #else
    @Shadow @Final private RecipeManager.CachedCheck<Container, ? extends AbstractCookingRecipe> quickCheck;
    #endif

    #if MC_VER >= MC_1_21_3
    @Shadow private static int getTotalCookTime(ServerLevel level, AbstractFurnaceBlockEntity furnace) {
        return 0;
    }
    #else
    @Shadow private static int getTotalCookTime(Level level, AbstractFurnaceBlockEntity furnace) {
        return 0;
    }
    #endif

    @Unique
    private boolean isLit() {
        return unloadedactivity$getLitTimeRemaining() > 0;
    }
    @Shadow protected NonNullList<ItemStack> items;

    #if MC_VER >= MC_1_21_3
    @Shadow protected abstract int getBurnDuration(FuelValues fuelValues, ItemStack fuel);
    #else
    @Shadow protected abstract int getBurnDuration(ItemStack fuel);
    #endif

    @Shadow
    public abstract void setRecipeUsed(@Nullable #if MC_VER >= MC_1_20_2 RecipeHolder<?> #else Recipe<?> #endif recipe);

    @Unique
    public boolean shouldSimulate(BlockState state) {
        if (state == null) return false;
        return UnloadedActivity.config.simulateFurnaceSmelting;
    }

    @Override public void unloadedactivity$simulateTime(long timeDifference)  {
        super.unloadedactivity$simulateTime(timeDifference);


        BlockState state = this.getBlockState();
        BlockPos pos = this.getBlockPos();

        if (shouldSimulate(state)) {
            AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
            boolean oldIsLit = this.isLit();
            boolean stateChanged = false;
            ItemStack ingredient = this.items.get(0);
            ItemStack fuelStack = this.items.get(1);
            ItemStack finishedStack = this.items.get(2);
            int inputCount = ingredient.getCount();
            int fuelCount = fuelStack.getCount();

            #if MC_VER >= MC_1_21_1
            SingleRecipeInput singleRecipeInput = new SingleRecipeInput(ingredient);
            #endif

            var recipe = inputCount != 0 ?
                #if MC_VER >= MC_1_21_1
                this.quickCheck.getRecipeFor(singleRecipeInput, (ServerLevel) level).orElse(null)
                #else
                this.quickCheck.getRecipeFor(this, level).orElse(null)
                #endif
            : null;

            #if MC_VER >= MC_1_21_3
            int burnDuration = this.getBurnDuration(level.fuelValues(), fuelStack);
            #else
            int burnDuration = this.getBurnDuration(fuelStack);
            #endif
            if (burnDuration == 0)
                burnDuration = this.unloadedactivity$getCookingTimer();

            if (burnDuration != 0) {
                if (this.unloadedactivity$getCookingTotalTime() == 0)
                    this.unloadedactivity$setCookingTotalTime(getTotalCookTime( (ServerLevel) level, furnace));

                int spacesLeft = getMaxStackSize() - finishedStack.getCount();

                //The amount of time to burn before we catch up to now or until we run out of something.
                int availableBurning = 0;

                if (recipe != null) { //if recipe is null then availableBurning will remain 0
                    availableBurning = (int) min(timeDifference, (long) this.unloadedactivity$getCookingTotalTime() * min(inputCount, spacesLeft) - this.unloadedactivity$getCookingTimer());
                    availableBurning = min(availableBurning, burnDuration * fuelCount + this.unloadedactivity$getLitTimeRemaining());
                }

                long leftoverTime = timeDifference - availableBurning;

                int fuelsConsumed = (int) ceil((float) max(availableBurning - this.unloadedactivity$getLitTimeRemaining(), 0) / (float) burnDuration);
                this.unloadedactivity$setLitTimeRemaining((int) max((this.unloadedactivity$getLitTimeRemaining() - availableBurning + fuelsConsumed * burnDuration) - (int) leftoverTime, 0));

                int itemsCrafted = (availableBurning + this.unloadedactivity$getCookingTimer()) / this.unloadedactivity$getCookingTotalTime();
                this.unloadedactivity$setCookingTimer((int) max(((availableBurning + this.unloadedactivity$getCookingTimer()) % this.unloadedactivity$getCookingTotalTime()) - leftoverTime * 2, 0));

                if (fuelsConsumed > 0) {
                    stateChanged = true;
                    Item fuelItem = fuelStack.getItem();
                    fuelStack.shrink(fuelsConsumed);
                    if (fuelStack.isEmpty()) {
                        #if MC_VER >= MC_26_1_2
                        ItemStackTemplate remainder = fuelItem.getCraftingRemainder();
                        this.items.set(1, remainder != null ? remainder.create() : ItemStack.EMPTY);
                        #elif MC_VER >= MC_1_21_3
                        this.items.set(1, fuelItem.getCraftingRemainder());
                        #else
                        Item remainder = fuelItem.getCraftingRemainingItem();
                        this.items.set(1, remainder == null ? ItemStack.EMPTY : new ItemStack(remainder));
                        #endif
                    }
                }

                if (itemsCrafted > 0) {
                    stateChanged = true;
                    for (int i = 0; i < itemsCrafted; i++) {
                        #if MC_VER >= MC_26_1_2
                        ItemStack burnResult = recipe.value().assemble(singleRecipeInput);
                        UnloadedActivity.platform.burn(
                            this.items,
                            ingredient,
                            burnResult,
                            furnace
                        );
                        #else
                        UnloadedActivity.platform.burn(
                            #if MC_VER >= MC_1_19_4 level.registryAccess(), #endif recipe,
                            #if MC_VER >= MC_1_21_3 singleRecipeInput, #endif this.items,
                            getMaxStackSize(),
                            furnace
                        );
                        #endif
                        setRecipeUsed(recipe);
                    }
                }

                if (ingredient.getCount() == 0 || getMaxStackSize() - finishedStack.getCount() == 0)
                    this.unloadedactivity$setCookingTimer(0);

                if (oldIsLit != isLit()) {
                    stateChanged = true;
                    state = state.setValue(AbstractFurnaceBlock.LIT, isLit());
                    level.setBlock(pos, state, Block.UPDATE_ALL);
                }

                if (!isLit())
                    this.unloadedactivity$setLitTotalTime(0);

                if (stateChanged) {
                    AbstractFurnaceBlockEntity.setChanged(level, pos, state);
                }
            }
        }
    }
}
