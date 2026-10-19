package rustic.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import rustic.content.crafting.RecipeVantaOil;
import rustic.content.fluids.RusticFluids;

public class VantaOilRecipeCategory extends AbstractRecipeCategory<RecipeHolder<RecipeVantaOil>> {

    public VantaOilRecipeCategory(IGuiHelper guiHelper, RecipeType<RecipeHolder<RecipeVantaOil>> recipeType) {
        super(recipeType,
                Component.translatable("jei.rustic.vanta_oiling"),
                guiHelper.createDrawableIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK,
                        new net.neoforged.neoforge.fluids.FluidStack(RusticFluids.VANTA_OIL.get(), 1000)),
                110, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RecipeVantaOil> holder, IFocusGroup focuses) {
        IRecipeSlotBuilder weapon = builder.addInputSlot(4, 12);
        weapon.setStandardSlotBackground();
        weapon.addIngredients(Ingredient.of(Items.DIAMOND_SWORD));

        IRecipeSlotBuilder oil = builder.addInputSlot(28, 12);
        oil.setStandardSlotBackground();
        oil.setFluidRenderer(1, false, 16, 16);
        oil.addFluidStack(RusticFluids.VANTA_OIL.get(), 1000);

        IRecipeSlotBuilder output = builder.addOutputSlot(84, 12);
        output.setOutputSlotBackground();
        output.addItemStack(new ItemStack(Items.DIAMOND_SWORD));

        builder.setShapeless();
    }
}
