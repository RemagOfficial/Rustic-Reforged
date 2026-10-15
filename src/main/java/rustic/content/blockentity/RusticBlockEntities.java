package rustic.content.blockentity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.content.blocks.RusticMachineBlocks;

/**
 * Block entity types, the 1.21 successor to 1.12.2's {@code GameRegistry.registerTileEntity} calls
 * scattered through {@code ModBlocks}.
 *
 * <p>The legacy mod registered one {@code TileEntity} class per block with a {@code DataFixer}-less
 * {@code TileEntitySpecialRenderer} hookup; 1.21 keys tile entities on a {@link BlockEntityType} built
 * from the constructor and the set of valid blocks. Each entry below follows vanilla's own
 * {@link BlockEntityType.Builder} pattern, and the type object is what a block entity hands to its
 * {@code super(...)} - which is why {@link BarrelBlockEntity} refers to {@link #BARREL} rather than
 * hard-coding a vanilla type.
 *
 * <p>The item-handler capability that {@link #registerCapabilities} exposes replaces the legacy
 * {@code TileEntityCabinet.getCapability}/{@code TileEntityBarrel.getCapability} overrides. Those
 * returned an {@code IItemHandler} to hoppers and other mods through Forge's old {@code @ICapabilityProvider}
 * plumbing; NeoForge asks the owning mod to register a provider per block entity type instead, and the
 * storage blocks in this slice back it with {@link InvWrapper} over their own {@code Container} half.
 */
public final class RusticBlockEntities {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Rustic.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarrelBlockEntity>> BARREL =
            BLOCK_ENTITIES.register("barrel", () -> BlockEntityType.Builder
                    .of(BarrelBlockEntity::new, RusticMachineBlocks.BARREL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidBarrelBlockEntity>> LIQUID_BARREL =
            BLOCK_ENTITIES.register("liquid_barrel", () -> BlockEntityType.Builder
                    .of(LiquidBarrelBlockEntity::new, RusticMachineBlocks.LIQUID_BARREL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CabinetBlockEntity>> CABINET =
            BLOCK_ENTITIES.register("cabinet", () -> BlockEntityType.Builder
                    .of(CabinetBlockEntity::new, RusticMachineBlocks.CABINET.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VaseBlockEntity>> VASE =
            BLOCK_ENTITIES.register("vase", () -> BlockEntityType.Builder
                    .of(VaseBlockEntity::new, RusticMachineBlocks.VASE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrushingTubBlockEntity>> CRUSHING_TUB =
            BLOCK_ENTITIES.register("crushing_tub", () -> BlockEntityType.Builder
                    .of(CrushingTubBlockEntity::new, RusticMachineBlocks.CRUSHING_TUB.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EvaporatingBasinBlockEntity>> EVAPORATING_BASIN =
            BLOCK_ENTITIES.register("evaporating_basin", () -> BlockEntityType.Builder
                    .of(EvaporatingBasinBlockEntity::new, RusticMachineBlocks.EVAPORATING_BASIN.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrewingBarrelBlockEntity>> BREWING_BARREL =
            BLOCK_ENTITIES.register("brewing_barrel", () -> BlockEntityType.Builder
                    .of(BrewingBarrelBlockEntity::new, RusticMachineBlocks.BREWING_BARREL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondenserBlockEntity>> CONDENSER =
            BLOCK_ENTITIES.register("condenser", () -> BlockEntityType.Builder
                    .of(CondenserBlockEntity::new, RusticMachineBlocks.CONDENSER.get())
                    .build(null));

    // The advanced condenser's type is bound only to the lower half; the decoration-only upper half has no block
    // entity (CondenserAdvancedBlock.newBlockEntity returns null for top=true) and forwards interaction down.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondenserAdvancedBlockEntity>> CONDENSER_ADVANCED =
            BLOCK_ENTITIES.register("condenser_advanced", () -> BlockEntityType.Builder
                    .of(CondenserAdvancedBlockEntity::new, RusticMachineBlocks.CONDENSER_ADVANCED.get())
                    .build(null));

    /**
     * Exposes the storage tile entities' inventories to automation. Kept in one place so a later machine
     * adds a type here and is guaranteed to be reachable by hoppers, matching the legacy mod where every
     * inventory-bearing tile entity overrode {@code getCapability}.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BARREL.get(),
                (barrel, side) -> new InvWrapper(barrel));
        // The liquid barrel exposes only its fluid tank to automation/pipes; it has no item inventory.
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                LIQUID_BARREL.get(),
                (liquidBarrel, side) -> liquidBarrel.getTank());
        // Each cabinet half exposes its own 27 slots to automation. The legacy combined DoubleCabinetItemHandler
        // (a hopper under a double pulling all 54) is deferred alongside the door renderer; the merged 54-slot
        // view a player sees is built on demand by CabinetBlockEntity.createMenu over a CompoundContainer.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CABINET.get(),
                (cabinet, side) -> new InvWrapper(cabinet));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                VASE.get(),
                (vase, side) -> new InvWrapper(vase));
        // The crushing tub exposes its input slot to item automation and its drain-only tank to fluid automation.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CRUSHING_TUB.get(),
                (tub, side) -> tub.getItemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CRUSHING_TUB.get(),
                (tub, side) -> tub.getDrainView());
        // The basin exposes its dust output slot and its (pour-in / take-out) tank to automation.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                EVAPORATING_BASIN.get(),
                (basin, side) -> basin.getItemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                EVAPORATING_BASIN.get(),
                (basin, side) -> basin.getTank());
        // The brewing barrel exposes its six slots to item automation (legacy exposed only the item handler; the
        // fluid tanks are filled/drawn through the GUI's slot IO, so no block fluid capability is published here).
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BREWING_BARREL.get(),
                (barrel, side) -> barrel.getItemHandler());
        // The condensers expose their full item inventory (result/fuel/bottle/ingredients[/modifier]) to automation and
        // their water tank - fillable and drainable, unlike the crushing tub's drain-only tank - to fluid automation.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CONDENSER.get(),
                (condenser, side) -> condenser.getItemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CONDENSER.get(),
                (condenser, side) -> condenser.getTank());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CONDENSER_ADVANCED.get(),
                (condenser, side) -> condenser.getItemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CONDENSER_ADVANCED.get(),
                (condenser, side) -> condenser.getTank());
    }

    /** Attaches the block entity register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    private RusticBlockEntities() {
    }
}
