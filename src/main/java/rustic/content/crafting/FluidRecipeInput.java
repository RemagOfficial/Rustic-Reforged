package rustic.content.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The {@link RecipeInput} for the fluid-fed machines (brewing barrel, evaporating basin). Those recipes match on a
 * single {@link FluidStack}, not on item slots, but the 1.21 {@code Recipe} interface is generic over a
 * {@code RecipeInput} that only exposes {@code getItem}/{@code size}. This record wraps the one fluid the machine
 * offers and satisfies the item contract with a lone empty slot; matching code reads {@link #fluid()} directly.
 *
 * <p>Replaces the legacy {@code matches(FluidStack)} methods that 1.12.2 put on the recipe classes beside the
 * (absent) {@code IRecipe} interface machinery, which had no {@code RecipeInput} to funnel a fluid through.
 */
public record FluidRecipeInput(FluidStack fluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        // There is no item input on a fluid-fed recipe; index 0 is the (empty) slot the interface demands.
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }
}
