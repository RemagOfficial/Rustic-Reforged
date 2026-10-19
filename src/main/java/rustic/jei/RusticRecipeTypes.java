package rustic.jei;

import java.util.function.Supplier;

import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import rustic.Rustic;
import rustic.content.crafting.BrewingBarrelRecipe;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.crafting.CrushingTubRecipe;
import rustic.content.crafting.EvaporatingBasinRecipe;
import rustic.content.crafting.RecipeNonIngredientReturn;
import rustic.content.crafting.RecipeOliveOil;
import rustic.content.crafting.RecipeVantaOil;
import rustic.registry.RusticRegistries;

public final class RusticRecipeTypes {

    public static final Supplier<RecipeType<RecipeHolder<CrushingTubRecipe>>> CRUSHING_TUB =
            RecipeType.createFromDeferredVanilla(RusticRegistries.CRUSHING_TUB_TYPE);
    public static final Supplier<RecipeType<RecipeHolder<EvaporatingBasinRecipe>>> EVAPORATING_BASIN =
            RecipeType.createFromDeferredVanilla(RusticRegistries.EVAPORATING_BASIN_TYPE);
    public static final Supplier<RecipeType<RecipeHolder<BrewingBarrelRecipe>>> BREWING_BARREL =
            RecipeType.createFromDeferredVanilla(RusticRegistries.BREWING_BARREL_TYPE);

    public static final RecipeType<RecipeHolder<CondenserRecipe>> CONDENSER_SIMPLE =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "condenser_simple"));
    public static final RecipeType<RecipeHolder<CondenserRecipe>> CONDENSER_ADVANCED =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "condenser_advanced"));

    public static final RecipeType<RecipeHolder<RecipeNonIngredientReturn>> NON_INGREDIENT_RETURN =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "non_ingredient_return"));
    public static final RecipeType<RecipeHolder<RecipeOliveOil>> OLIVE_OIL =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "olive_oiling"));
    public static final RecipeType<RecipeHolder<RecipeVantaOil>> VANTA_OIL =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "vanta_oiling"));

    private RusticRecipeTypes() {
    }
}
