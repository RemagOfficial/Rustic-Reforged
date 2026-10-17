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

/**
 * CraftTweaker 1.21.1 manager for {@link CrushingTubRecipe}, the port of the 1.12.2 {@code mods.rustic.CrushingTub}
 * ZenScript class (its {@code CraftTweakerHelper}/{@code crafttweaker.IAction} pattern was removed in CT for 1.21).
 *
 * <p>Modern CraftTweaker exposes each custom recipe type as an {@link IRecipeManager} object bound to the recipe's
 * {@link RecipeType}. This class is {@link ZenRegister}-annotated, so CT picks it up through its runtime class scan
 * (the same auto-discovery JEI uses) and never loads it when CraftTweaker is absent - which is the whole presence
 * gate, so no {@code ModList.isLoaded} check is needed here. It is published to ZenScript as the global
 * {@code crushingTub}, and inherits the generic {@code removeAll}/{@code removeByName}/{@code removeByModid}/
 * {@code removeMatching}/{@code addJsonRecipe} operations from {@link IRecipeManager}.
 *
 * <p>The legacy class mutated an in-memory list ({@code Recipes.crushingTubRecipes}); here {@link #add} builds a real
 * {@link CrushingTubRecipe} and hands it to the vanilla recipe manager through {@link ActionAddRecipe}, so the crushing
 * tub block entity (which queries {@code getAllRecipesFor(CRUSHING_TUB_TYPE)}) sees it like any data-driven recipe.
 */
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

    /**
     * Add a crushing tub recipe with no byproduct: one input item crushes into the output fluid.
     * Mirrors legacy {@code CrushingTub.addRecipe(ILiquidStack output, IItemStack input)} (the byproduct-less form).
     *
     * <p>Exposed as two distinct methods rather than one {@code @ZenCodeType.Optional} byproduct because CT only omits
     * a trailing argument when its type carries an OpenZen default value, which {@code IItemStack} does not (see
     * {@link CondenserRecipeManager} for the full rationale).</p>
     *
     * @docParam name "apple_juice"
     * @docParam input <item:minecraft:apple>
     * @docParam output <fluid:rustic:applejuice> * 250
     */
    @ZenCodeType.Method
    public void add(String name, IItemStack input, IFluidStack output) {
        addRecipe(name, input, output, null);
    }

    /**
     * Add a crushing tub recipe that also drops a byproduct item: one input item crushes into the output fluid plus
     * {@code byproduct}. Mirrors legacy {@code CrushingTub.addRecipe(ILiquidStack output, IItemStack byproduct,
     * IItemStack input)}, reordered into the more natural input-first form.
     *
     * @docParam name "apple_juice"
     * @docParam input <item:minecraft:apple>
     * @docParam output <fluid:rustic:applejuice> * 250
     * @docParam byproduct <item:rustic:apple_seeds>
     */
    @ZenCodeType.Method
    public void addWithByproduct(String name, IItemStack input, IFluidStack output, IItemStack byproduct) {
        addRecipe(name, input, output, byproduct);
    }

    /** Shared body for {@link #add} ({@code byproduct == null}) and {@link #addWithByproduct}. */
    private void addRecipe(String name, IItemStack input, IFluidStack output, IItemStack byproduct) {
        ItemStack by = byproduct == null ? ItemStack.EMPTY : byproduct.getInternal();
        CrushingTubRecipe recipe = new CrushingTubRecipe(
                input.asVanillaIngredient(),
                output.<FluidStack>getInternal(),
                by);
        CraftTweakerAPI.apply(new ActionAddRecipe<>(this, createHolder(fixRecipeId(name), recipe)));
    }

    /**
     * Remove every crushing tub recipe whose output fluid matches {@code output} (fluid identity, amount ignored),
     * the port of legacy {@code CrushingTub.removeRecipe(ILiquidStack output)}. Uses a {@link IIngredient}-free field
     * predicate rather than the inherited {@code remove(IIngredient)} so it keys on the fluid (the machine's real
     * output) instead of the item byproduct.
     *
     * @docParam output <fluid:rustic:applejuice>
     */
    @ZenCodeType.Method
    public void removeByOutputFluid(IFluidStack output) {
        FluidStack target = output.<FluidStack>getInternal();
        removeMatching(holder -> holder.value().getFluid().getFluid() == target.getFluid());
    }
}
