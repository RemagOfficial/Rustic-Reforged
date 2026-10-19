package rustic.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.crafting.BrewingBarrelRecipe;

public class BrewingBarrelRecipeCategory extends AbstractRecipeCategory<RecipeHolder<BrewingBarrelRecipe>> {

    public BrewingBarrelRecipeCategory(IGuiHelper guiHelper, RecipeType<RecipeHolder<BrewingBarrelRecipe>> recipeType) {
        super(recipeType,
                Component.translatable(RusticMachineBlocks.BREWING_BARREL.get().getDescriptionId()),
                guiHelper.createDrawableItemLike(RusticMachineBlocks.BREWING_BARREL.get().asItem()),
                110, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BrewingBarrelRecipe> holder, IFocusGroup focuses) {
        BrewingBarrelRecipe recipe = holder.value();

        FluidStack in = recipe.getInput();
        IRecipeSlotBuilder input = builder.addInputSlot(6, 12);
        input.setStandardSlotBackground();
        input.setFluidRenderer(1, false, 16, 16);
        input.addFluidStack(in.getFluid(), in.getAmount());

        FluidStack out = recipe.getOutputFluid();
        IRecipeSlotBuilder output = builder.addOutputSlot(84, 12);
        output.setOutputSlotBackground();
        output.setFluidRenderer(1, false, 16, 16);
        output.addFluidStack(out.getFluid(), out.getAmount());
    }
}
