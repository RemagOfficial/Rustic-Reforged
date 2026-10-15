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
import rustic.content.crafting.EvaporatingBasinRecipe;

/**
 * The evaporating basin: an input fluid evaporates over time into an output dust item. Port of 1.12.2's
 * {@code EvaporatingRecipeCategory}.
 */
public class EvaporatingBasinRecipeCategory extends AbstractRecipeCategory<RecipeHolder<EvaporatingBasinRecipe>> {

    public EvaporatingBasinRecipeCategory(IGuiHelper guiHelper, RecipeType<RecipeHolder<EvaporatingBasinRecipe>> recipeType) {
        super(recipeType,
                Component.translatable(RusticMachineBlocks.EVAPORATING_BASIN.get().getDescriptionId()),
                guiHelper.createDrawableItemLike(RusticMachineBlocks.EVAPORATING_BASIN.get().asItem()),
                110, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<EvaporatingBasinRecipe> holder, IFocusGroup focuses) {
        EvaporatingBasinRecipe recipe = holder.value();

        FluidStack input = recipe.getInput();
        IRecipeSlotBuilder in = builder.addInputSlot(6, 12);
        in.setStandardSlotBackground();
        in.setFluidRenderer(1, false, 16, 16);
        in.addFluidStack(input.getFluid(), input.getAmount());

        IRecipeSlotBuilder out = builder.addOutputSlot(84, 12);
        out.setOutputSlotBackground();
        out.addItemStack(recipe.getOutput());
    }
}
