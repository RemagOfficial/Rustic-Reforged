package rustic.content.blocks;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.config.RusticConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The decorative building blocks, ported from 1.12.2's {@code ModBlocks}.
 *
 * <p>Only the families that are plain cubes, pillars, slabs and stairs of one texture are here yet;
 * the machine, crop, furniture and tile-entity blocks of the legacy file arrive with their own
 * phases. What the legacy file did with {@code new BlockBase(Material.ROCK, "slate").setHardness(2.0F)}
 * plus a self-registering constructor becomes one {@code DeferredRegister} entry, because 1.21 works
 * out the sound, the map colour and the drop from the block's properties and its item instead.
 *
 * <p><b>Deliberate deviation:</b> in 1.12.2 an {@code enable_*} config flag decided whether a block
 * was <em>registered at all</em>, which meant turning a family off in an existing world deleted those
 * blocks from every chunk that contained them. Registering unconditionally and letting the flag decide
 * only whether the blocks appear in the creative tab (and, from Phase 8, whether they generate) keeps
 * old worlds loadable. The groups below are what carries the legacy grouping into the creative tab.
 */
public final class RusticBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Rustic.MODID);

    // ------------------------------------------------------------------
    // Stone pillars. 1.12.2: BlockPillar(type) -> Material.ROCK, hardness 2.0,
    // "<type>_pillar". BlockPillar's own axis property, metadata mapping and rotation
    // are exactly vanilla RotatedPillarBlock's, so the subclass is not ported.
    // ------------------------------------------------------------------
    public static final DeferredBlock<RotatedPillarBlock> STONE_PILLAR =
            BLOCKS.registerBlock("stone_pillar", RotatedPillarBlock::new, RusticBlockProperties.stone(2.0F));
    public static final DeferredBlock<RotatedPillarBlock> ANDESITE_PILLAR =
            BLOCKS.registerBlock("andesite_pillar", RotatedPillarBlock::new, RusticBlockProperties.stone(2.0F));
    public static final DeferredBlock<RotatedPillarBlock> DIORITE_PILLAR =
            BLOCKS.registerBlock("diorite_pillar", RotatedPillarBlock::new, RusticBlockProperties.stone(2.0F));
    public static final DeferredBlock<RotatedPillarBlock> GRANITE_PILLAR =
            BLOCKS.registerBlock("granite_pillar", RotatedPillarBlock::new, RusticBlockProperties.stone(2.0F));
    /** Nested under both flags in 1.12.2: {@code if (ENABLE_PILLARS) { ... if (ENABLE_SLATE) ... }} */
    public static final DeferredBlock<RotatedPillarBlock> SLATE_PILLAR =
            BLOCKS.registerBlock("slate_pillar", RotatedPillarBlock::new, RusticBlockProperties.stone(2.0F));

    // ------------------------------------------------------------------
    // Slate. 1.12.2: Material.ROCK, hardness 2.0, one cube_all texture each.
    // ------------------------------------------------------------------
    public static final DeferredBlock<Block> SLATE =
            BLOCKS.registerSimpleBlock("slate", RusticBlockProperties.stone(2.0F));
    public static final DeferredBlock<Block> SLATE_ROOF =
            BLOCKS.registerSimpleBlock("slate_roof", RusticBlockProperties.stone(2.0F));
    public static final DeferredBlock<Block> SLATE_TILE =
            BLOCKS.registerSimpleBlock("slate_tile", RusticBlockProperties.stone(2.0F));
    public static final DeferredBlock<Block> SLATE_BRICK =
            BLOCKS.registerSimpleBlock("slate_brick", RusticBlockProperties.stone(2.0F));
    public static final DeferredBlock<Block> SLATE_CHISELED =
            BLOCKS.registerSimpleBlock("slate_chiseled", RusticBlockProperties.stone(2.0F));

    // ------------------------------------------------------------------
    // Slate stairs and slabs. The legacy ids are kept (stairs_slate_roof, slate_roof_slab), but the
    // *_doubleslab blocks and the ItemBlockSlabBase that hid them are gone: 1.21 slabs store
    // SlabType.DOUBLE in the block itself, so one block and one item cover bottom, top and double.
    // The properties are copied from the base block, which is what 1.12.2's
    // new BlockStairsBase(SLATE_ROOF.getDefaultState(), ...) inherited through BlockStairs.
    // ------------------------------------------------------------------
    public static final DeferredBlock<StairBlock> SLATE_ROOF_STAIRS =
            BLOCKS.register("stairs_slate_roof", () -> new StairBlock(SLATE_ROOF.get().defaultBlockState(),
                    RusticBlockProperties.ofBase(SLATE_ROOF.get())));
    public static final DeferredBlock<SlabBlock> SLATE_ROOF_SLAB =
            BLOCKS.register("slate_roof_slab", () -> new SlabBlock(RusticBlockProperties.ofBase(SLATE_ROOF.get())));
    public static final DeferredBlock<StairBlock> SLATE_BRICK_STAIRS =
            BLOCKS.register("stairs_slate_brick", () -> new StairBlock(SLATE_BRICK.get().defaultBlockState(),
                    RusticBlockProperties.ofBase(SLATE_BRICK.get())));
    public static final DeferredBlock<SlabBlock> SLATE_BRICK_SLAB =
            BLOCKS.register("slate_brick_slab", () -> new SlabBlock(RusticBlockProperties.ofBase(SLATE_BRICK.get())));

    // ------------------------------------------------------------------
    // Clay walls. 1.12.2: Material.CLAY, hardness 1.0, SoundType.GROUND.
    // ------------------------------------------------------------------
    public static final DeferredBlock<Block> CLAY_WALL =
            BLOCKS.registerSimpleBlock("clay_wall", RusticBlockProperties.clay(1.0F));
    public static final DeferredBlock<Block> CLAY_WALL_CROSS =
            BLOCKS.registerSimpleBlock("clay_wall_cross", RusticBlockProperties.clay(1.0F));
    public static final DeferredBlock<BlockClayWallDiag> CLAY_WALL_DIAG =
            BLOCKS.registerBlock("clay_wall_diag", BlockClayWallDiag::new, RusticBlockProperties.clay(1.0F));

    // ------------------------------------------------------------------
    // Painted wood. 1.12.2: sixteen separate BlockBase fields, Material.WOOD, hardness 2.0, one
    // texture per dye colour, and each one handed to fire with setFireInfo(block, 5, 20) - which is
    // what setFlammables() below does now. Keyed by DyeColor here, in enum order, which is the
    // legacy order too (legacy painted_wood_blue is DyeColor.BLUE, whose texture is blue).
    // ------------------------------------------------------------------
    public static final Map<DyeColor, DeferredBlock<Block>> PAINTED_WOOD = paintedWood();

    private static Map<DyeColor, DeferredBlock<Block>> paintedWood() {
        Map<DyeColor, DeferredBlock<Block>> paintedWood = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            paintedWood.put(color, BLOCKS.registerSimpleBlock(paintedWoodPath(color),
                    RusticBlockProperties.wood(2.0F)));
        }
        return Collections.unmodifiableMap(paintedWood);
    }

    /**
     * The legacy registry name of one painted wood block. Every colour is just
     * {@code painted_wood_<dye colour>}, except light gray, which is still registered as
     * {@code painted_wood_silver} because that is the id 1.12.2 worlds and the old texture file use -
     * {@code DyeColor.SILVER} was renamed to {@code LIGHT_GRAY} in 1.17 without the mod's id changing.
     */
    public static String paintedWoodPath(DyeColor color) {
        return "painted_wood_" + (color == DyeColor.LIGHT_GRAY ? "silver" : color.getName());
    }

    /**
     * A family of decorative blocks: the group that 1.12.2 built together behind one {@code enable_*}
     * flag, and which shared one {@code Material} and therefore one fastest tool. The flag now decides
     * creative tab membership rather than registration (see the class note above), and the material is
     * what the {@code minecraft:mineable/*} block tag in {@code rustic.data.RusticBlockTags} replaces.
     *
     * <p>Later phases add the remaining decor families here (candles, chains, chairs, tables, lattice,
     * lanterns, gargoyle, vase) as their blocks are ported.
     */
    public record DecorFamily(ModConfigSpec.BooleanValue gate, TagKey<Block> tool, List<DeferredBlock<?>> blocks) {
    }

    public static final List<DecorFamily> DECOR_FAMILIES = List.of(
            new DecorFamily(RusticConfig.ENABLE_PILLARS, BlockTags.MINEABLE_WITH_PICKAXE, List.of(
                    STONE_PILLAR, ANDESITE_PILLAR, DIORITE_PILLAR, GRANITE_PILLAR)),
            new DecorFamily(RusticConfig.ENABLE_SLATE, BlockTags.MINEABLE_WITH_PICKAXE, List.of(
                    SLATE_PILLAR, SLATE, SLATE_ROOF, SLATE_TILE, SLATE_BRICK, SLATE_CHISELED,
                    SLATE_ROOF_STAIRS, SLATE_ROOF_SLAB, SLATE_BRICK_STAIRS, SLATE_BRICK_SLAB)),
            new DecorFamily(RusticConfig.ENABLE_CLAY_WALLS, BlockTags.MINEABLE_WITH_SHOVEL, List.of(
                    CLAY_WALL, CLAY_WALL_CROSS, CLAY_WALL_DIAG)),
            new DecorFamily(RusticConfig.ENABLE_PAINTED_WOOD, BlockTags.MINEABLE_WITH_AXE,
                    List.copyOf(PAINTED_WOOD.values())));

    /** Attaches the block register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    /**
     * Replaces 1.12.2's {@code Blocks.FIRE.setFireInfo(block, 5, 20)} calls, one per painted wood
     * block. 1.21.1 still decides flammability from a lookup table held on the fire block rather than
     * from {@code BlockBehaviour.Properties} - NeoForge's {@code IBlockExtension#getFlammability}
     * reads {@link FireBlock#getBurnOdds(BlockState)} - so the ported call is the faithful one. It
     * needs live block instances, hence a setup hook instead of the static initialiser above.
     *
     * <p>Vanilla's own numbers for planks are 5 and 20, which is why the legacy values line up with
     * what wooden blocks still burn at. Later phases add the tables, chairs, fences, logs and rope
     * this same legacy file made flammable.
     */
    public static void setFlammables() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        for (DeferredBlock<Block> paintedWood : PAINTED_WOOD.values()) {
            fire.setFlammable(paintedWood.get(), 5, 20);
        }
    }

    /** All Rustic blocks in registration order, used to create the matching block items and datagen. */
    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return BLOCKS.getEntries();
    }

    private RusticBlocks() {
    }
}
