package rustic.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.recipe.ActionAddRecipe;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import org.openzen.zencode.java.ZenCodeGlobals;
import org.openzen.zencode.java.ZenCodeType;
import rustic.content.crafting.EvaporatingBasinRecipe;
import rustic.registry.RusticRegistries;

@ZenRegister
@ZenCodeType.Name("mods.rustic.EvaporatingBasinRecipeManager")
public class EvaporatingBasinRecipeManager implements IRecipeManager<EvaporatingBasinRecipe> {

    @ZenCodeGlobals.Global("evaporatingBasin")
    public static final EvaporatingBasinRecipeManager INSTANCE = new EvaporatingBasinRecipeManager();

    private EvaporatingBasinRecipeManager() {
    }

    @Override
    public RecipeType<EvaporatingBasinRecipe> getRecipeType() {
        return RusticRegistries.EVAPORATING_BASIN_TYPE.get();
    }

    @ZenCodeType.Method
    public void add(String name, IItemStack output, IFluidStack input) {
        EvaporatingBasinRecipe recipe = new EvaporatingBasinRecipe(
                input.<FluidStack>getInternal(),
                output.getInternal());
        CraftTweakerAPI.apply(new ActionAddRecipe<>(this, createHolder(fixRecipeId(name), recipe)));
    }

    @ZenCodeType.Method
    public void removeByInputFluid(IFluidStack input) {
        FluidStack target = input.<FluidStack>getInternal();
        removeMatching(holder -> holder.value().getInput().getFluid() == target.getFluid());
    }
}
