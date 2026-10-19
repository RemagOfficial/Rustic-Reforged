package rustic.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.crafting.CrushingTubRecipe;

public class CrushingTubRecipeCategory extends AbstractRecipeCategory<RecipeHolder<CrushingTubRecipe>> {

    public CrushingTubRecipeCategory(IGuiHelper guiHelper, RecipeType<RecipeHolder<CrushingTubRecipe>> recipeType) {
        super(recipeType,
                Component.translatable(RusticMachineBlocks.CRUSHING_TUB.get().getDescriptionId()),
                guiHelper.createDrawableItemLike(RusticMachineBlocks.CRUSHING_TUB.get().asItem()),
                120, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrushingTubRecipe> holder, IFocusGroup focuses) {
        CrushingTubRecipe recipe = holder.value();

        IRecipeSlotBuilder input = builder.addInputSlot(2, 12);
        input.setStandardSlotBackground();
        input.addIngredients(recipe.getInput());

        FluidStack fluid = recipe.getFluid();
        IRecipeSlotBuilder output = builder.addOutputSlot(52, 12);
        output.setOutputSlotBackground();
        output.setFluidRenderer(1, false, 16, 16);
        output.addFluidStack(fluid.getFluid(), fluid.getAmount());

        ItemStack byproduct = recipe.getByproduct();
        if (!byproduct.isEmpty()) {
            IRecipeSlotBuilder by = builder.addOutputSlot(96, 12);
            by.setOutputSlotBackground();
            by.addItemStack(byproduct);
        }
    }
}
