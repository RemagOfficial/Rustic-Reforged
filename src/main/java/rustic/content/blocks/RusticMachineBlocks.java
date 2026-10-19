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
    // Same occlusion contract as crushing_tub/evaporating_basin (Phase 11 S11.1 in-game verdict): the restored
    // barrel model's footprint is [1,0,1]->[15,8,15] while its getShape is the full cube, so an opaque block culls
    // the neighbour DOWN face and shows as missing ground in a ring under the barrel. noOcclusion() reproduces
    // legacy isOpaqueCube=false.
    // ------------------------------------------------------------------
    public static final DeferredBlock<BarrelBlock> BARREL =
            MACHINE_BLOCKS.registerBlock("barrel", BarrelBlock::new, RusticBlockProperties.wood(1.5F).noOcclusion());

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
    // Faithful 1.12.2 contract: legacy BlockCrushingTub declared isOpaqueCube=false, so it never occluded a neighbour.
    // The tub's getShape is a full-footprint box (0..1 x/z, 0..0.5625 y), so left opaque its DOWN face culls the grass
    // top beneath it, exactly like the basin. The tub MODEL's base plate is [1,0,1]->[15,1,15], so the dropped grass
    // shows through as a thin ring of missing ground at the block edges/corners. noOcclusion() reproduces the legacy
    // non-opaque contract (its DOWN=SOLID getBlockFaceShape only ever governed connection/stand-on logic, not the
    // 1.21 neighbour face-cull, so it is not a reason to keep occlusion here).
    // ------------------------------------------------------------------
    public static final DeferredBlock<CrushingTubBlock> CRUSHING_TUB =
            MACHINE_BLOCKS.registerBlock("crushing_tub", CrushingTubBlock::new, RusticBlockProperties.wood(1.5F).noOcclusion());

    // 1.12.2: BlockEvaporatingBasin, Material.ROCK, hardness 1.5 - a fluid-in / dust-out basin, no GUI.
    // The legacy block declared isOpaqueCube=false and getBlockFaceShape=UNDEFINED on EVERY side, i.e. it never
    // occludes a neighbour. That must be reproduced here: the basin's getShape is a full-footprint shallow box
    // (0..1 x/z, 0..0.25 y), and 1.21's Block.shouldRenderFace derives the neighbour-cull test from getOcclusionShape
    // (which defaults to getShape). Left as a normal opaque block that full DOWN face exactly coincides with the
    // grass top beneath it, so the chunk builder drops the grass face - and because the basin MODEL's base plate only
    // covers the inner 10/16 footprint, the dropped face shows through the outer rim as a hole in the ground.
    // noOcclusion() sets canOcclude=false so shouldRenderFace short-circuits to "render", matching the legacy contract.
    public static final DeferredBlock<EvaporatingBasinBlock> EVAPORATING_BASIN =
            MACHINE_BLOCKS.registerBlock("evaporating_basin", EvaporatingBasinBlock::new, RusticBlockProperties.stone(1.5F).noOcclusion());

    // 1.12.2: BlockBrewingBarrel, Material.WOOD, hardness 1.5 - the fermenting barrel with the bespoke GUI.
    // noOcclusion() for the same reason as BARREL above: banded model inside a full-cube shape.
    public static final DeferredBlock<BrewingBarrelBlock> BREWING_BARREL =
            MACHINE_BLOCKS.registerBlock("brewing_barrel", BrewingBarrelBlock::new, RusticBlockProperties.wood(1.5F).noOcclusion());

    // 1.12.2: BlockCondenser, Material.ROCK, hardness 2 - the elixir still. Phase 12 restored the legacy TWO-BLOCK
    // TOP/FACING column (bottom owns the CondenserBlockEntity, top is a decoration that forwards clicks) gated by the
    // outward-facing RETORT flanks; its item and loot ride the same entries() loops as every machine.
    public static final DeferredBlock<CondenserBlock> CONDENSER =
            MACHINE_BLOCKS.registerBlock("condenser", CondenserBlock::new, RusticBlockProperties.stone(2.0F));

    // 1.12.2: BlockCondenserAdvanced, Material.ROCK, hardness 2 - the two-block advanced still (bottom owns the
    // inventory, top forwards) behind CondenserAdvancedBlockEntity, with the modifier slot for advanced recipes.
    // Phase 12 added the horizontal FACING and the outward-facing RETORT_ADVANCED flank gate to match the basic still.
    public static final DeferredBlock<CondenserAdvancedBlock> CONDENSER_ADVANCED =
            MACHINE_BLOCKS.registerBlock("condenser_advanced", CondenserAdvancedBlock::new, RusticBlockProperties.stone(2.0F));

    // 1.12.2: BlockRetort (Material.ROCK, hardness 2, SoundType.STONE, isOpaqueCube=false) - the companion column that
    // flanks a condenser to complete its multi-block shape and, when clicked, opens the still behind its front. It has
    // no block entity, so it needs none of the machine wiring beyond block + block item + loot + pickaxe tag (all
    // supplied by the shared entries() loops). noOcclusion() reproduces the legacy non-opaque contract.
    // RETORT sits beside the basic condenser, RETORT_ADVANCED (same class, different art) beside the advanced one; each
    // condenser's hasRetorts gate requires its own tier.
    public static final DeferredBlock<RetortBlock> RETORT =
            MACHINE_BLOCKS.registerBlock("retort", RetortBlock::new, RusticBlockProperties.stone(2.0F).noOcclusion());

    public static final DeferredBlock<RetortBlock> RETORT_ADVANCED =
            MACHINE_BLOCKS.registerBlock("retort_advanced", RetortBlock::new, RusticBlockProperties.stone(2.0F).noOcclusion());

    // 1.12.2: BlockApiary, Material.WOOD, hardness 1F (read from the legacy constructor - the backlog's "1.5" was
    // a misremember), SoundType.WOOD, isOpaqueCube=false -> noOcclusion(), horizontal FACING facing the placer, and
    // a two-slot (bee / honeycomb) tile entity behind a bespoke GUI. Phase 15 (§A1).
    public static final DeferredBlock<ApiaryBlock> APIARY =
            MACHINE_BLOCKS.registerBlock("apiary", ApiaryBlock::new, RusticBlockProperties.wood(1.0F).noOcclusion());

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
