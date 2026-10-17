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

/**
 * CraftTweaker 1.21.1 manager for {@link EvaporatingBasinRecipe}, the port of the 1.12.2
 * {@code mods.rustic.EvaporatingBasin} ZenScript class. Same auto-discovery / presence-gate model as
 * {@link CrushingTubRecipeManager}: published to ZenScript as the global {@code evaporatingBasin}.
 *
 * <p>The legacy class kept a separate evaporation {@code time} (its 3-arg {@code addRecipe} and
 * {@code CrTEvaporatingBasinRecipe}). The 1.21.1 port derives the countdown from the input fluid amount
 * ({@link EvaporatingBasinRecipe#getTime()} returns {@code input.getAmount()}), so packmakers set duration by sizing
 * the input fluid - there is no independent time parameter to expose.
 */
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

    /**
     * Add a basin recipe: the input fluid evaporates into the output dust item (duration = fluid amount).
     * Mirrors legacy {@code EvaporatingBasin.addRecipe(IItemStack output, ILiquidStack input)}.
     *
     * @docParam name "iron_dust"
     * @docParam output <item:rustic:tiny_iron_dust>
     * @docParam input <fluid:rustic:ironberryjuice> * 200
     */
    @ZenCodeType.Method
    public void add(String name, IItemStack output, IFluidStack input) {
        EvaporatingBasinRecipe recipe = new EvaporatingBasinRecipe(
                input.<FluidStack>getInternal(),
                output.getInternal());
        CraftTweakerAPI.apply(new ActionAddRecipe<>(this, createHolder(fixRecipeId(name), recipe)));
    }

    /**
     * Remove every basin recipe keyed on the given input fluid (fluid identity, amount ignored) - the port of legacy
     * {@code EvaporatingBasin.removeRecipe(ILiquidStack input)}. Removal by the item output is already covered by the
     * inherited {@code remove(IIngredient output)} / {@code removeByName}.
     *
     * @docParam input <fluid:rustic:ironberryjuice>
     */
    @ZenCodeType.Method
    public void removeByInputFluid(IFluidStack input) {
        FluidStack target = input.<FluidStack>getInternal();
        removeMatching(holder -> holder.value().getInput().getFluid() == target.getFluid());
    }
}
