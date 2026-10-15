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
import rustic.content.fluids.RusticFluids;
import rustic.content.crafting.RecipeOliveOil;

/**
 * Olive oiling. {@link RecipeOliveOil} is a stateless, config-gated pattern recipe: any eligible food plus a fluid
 * bottle of olive oil yields that food marked {@code rustic:oiled}. There is no enumerable per-recipe data, so this
 * category renders one representative example (the legacy {@code OliveOilRecipeWrapper} did the same) - bread plus
 * olive oil in, oiled bread out. The oil is shown as a searchable fluid ingredient.
 */
public class OliveOilRecipeCategory extends AbstractRecipeCategory<RecipeHolder<RecipeOliveOil>> {

    public OliveOilRecipeCategory(IGuiHelper guiHelper, RecipeType<RecipeHolder<RecipeOliveOil>> recipeType) {
        super(recipeType,
                Component.translatable("jei.rustic.olive_oiling"),
                guiHelper.createDrawableIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK,
                        new net.neoforged.neoforge.fluids.FluidStack(RusticFluids.OLIVE_OIL.get(), 1000)),
                110, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RecipeOliveOil> holder, IFocusGroup focuses) {
        IRecipeSlotBuilder food = builder.addInputSlot(4, 12);
        food.setStandardSlotBackground();
        food.addIngredients(Ingredient.of(Items.BREAD));

        IRecipeSlotBuilder oil = builder.addInputSlot(28, 12);
        oil.setStandardSlotBackground();
        oil.setFluidRenderer(1, false, 16, 16);
        oil.addFluidStack(RusticFluids.OLIVE_OIL.get(), 1000);

        IRecipeSlotBuilder output = builder.addOutputSlot(84, 12);
        output.setOutputSlotBackground();
        output.addItemStack(new ItemStack(Items.BREAD));

        builder.setShapeless();
    }
}
