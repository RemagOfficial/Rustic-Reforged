package rustic.registry;

import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import rustic.Rustic;
import rustic.content.advancements.AlcoholItemPredicate;
import rustic.content.crafting.BrewingBarrelRecipe;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.crafting.CrushingTubRecipe;
import rustic.content.crafting.EvaporatingBasinRecipe;
import rustic.content.crafting.RecipeNonIngredientReturn;
import rustic.content.crafting.RecipeOliveOil;
import rustic.content.crafting.RecipeVantaOil;

public final class RusticRegistries {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Rustic.MODID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Rustic.MODID);

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Rustic.MODID);

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Rustic.MODID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Rustic.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Rustic.MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Rustic.MODID);

    public static final DeferredRegister<ItemSubPredicate.Type<?>> ITEM_SUB_PREDICATES =
            DeferredRegister.create(Registries.ITEM_SUB_PREDICATE_TYPE, Rustic.MODID);

    public static final DeferredHolder<ItemSubPredicate.Type<?>, ItemSubPredicate.Type<AlcoholItemPredicate>> ALCOHOL_PREDICATE =
            ITEM_SUB_PREDICATES.register("alcohol", () -> AlcoholItemPredicate.TYPE);

    private static ResourceLocation machineRecipe(String name) {
        return ResourceLocation.fromNamespaceAndPath(Rustic.MODID, name);
    }

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushingTubRecipe>> CRUSHING_TUB_TYPE =
            RECIPE_TYPES.register("crushing_tub", () -> RecipeType.simple(machineRecipe("crushing_tub")));
    public static final DeferredHolder<RecipeType<?>, RecipeType<EvaporatingBasinRecipe>> EVAPORATING_BASIN_TYPE =
            RECIPE_TYPES.register("evaporating_basin", () -> RecipeType.simple(machineRecipe("evaporating_basin")));
    public static final DeferredHolder<RecipeType<?>, RecipeType<BrewingBarrelRecipe>> BREWING_BARREL_TYPE =
            RECIPE_TYPES.register("brewing_barrel", () -> RecipeType.simple(machineRecipe("brewing_barrel")));
    public static final DeferredHolder<RecipeType<?>, RecipeType<CondenserRecipe>> CONDENSER_TYPE =
            RECIPE_TYPES.register("condenser", () -> RecipeType.simple(machineRecipe("condenser")));

    public static final DeferredHolder<RecipeSerializer<?>, CrushingTubRecipe.Serializer> CRUSHING_TUB_SERIALIZER =
            RECIPE_SERIALIZERS.register("crushing_tub", CrushingTubRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, EvaporatingBasinRecipe.Serializer> EVAPORATING_BASIN_SERIALIZER =
            RECIPE_SERIALIZERS.register("evaporating_basin", EvaporatingBasinRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, BrewingBarrelRecipe.Serializer> BREWING_BARREL_SERIALIZER =
            RECIPE_SERIALIZERS.register("brewing_barrel", BrewingBarrelRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, CondenserRecipe.Serializer> CONDENSER_SERIALIZER =
            RECIPE_SERIALIZERS.register("condenser", CondenserRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeNonIngredientReturn.Serializer> NON_INGREDIENT_RETURN_SERIALIZER =
            RECIPE_SERIALIZERS.register("non_ingredient_return", RecipeNonIngredientReturn.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeOliveOil.Serializer> OLIVE_OIL_SERIALIZER =
            RECIPE_SERIALIZERS.register("olive_oil", RecipeOliveOil.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeVantaOil.Serializer> VANTA_OIL_SERIALIZER =
            RECIPE_SERIALIZERS.register("vanta_oil", RecipeVantaOil.Serializer::new);

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        ITEM_SUB_PREDICATES.register(modEventBus);
    }

    private RusticRegistries() {
    }
}
