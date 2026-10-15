package rustic.content.blocks;

import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

import java.util.Collection;

/**
 * The tile-entity (machine) blocks, kept in their own register rather than folded into
 * {@link RusticBlocks}.
 *
 * <p>{@link RusticRegistries} states the rule this follows: a content class that gains blocks keeps
 * its own {@link DeferredRegister}, and several registers may point at one registry. Separating the
 * machines from the decor is not cosmetic - the machines need a {@code BlockEntityType}, a
 * {@code MenuType} and (later) recipe tables hung off them, none of which the pure decor blocks have,
 * and it is that second set of registrations the machine phase is really about.
 *
 * <p>The consequence, and the reason both {@code RusticItems}' item loop and {@code RusticBlockLoot}'s
 * {@code knownBlocks()} read this register as well as {@link RusticBlocks#entries()}: every machine is
 * still a block that needs a block item and a drop-myself loot table, exactly like the decor. Nothing
 * about the shared item/loot pipeline changes because the block lives in a different register.
 */
public final class RusticMachineBlocks {

    private static final DeferredRegister.Blocks MACHINE_BLOCKS = DeferredRegister.createBlocks(Rustic.MODID);

    // ------------------------------------------------------------------
    // Storage. 1.12.2: BlockBarrel, Material.WOOD, hardness 1.5, no harvest level, a 27-slot tile
    // entity behind it. The wooden barrel is registered here; the liquid barrel (a fluid tank) and the
    // brewing barrel arrive with the fluid phase.
    // ------------------------------------------------------------------
    public static final DeferredBlock<BarrelBlock> BARREL =
            MACHINE_BLOCKS.registerBlock("barrel", BarrelBlock::new, RusticBlockProperties.wood(1.5F));

    // 1.12.2: BlockLiquidBarrel, Material.WOOD, hardness 1.5 - a fluid tank (16 buckets), no GUI. Its item and loot
    // ride the same entries() loops as every other machine block.
    public static final DeferredBlock<LiquidBarrelBlock> LIQUID_BARREL =
            MACHINE_BLOCKS.registerBlock("liquid_barrel", LiquidBarrelBlock::new, RusticBlockProperties.wood(1.5F));

    // 1.12.2: BlockCabinet, Material.WOOD, hardness 2.5, no harvest level, a 27-slot tile entity that merges
    // vertically into a 54-slot double. The animated doors are Phase 9; the block and its double are here.
    public static final DeferredBlock<CabinetBlock> CABINET =
            MACHINE_BLOCKS.registerBlock("cabinet", CabinetBlock::new, RusticBlockProperties.wood(2.5F));

    // 1.12.2: BlockVase, Material.ROCK, hardness 0.5, no harvest level, a 27-slot tile entity behind an inset
    // bounding box. It carries a scaffolded VARIANT(0-12) property; the thirteen real models and the
    // variant-selection packet are deferred to the art (Phase 9) and recipe (Phase 7) phases.
    public static final DeferredBlock<VaseBlock> VASE =
            MACHINE_BLOCKS.registerBlock("vase", VaseBlock::new, RusticBlockProperties.stone(0.5F));

    // ------------------------------------------------------------------
    // Processing machines. 1.12.2: BlockCrushingTub, Material.WOOD, hardness 1.5 - a one-slot input over a
    // drain-only fluid tank, no GUI.
    // ------------------------------------------------------------------
    public static final DeferredBlock<CrushingTubBlock> CRUSHING_TUB =
            MACHINE_BLOCKS.registerBlock("crushing_tub", CrushingTubBlock::new, RusticBlockProperties.wood(1.5F));

    // 1.12.2: BlockEvaporatingBasin, Material.ROCK, hardness 1.5 - a fluid-in / dust-out basin, no GUI.
    public static final DeferredBlock<EvaporatingBasinBlock> EVAPORATING_BASIN =
            MACHINE_BLOCKS.registerBlock("evaporating_basin", EvaporatingBasinBlock::new, RusticBlockProperties.stone(1.5F));

    // 1.12.2: BlockBrewingBarrel, Material.WOOD, hardness 1.5 - the fermenting barrel with the bespoke GUI.
    public static final DeferredBlock<BrewingBarrelBlock> BREWING_BARREL =
            MACHINE_BLOCKS.registerBlock("brewing_barrel", BrewingBarrelBlock::new, RusticBlockProperties.wood(1.5F));

    // 1.12.2: BlockCondenser, Material.ROCK, hardness 2 - the elixir still. Rebuilt as a single functional block
    // (no retort flank) behind CondenserBlockEntity; its item and loot ride the same entries() loops as every machine.
    public static final DeferredBlock<CondenserBlock> CONDENSER =
            MACHINE_BLOCKS.registerBlock("condenser", CondenserBlock::new, RusticBlockProperties.stone(2.0F));

    // 1.12.2: BlockCondenserAdvanced, Material.ROCK, hardness 2 - the two-block advanced still (bottom owns the
    // inventory, top forwards) behind CondenserAdvancedBlockEntity, with the modifier slot for advanced recipes.
    public static final DeferredBlock<CondenserAdvancedBlock> CONDENSER_ADVANCED =
            MACHINE_BLOCKS.registerBlock("condenser_advanced", CondenserAdvancedBlock::new, RusticBlockProperties.stone(2.0F));

    /**
     * All machine blocks, in registration order. Consumed by the same item/loot loops that walk
     * {@link RusticBlocks#entries()} so a machine cannot be added here and forget its item or drop.
     */
    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return MACHINE_BLOCKS.getEntries();
    }

    /** Attaches the machine-block register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        MACHINE_BLOCKS.register(modEventBus);
    }

    private RusticMachineBlocks() {
    }
}
