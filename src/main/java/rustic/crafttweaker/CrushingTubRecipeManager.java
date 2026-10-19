package rustic.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.recipe.ActionAddRecipe;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import org.openzen.zencode.java.ZenCodeGlobals;
import org.openzen.zencode.java.ZenCodeType;
import rustic.content.crafting.CrushingTubRecipe;
import rustic.registry.RusticRegistries;

@ZenRegister
@ZenCodeType.Name("mods.rustic.CrushingTubRecipeManager")
public class CrushingTubRecipeManager implements IRecipeManager<CrushingTubRecipe> {

    @ZenCodeGlobals.Global("crushingTub")
    public static final CrushingTubRecipeManager INSTANCE = new CrushingTubRecipeManager();

    private CrushingTubRecipeManager() {
    }

    @Override
    public RecipeType<CrushingTubRecipe> getRecipeType() {
        return RusticRegistries.CRUSHING_TUB_TYPE.get();
    }

    @ZenCodeType.Method
    public void add(String name, IItemStack input, IFluidStack output) {
        addRecipe(name, input, output, null);
    }

    @ZenCodeType.Method
    public void addWithByproduct(String name, IItemStack input, IFluidStack output, IItemStack byproduct) {
        addRecipe(name, input, output, byproduct);
    }

    private void addRecipe(String name, IItemStack input, IFluidStack output, IItemStack byproduct) {
        ItemStack by = byproduct == null ? ItemStack.EMPTY : byproduct.getInternal();
        CrushingTubRecipe recipe = new CrushingTubRecipe(
                input.asVanillaIngredient(),
                output.<FluidStack>getInternal(),
                by);
        CraftTweakerAPI.apply(new ActionAddRecipe<>(this, createHolder(fixRecipeId(name), recipe)));
    }

    @ZenCodeType.Method
    public void removeByOutputFluid(IFluidStack output) {
        FluidStack target = output.<FluidStack>getInternal();
        removeMatching(holder -> holder.value().getFluid().getFluid() == target.getFluid());
    }
}
