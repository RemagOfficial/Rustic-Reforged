package rustic.registry;

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
import rustic.content.crafting.BrewingBarrelRecipe;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.crafting.CrushingTubRecipe;
import rustic.content.crafting.EvaporatingBasinRecipe;

/**
 * The registry handles every content class registers into. Phase 1 only wires the plumbing up;
 * Phase 2 fills these with the actual blocks, items, fluids, ... that 1.12.2 built by hand in
 * {@code ModBlocks.init()} / {@code ModItems.init()} / {@code ModFluids.init()} and friends.
 *
 * <p>The 1.12.2 pattern was: construct an object, assign it to a public static field, then call a
 * static {@code init()} that did {@code registry.register(...)}. Here a content class declares
 * {@code DeferredRegister} entries in its own static initialiser and everything is registered when
 * NeoForge fires the registry event, which removes the init-ordering constraints completely.
 *
 * <p>Blocks and items are the exception: they are owned by the content classes that fill them
 * ({@link rustic.content.blocks.RusticBlocks} and {@link rustic.content.items.RusticItems}) rather
 * than by a handle here, because {@code DeferredRegister.createBlocks}/{@code createItems} give
 * {@code DeferredBlock}/{@code DeferredItem} holders with the block-item helpers on them. Several
 * registers may point at one registry, so a content class that gains blocks later (fluid blocks,
 * crops, machines) keeps its own. Entities follow the same rule: the chair seat and the tomato are
 * declared by {@link rustic.content.entities.RusticEntities}, which owns its own register.
 */
public final class RusticRegistries {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Rustic.MODID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Rustic.MODID);

    /** The seven potion effects (fullmetal, shame, iron skin, ...). */
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Rustic.MODID);

    /** Fluid types carry the render/steam/sound data that 1.12.2 kept on {@code Fluid} itself. */
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Rustic.MODID);

    /**
     * The sixteen Rustic fluids, still/flowing pairs included. The vanilla registry is typed
     * {@code Registry<Fluid>}, so the register is too and concrete entries use {@code FlowingFluid}.
     */
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Rustic.MODID);

    /** Custom recipe types: barrel, brewing barrel, condenser, crushing tub, evaporating basin, elixir. */
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Rustic.MODID);

    /** The machine {@link RecipeType}s. A {@code RecipeMap} keys recipes by type registry name, so these must be registered. */
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Rustic.MODID);

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

    /**
     * Attaches every {@link DeferredRegister} above to the mod event bus. Must be called from the
     * mod constructor, before the loader fires the registry events.
     */
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
    }

    private RusticRegistries() {
    }
}
