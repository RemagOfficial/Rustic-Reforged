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

    @ZenCodeType.Method
    public void add(String name,
                    IItemStack result,
                    IIngredient[] inputs,
                    IItemStack bottle,
                    IFluidStack water,
                    int time) {
        addRecipe(name, result, inputs, bottle, water, time, null);
    }

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

    @ZenCodeType.Method
    public void removeByOutput(IItemStack output) {
        ItemStack target = output.getInternal();
        removeMatching(holder -> ItemStack.isSameItem(holder.value().getResultItem(null), target));
    }
}
