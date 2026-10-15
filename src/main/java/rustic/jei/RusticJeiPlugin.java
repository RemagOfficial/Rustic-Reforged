package rustic.jei;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import rustic.Rustic;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.crafting.BrewingBarrelRecipe;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.crafting.CrushingTubRecipe;
import rustic.content.crafting.EvaporatingBasinRecipe;
import rustic.content.crafting.RecipeNonIngredientReturn;
import rustic.content.crafting.RecipeOliveOil;
import rustic.content.crafting.RecipeVantaOil;
import rustic.jei.category.BrewingBarrelRecipeCategory;
import rustic.jei.category.CondenserRecipeCategory;
import rustic.jei.category.CrushingTubRecipeCategory;
import rustic.jei.category.EvaporatingBasinRecipeCategory;
import rustic.jei.category.NonIngredientReturnRecipeCategory;
import rustic.jei.category.OliveOilRecipeCategory;
import rustic.jei.category.VantaOilRecipeCategory;
import rustic.registry.RusticRegistries;

/**
 * The 1.21.1 port of 1.12.2's {@code rustic.compat.jei.RusticJEIPlugin}. It surfaces every custom Rustic recipe in
 * JEI so the ported machines and crafting-table recipes can be browsed in-game without crafting each one.
 *
 * <p>Discovery is automatic: modern JEI scans the mod jar for {@link JeiPlugin}-annotated {@link IModPlugin}
 * implementations with a no-arg constructor (see {@code ForgePluginFinder}), so no service file or mod-bus wiring is
 * needed - the annotation is the whole hook. JEI only ever runs on the client, and every method it calls here
 * ({@link #registerCategories}, {@link #registerRecipes}, {@link #registerRecipeCatalysts}) is client-only, which is
 * why this class is allowed to touch {@code Minecraft.getInstance()}.
 *
 * <p>The legacy plugin mixed three concerns into one {@code register(IModRegistry)} call; the 1.21 API splits them
 * into the phase callbacks below, and the old string uids ({@code "rustic.crushing_tub"} etc) become the typed
 * {@link RusticRecipeTypes} handles. The {@code addRecipeCategoryCraftingItem} calls become
 * {@link #registerRecipeCatalysts}, and the {@code addRecipeClickArea} GUI hooks are dropped - the machine GUIs are
 * still Phase-11 placeholders and JEI's recipe-transfer/click areas can be re-added once the real screens exist.
 */
@JeiPlugin
public class RusticJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "rustic");

    // Resolved once in registerCategories, then reused by registerRecipes / registerRecipeCatalysts, which JEI
    // guarantees are called after it. Held as fields because the deferred vanilla types are memoised suppliers.
    private RecipeType<RecipeHolder<CrushingTubRecipe>> crushingTubType;
    private RecipeType<RecipeHolder<EvaporatingBasinRecipe>> evaporatingBasinType;
    private RecipeType<RecipeHolder<BrewingBarrelRecipe>> brewingBarrelType;
    private RecipeType<RecipeHolder<CondenserRecipe>> condenserSimpleType;
    private RecipeType<RecipeHolder<CondenserRecipe>> condenserAdvancedType;
    private RecipeType<RecipeHolder<RecipeNonIngredientReturn>> nonIngredientReturnType;
    private RecipeType<RecipeHolder<RecipeOliveOil>> oliveOilType;
    private RecipeType<RecipeHolder<RecipeVantaOil>> vantaOilType;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        this.crushingTubType = RusticRecipeTypes.CRUSHING_TUB.get();
        this.evaporatingBasinType = RusticRecipeTypes.EVAPORATING_BASIN.get();
        this.brewingBarrelType = RusticRecipeTypes.BREWING_BARREL.get();
        this.condenserSimpleType = RusticRecipeTypes.CONDENSER_SIMPLE;
        this.condenserAdvancedType = RusticRecipeTypes.CONDENSER_ADVANCED;
        this.nonIngredientReturnType = RusticRecipeTypes.NON_INGREDIENT_RETURN;
        this.oliveOilType = RusticRecipeTypes.OLIVE_OIL;
        this.vantaOilType = RusticRecipeTypes.VANTA_OIL;

        registration.addRecipeCategories(
                new CrushingTubRecipeCategory(guiHelper, crushingTubType),
                new EvaporatingBasinRecipeCategory(guiHelper, evaporatingBasinType),
                new BrewingBarrelRecipeCategory(guiHelper, brewingBarrelType),
                new CondenserRecipeCategory(guiHelper, condenserSimpleType, false),
                new CondenserRecipeCategory(guiHelper, condenserAdvancedType, true),
                new NonIngredientReturnRecipeCategory(guiHelper, nonIngredientReturnType),
                new OliveOilRecipeCategory(guiHelper, oliveOilType),
                new VantaOilRecipeCategory(guiHelper, vantaOilType));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();

        registration.addRecipes(crushingTubType, manager.getAllRecipesFor(RusticRegistries.CRUSHING_TUB_TYPE.get()));
        registration.addRecipes(evaporatingBasinType, manager.getAllRecipesFor(RusticRegistries.EVAPORATING_BASIN_TYPE.get()));
        registration.addRecipes(brewingBarrelType, manager.getAllRecipesFor(RusticRegistries.BREWING_BARREL_TYPE.get()));

        // The single vanilla condenser type is split across the basic/advanced JEI categories on the recipe flag.
        List<RecipeHolder<CondenserRecipe>> condenserRecipes = manager.getAllRecipesFor(RusticRegistries.CONDENSER_TYPE.get());
        registration.addRecipes(condenserSimpleType,
                condenserRecipes.stream().filter(holder -> !holder.value().isAdvanced()).toList());
        registration.addRecipes(condenserAdvancedType,
                condenserRecipes.stream().filter(holder -> holder.value().isAdvanced()).toList());

        // Crafting-table recipes that are not plain shaped/shapeless JSON. They craft in the vanilla table, so they
        // live under the shared RecipeType.CRAFTING; each JEI category is filled by filtering those holders by recipe
        // class. The non-ingredient-return rows are data-driven, the two oiling rows are single stub holders whose
        // category renders a representative example. The cabinet is absent by design - as a minecraft:crafting_shaped
        // recipe JEI's built-in Crafting category already shows it.
        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> crafting =
                manager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
        registration.addRecipes(nonIngredientReturnType, filterCrafting(crafting, RecipeNonIngredientReturn.class));
        registration.addRecipes(oliveOilType, filterCrafting(crafting, RecipeOliveOil.class));
        registration.addRecipes(vantaOilType, filterCrafting(crafting, RecipeVantaOil.class));
    }

    /** Pull the holders of one concrete crafting-recipe class out of the shared {@code RecipeType.CRAFTING} list. */
    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.item.crafting.CraftingRecipe> List<RecipeHolder<T>> filterCrafting(
            List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> holders, Class<T> type) {
        return holders.stream()
                .filter(holder -> type.isInstance(holder.value()))
                .map(holder -> (RecipeHolder<T>) holder)
                .toList();
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(RusticMachineBlocks.CRUSHING_TUB.get().asItem(), crushingTubType);
        registration.addRecipeCatalyst(RusticMachineBlocks.EVAPORATING_BASIN.get().asItem(), evaporatingBasinType);
        registration.addRecipeCatalyst(RusticMachineBlocks.BREWING_BARREL.get().asItem(), brewingBarrelType);
        registration.addRecipeCatalyst(RusticMachineBlocks.CONDENSER.get().asItem(), condenserSimpleType);
        registration.addRecipeCatalyst(RusticMachineBlocks.CONDENSER_ADVANCED.get().asItem(), condenserAdvancedType);
    }
}
