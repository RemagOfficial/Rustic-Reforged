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
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.crafting.CondenserRecipe;

/**
 * The condenser / advanced condenser: a fixed set of ingredient items plus water and a glass bottle brew, over time,
 * into an elixir. One class serves both legacy categories ({@code SimpleAlchemy} and {@code AdvancedAlchemy}) - the
 * only difference is the extra modifier-herb slot the advanced variant gates on, so the {@code advanced} flag drives
 * the layout. The plugin splits the single vanilla {@code CONDENSER} recipe type across the two JEI categories by
 * {@link CondenserRecipe#isAdvanced()}.
 *
 * <p>Port of 1.12.2's {@code SimpleAlchemyRecipeCategory} / {@code AdvancedAlchemyRecipeCategory}. The elixir result
 * carries its {@code rustic:elixir_effects} data component; JEI renders it as the plain elixir item, which is enough
 * to browse the recipe (effect detail is a later UI concern).
 */
public class CondenserRecipeCategory extends AbstractRecipeCategory<RecipeHolder<CondenserRecipe>> {

    private final boolean advanced;

    public CondenserRecipeCategory(IGuiHelper guiHelper, RecipeType<RecipeHolder<CondenserRecipe>> recipeType,
                                   boolean advanced) {
        super(recipeType,
                Component.translatable((advanced ? RusticMachineBlocks.CONDENSER_ADVANCED : RusticMachineBlocks.CONDENSER)
                        .get().getDescriptionId()),
                guiHelper.createDrawableItemLike((advanced ? RusticMachineBlocks.CONDENSER_ADVANCED : RusticMachineBlocks.CONDENSER)
                        .get().asItem()),
                150, 80);
        this.advanced = advanced;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CondenserRecipe> holder, IFocusGroup focuses) {
        CondenserRecipe recipe = holder.value();

        // Base fluid (water) the condenser must hold.
        FluidStack fluid = recipe.getFluid();
        IRecipeSlotBuilder water = builder.addInputSlot(6, 8);
        water.setStandardSlotBackground();
        water.setFluidRenderer(1, false, 16, 16);
        water.addFluidStack(fluid.getFluid(), fluid.getAmount());

        // The bottle it consumes.
        IRecipeSlotBuilder bottle = builder.addInputSlot(6, 30);
        bottle.setStandardSlotBackground();
        bottle.addItemStack(recipe.getBottle());

        // Advanced variant only: the modifier herb gate.
        if (advanced) {
            IRecipeSlotBuilder modifier = builder.addInputSlot(6, 52);
            modifier.setStandardSlotBackground();
            Ingredient mod = recipe.getModifier();
            if (mod != null && !mod.isEmpty()) {
                modifier.addIngredients(mod);
            }
        }

        // The ingredient multiset, laid out as a wrapping grid.
        List<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            int x = 50 + (i % 3) * 20;
            int y = 8 + (i / 3) * 20;
            IRecipeSlotBuilder slot = builder.addInputSlot(x, y);
            slot.setStandardSlotBackground();
            slot.addIngredients(ingredients.get(i));
        }

        // The brewed elixir.
        ItemStack result = recipe.getResultItem(null);
        IRecipeSlotBuilder output = builder.addOutputSlot(124, 30);
        output.setOutputSlotBackground();
        output.addItemStack(result);
    }
}
