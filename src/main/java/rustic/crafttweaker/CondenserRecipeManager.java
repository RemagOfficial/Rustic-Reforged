package rustic.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.recipe.ActionAddRecipe;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import org.openzen.zencode.java.ZenCodeGlobals;
import org.openzen.zencode.java.ZenCodeType;
import rustic.content.crafting.CondenserRecipe;
import rustic.registry.RusticRegistries;

import java.util.Arrays;
import java.util.List;

/**
 * CraftTweaker 1.21.1 manager for {@link CondenserRecipe}, the port of the 1.12.2 {@code mods.rustic.Condenser}
 * ZenScript class and its {@code CrTCondenserRecipe} adapter. Published as the global {@code condenser}.
 *
 * <p>1.12.2 collapsed the basic/advanced condenser into one data-driven type ({@code CondenserRecipe.advanced}), with
 * the modifier herb slot as the discriminator: a basic recipe carries an {@link Ingredient#EMPTY} modifier and an
 * advanced one a real herb. The legacy class offered six {@code addRecipe} overloads that filled defaults
 * (glass bottle, {@code 125 mB} water, {@code 400} ticks); here those three become explicit required arguments so
 * nothing is silently guessed. Basic vs advanced is exposed as two distinct methods ({@link #add} and
 * {@link #addAdvanced}) rather than one trailing {@code @ZenCodeType.Optional} modifier: CT's generic {@code Optional}
 * only omits an argument when the parameter's type carries an OpenZen default value, which the native {@code IIngredient}
 * type does not, and ZenCode has no same-name overloading - the same reason CT's own managers use distinct names
 * ({@code addShaped}/{@code addShapeless}/{@code addShapedMirrored}).
 */
@ZenRegister
@ZenCodeType.Name("mods.rustic.CondenserRecipeManager")
public class CondenserRecipeManager implements IRecipeManager<CondenserRecipe> {

    @ZenCodeGlobals.Global("condenser")
    public static final CondenserRecipeManager INSTANCE = new CondenserRecipeManager();

    private CondenserRecipeManager() {
    }

    @Override
    public RecipeType<CondenserRecipe> getRecipeType() {
        return RusticRegistries.CONDENSER_TYPE.get();
    }

    /**
     * Add a <b>basic</b> condenser recipe (no modifier herb). The condenser consumes {@code bottle} plus {@code water}
     * and the exact multiset of {@code inputs} to brew {@code result} after {@code time} ticks.
     *
     * @docParam name "elixir_healing"
     * @docParam result <item:rustic:elixir>
     * @docParam inputs [<item:minecraft:red_mushroom>, <item:minecraft:sugar>]
     * @docParam bottle <item:minecraft:glass_bottle>
     * @docParam water <fluid:minecraft:water> * 125
     * @docParam time 400
     */
    @ZenCodeType.Method
    public void add(String name,
                    IItemStack result,
                    IIngredient[] inputs,
                    IItemStack bottle,
                    IFluidStack water,
                    int time) {
        addRecipe(name, result, inputs, bottle, water, time, null);
    }

    /**
     * Add an <b>advanced</b> condenser recipe: like {@link #add}, but the {@code modifier} herb slot must also match
     * for the recipe to apply.
     *
     * @docParam name "elixir_haste"
     * @docParam result <item:rustic:elixir>
     * @docParam inputs [<item:minecraft:red_mushroom>, <item:minecraft:sugar>]
     * @docParam bottle <item:minecraft:glass_bottle>
     * @docParam water <fluid:minecraft:water> * 125
     * @docParam time 400
     * @docParam modifier <item:rustic:moonlight_herb>
     */
    @ZenCodeType.Method
    public void addAdvanced(String name,
                            IItemStack result,
                            IIngredient[] inputs,
                            IItemStack bottle,
                            IFluidStack water,
                            int time,
                            IIngredient modifier) {
        addRecipe(name, result, inputs, bottle, water, time, modifier);
    }

    /** Shared body for {@link #add} ({@code modifier == null}) and {@link #addAdvanced}. */
    private void addRecipe(String name,
                           IItemStack result,
                           IIngredient[] inputs,
                           IItemStack bottle,
                           IFluidStack water,
                           int time,
                           IIngredient modifier) {
        boolean advanced = modifier != null && !modifier.isEmpty();
        Ingredient modIngredient = advanced ? modifier.asVanillaIngredient() : Ingredient.EMPTY;
        List<Ingredient> ingredientList = Arrays.stream(inputs).map(IIngredient::asVanillaIngredient).toList();
        CondenserRecipe recipe = new CondenserRecipe(
                advanced,
                modIngredient,
                ingredientList,
                water.<FluidStack>getInternal(),
                bottle.getInternal(),
                result.getInternal(),
                time);
        CraftTweakerAPI.apply(new ActionAddRecipe<>(this, createHolder(fixRecipeId(name), recipe)));
    }

    /**
     * Remove every condenser recipe that produces an item of the same type as {@code output} (component-insensitive,
     * matching the legacy {@code Condenser.removeRecipe(IItemStack output)} behaviour of keying on the result item).
     *
     * @docParam output <item:rustic:elixir>
     */
    @ZenCodeType.Method
    public void removeByOutput(IItemStack output) {
        ItemStack target = output.getInternal();
        removeMatching(holder -> ItemStack.isSameItem(holder.value().getResultItem(null), target));
    }
}
