package rustic.content.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record FluidRecipeInput(FluidStack fluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {

        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }
}
