package rustic.content.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

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
