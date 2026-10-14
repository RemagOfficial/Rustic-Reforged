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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CabinetBlockEntity>> CABINET =
            BLOCK_ENTITIES.register("cabinet", () -> BlockEntityType.Builder
                    .of(CabinetBlockEntity::new, RusticMachineBlocks.CABINET.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VaseBlockEntity>> VASE =
            BLOCK_ENTITIES.register("vase", () -> BlockEntityType.Builder
                    .of(VaseBlockEntity::new, RusticMachineBlocks.VASE.get())
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
    }

    /** Attaches the block entity register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    private RusticBlockEntities() {
    }
}
