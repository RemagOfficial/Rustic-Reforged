package rustic.content.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The {@link RecipeInput} the condenser offers a {@link CondenserRecipe} when it tries to match: the ingredient-slot
 * stacks plus the bottle slot, the advanced modifier slot, and the tank fluid. Legacy {@code CondenserRecipe.matches}
 * took {@code (Fluid, ItemStack modifier, ItemStack bottle, ItemStack[] inputs)}; this object bundles the same four
 * inputs so the {@code Recipe<CondenserRecipeInput>} contract can be honoured and the matching logic stays where the
 * data is.
 */
public final class CondenserRecipeInput implements RecipeInput {

    private final ItemStack[] ingredients;
    private final ItemStack bottle;
    private final ItemStack modifier;
    private final FluidStack fluid;

    public CondenserRecipeInput(ItemStack[] ingredients, ItemStack bottle, ItemStack modifier, FluidStack fluid) {
        this.ingredients = ingredients;
        this.bottle = bottle;
        this.modifier = modifier;
        this.fluid = fluid;
    }

    public ItemStack bottle() {
        return bottle;
    }

    public ItemStack modifier() {
        return modifier;
    }

    public FluidStack fluid() {
        return fluid;
    }

    public ItemStack[] ingredients() {
        return ingredients;
    }

    @Override
    public ItemStack getItem(int index) {
        return ingredients[index];
    }

    @Override
    public int size() {
        return ingredients.length;
    }
}
