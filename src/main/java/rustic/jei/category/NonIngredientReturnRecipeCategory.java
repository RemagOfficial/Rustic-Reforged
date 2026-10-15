package rustic.jei.category;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import rustic.content.crafting.RecipeNonIngredientReturn;

/**
 * The non-ingredient-return crafting recipes (ale wort, bottle emptying, barrel emptying): a data-driven shapeless
 * craft whose only quirk is that the consumed container's remainder is dropped. The three generated rows carry real
 * ingredient lists and results, so this category renders them straight from {@link RecipeNonIngredientReturn} rather
 * than a synthetic example. Port of the legacy {@code ShapelessOreRecipe} handling in {@code Recipes.addCraftingRecipes}.
 */
public class NonIngredientReturnRecipeCategory extends AbstractRecipeCategory<RecipeHolder<RecipeNonIngredientReturn>> {

    public NonIngredientReturnRecipeCategory(IGuiHelper guiHelper,
                                             RecipeType<RecipeHolder<RecipeNonIngredientReturn>> recipeType) {
        super(recipeType,
                Component.translatable("jei.rustic.non_ingredient_return"),
                guiHelper.createDrawableItemLike(net.minecraft.world.item.Items.GLASS_BOTTLE),
                120, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RecipeNonIngredientReturn> holder, IFocusGroup focuses) {
        RecipeNonIngredientReturn recipe = holder.value();

        List<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            int x = 4 + (i % 4) * 20;
            int y = 12 + (i / 4) * 20;
            IRecipeSlotBuilder slot = builder.addInputSlot(x, y);
            slot.setStandardSlotBackground();
            slot.addIngredients(ingredients.get(i));
        }

        ItemStack result = recipe.getResultItem(null);
        IRecipeSlotBuilder output = builder.addOutputSlot(100, 12);
        output.setOutputSlotBackground();
        output.addItemStack(result);

        builder.setShapeless();
    }
}
