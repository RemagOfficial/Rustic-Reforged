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

/**
 * The JEI {@link RecipeType} handles for every Rustic recipe the plugin exposes. Modern JEI identifies a category by
 * its own {@link RecipeType} (not the vanilla one), so this is the single place that bridges the mod's
 * {@link RusticRegistries} deferred vanilla types onto JEI's recipe-type id space.
 *
 * <p>Every entry is a {@code RecipeHolder}-based type, exactly like JEI's built-in vanilla categories, so the plugin
 * hands JEI the {@link RecipeHolder}s it pulls from the {@code RecipeManager} and the categories read the recipe out
 * with {@link RecipeHolder#value()}. The vanilla-backed types use {@link RecipeType#createFromDeferredVanilla} - a
 * memoised supplier - so they are only resolved once the vanilla recipe types are actually registered (the plugin is
 * constructed after registry freeze, so calling {@code get()} there is safe). The two condenser categories share one
 * vanilla type but split by {@link CondenserRecipe#isAdvanced()}, so they get their own namespaced JEI uids rather
 * than being derived from the vanilla type.
 */
public final class RusticRecipeTypes {

    // ---- machine categories (one vanilla type each) ----
    public static final Supplier<RecipeType<RecipeHolder<CrushingTubRecipe>>> CRUSHING_TUB =
            RecipeType.createFromDeferredVanilla(RusticRegistries.CRUSHING_TUB_TYPE);
    public static final Supplier<RecipeType<RecipeHolder<EvaporatingBasinRecipe>>> EVAPORATING_BASIN =
            RecipeType.createFromDeferredVanilla(RusticRegistries.EVAPORATING_BASIN_TYPE);
    public static final Supplier<RecipeType<RecipeHolder<BrewingBarrelRecipe>>> BREWING_BARREL =
            RecipeType.createFromDeferredVanilla(RusticRegistries.BREWING_BARREL_TYPE);

    // ---- condenser: one vanilla type, two JEI categories (basic / advanced) ----
    public static final RecipeType<RecipeHolder<CondenserRecipe>> CONDENSER_SIMPLE =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "condenser_simple"));
    public static final RecipeType<RecipeHolder<CondenserRecipe>> CONDENSER_ADVANCED =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "condenser_advanced"));

    // ---- crafting-table categories ----
    // These three recipes craft in the vanilla table but cannot be plain shaped/shapeless JSON (dynamic result or
    // suppressed container remainder), so their vanilla type is RecipeType.CRAFTING (shared). Each gets its own
    // namespaced JEI display type - like the condenser split - and the plugin filters the CRAFTING holders by recipe
    // class to fill it. The cabinet is not here: it is a normal minecraft:crafting_shaped recipe shown by JEI's
    // built-in Crafting category.
    public static final RecipeType<RecipeHolder<RecipeNonIngredientReturn>> NON_INGREDIENT_RETURN =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "non_ingredient_return"));
    public static final RecipeType<RecipeHolder<RecipeOliveOil>> OLIVE_OIL =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "olive_oiling"));
    public static final RecipeType<RecipeHolder<RecipeVantaOil>> VANTA_OIL =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "vanta_oiling"));

    private RusticRecipeTypes() {
    }
}
