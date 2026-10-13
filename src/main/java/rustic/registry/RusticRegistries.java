package rustic.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import rustic.Rustic;

/**
 * The registry handles every content class registers into. Phase 1 only wires the plumbing up;
 * Phase 2 fills these with the actual blocks, items, fluids, ... that 1.12.2 built by hand in
 * {@code ModBlocks.init()} / {@code ModItems.init()} / {@code ModFluids.init()} and friends.
 *
 * <p>The 1.12.2 pattern was: construct an object, assign it to a public static field, then call a
 * static {@code init()} that did {@code registry.register(...)}. Here a content class declares
 * {@code DeferredRegister} entries in its own static initialiser and everything is registered when
 * NeoForge fires the registry event, which removes the init-ordering constraints completely.
 */
public final class RusticRegistries {

    /** Blocks, registered under {@code rustic:<name>}. */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Rustic.MODID);

    /** Items, including the block items that 1.12.2 registered next to each block. */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Rustic.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Rustic.MODID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Rustic.MODID);

    /** The throwable tomato. */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Rustic.MODID);

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
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Rustic.MODID);

    /**
     * Attaches every {@link DeferredRegister} above to the mod event bus. Must be called from the
     * mod constructor, before the loader fires the registry events.
     */
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }

    private RusticRegistries() {
    }
}
