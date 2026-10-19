package rustic.content.blocks;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.config.ConfigPreread;
import rustic.config.RusticConfig;
import rustic.world.RusticWorldGen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * The decorative building blocks, ported from 1.12.2's {@code ModBlocks}.
 *
 * <p>Only the blocks that need no behaviour of their own are here yet, plus the wood family, whose
 * planks, log, leaves, sapling, fence, gate, slab and stairs are vanilla's own classes. The machine,
 * crop and tile-entity blocks of the legacy file arrive with their own phases. What the legacy file did
 * with {@code new BlockBase(Material.ROCK, "slate").setHardness(2.0F)} plus a self-registering
 * constructor becomes one {@code DeferredRegister} entry, because 1.21 works out the sound, the map
 * colour and the drop from the block's properties and its item instead.
 *
 * <p><b>Deliberate deviation:</b> in 1.12.2 an {@code enable_*} config flag decided whether a block
 * was <em>registered at all</em>, which meant turning a family off in an existing world deleted those
 * blocks from every chunk that contained them. Registering unconditionally and letting the flag decide
 * only whether the blocks appear in the creative tab (and, from Phase 8, whether they generate) keeps
 * old worlds loadable. The groups below are what carries the legacy grouping into the creative tab.
 *
 * <p><b>The one exception is the silver set.</b> {@link #SILVER_DECOR} is the only family still gated at
 * registration time, because its six blocks are a compat add-on for mods that add a silver ingot: an
 * install without such a mod wants them gone entirely, and 1.12.2's own behaviour there is what the
 * approved port keeps. Everything else in this class registers unconditionally.
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

    // ------------------------------------------------------------------
    // The Rustic wood family: olive and ironwood.
    //
    // 1.12.2 registered one block per kind with a VARIANT property and one ItemBlock with subtypes
    // (BlockPlanksRustic.EnumType: OLIVE(0), IRONWOOD(1)), which is why its language keys already read
    // planks_olive, log_ironwood, fence_olive: the sub-type item appended the variant name to the
    // block's. 1.21 has no block metadata, so every variant becomes its own block with that name as its
    // id - the split the port plan prescribes for metadata-bounded blocks, and what makes the per-variant
    // item model, loot table and tags expressible at all. The legacy log's fourth axis ("none", the bark
    // state its metadata allowed) has no equivalent: RotatedPillarBlock only answers x, y and z.
    //
    // Numbers come from the legacy constructors: planks 2.0F hardness with the resistance its
    // setResistance(5F) reported and SoundType.WOOD, and the fence, gate, slab and stairs inherit the
    // planks' pair, which is what 1.12.2's BlockFence/BlockSlab/BlockStairs constructors were handed and
    // what 1.21's own oak equivalents declare - 2.0F and 3.0F. The log, leaves and sapling set no
    // hardness of their own there, so they copy the matching vanilla block rather than invent a number.
    // ------------------------------------------------------------------

    /**
     * One wood type's blocks, as 1.12.2 registered them together: the planks the rest of the family is
     * derived from, the log, leaves and sapling that go to the agriculture tab, and the fence, gate, slab
     * and stairs that go to the decor tab. {@code name} is the wood name, which is the suffix (or for the
     * slab, the prefix) of every id in the family.
     */
    public record WoodFamily(
            String name,
            DeferredBlock<Block> planks,
            DeferredBlock<RotatedPillarBlock> log,
            DeferredBlock<LeavesBlock> leaves,
            DeferredBlock<SaplingBlock> sapling,
            DeferredBlock<FenceBlock> fence,
            DeferredBlock<FenceGateBlock> fenceGate,
            DeferredBlock<SlabBlock> slab,
            DeferredBlock<StairBlock> stairs,
            DeferredBlock<DoorBlock> door
    ) {
        /** The six blocks 1.12.2 put in {@code Rustic.decorTab}. */
        public List<DeferredBlock<?>> decorBlocks() {
            return List.of(planks, fence, fenceGate, slab, stairs, door);
        }

        /** The three blocks 1.12.2 put in {@code Rustic.farmingTab}. */
        public List<DeferredBlock<?>> farmingBlocks() {
            return List.of(log, leaves, sapling);
        }
    }

    public static final List<WoodFamily> WOOD_FAMILIES = List.of(woodFamily("olive"), woodFamily("ironwood"));

    /**
     * Registers one wood type. The sapling is the only entry that needs a supplier rather than
     * {@code registerBlock}: 1.21's {@link SaplingBlock} takes a {@link TreeGrower} next to its properties.
     *
     * <p>The grower names the {@code minecraft:tree} configured feature the sapling matures into - the olive
     * and ironwood features {@code rustic.data.RusticWorldGenProvider} emits under the keys in
     * {@link RusticWorldGen}. Growing those trees is Phase 8's worldgen work; before the feature exists at a
     * key, {@link TreeGrower#growTree} resolves it to nothing and the sapling matures to nothing, but the
     * random-tick light level, the one-in-seven roll and the 0.45 bonemeal chance are all vanilla
     * {@code SaplingBlock} behaviour that {@code BlockSaplingRustic} hand-wrote to the same numbers.
     */
    private static WoodFamily woodFamily(String name) {
        ResourceKey<ConfiguredFeature<?, ?>> tree = treeFeature(name);
        DeferredBlock<Block> planks = BLOCKS.registerBlock("planks_" + name, Block::new,
                RusticBlockProperties.woodResistant(2.0F));
        DeferredBlock<RotatedPillarBlock> log = BLOCKS.registerBlock("log_" + name, RotatedPillarBlock::new,
                RusticBlockProperties.ofBase(Blocks.OAK_LOG));
        DeferredBlock<LeavesBlock> leaves = BLOCKS.registerBlock("leaves_" + name, LeavesBlock::new,
                RusticBlockProperties.ofBase(Blocks.OAK_LEAVES));
        DeferredBlock<SaplingBlock> sapling = BLOCKS.register("sapling_" + name,
                () -> new SaplingBlock(new TreeGrower(name, Optional.empty(), Optional.of(tree), Optional.empty()),
                        RusticBlockProperties.ofBase(Blocks.OAK_SAPLING)));
        DeferredBlock<FenceBlock> fence = BLOCKS.registerBlock("fence_" + name, FenceBlock::new,
                RusticBlockProperties.woodResistant(2.0F));
        // The legacy gate was built on BlockFence's oak sound type and played the plain fence gate sounds,
        // so the two sound events are vanilla's while the properties stay Rustic's.
        DeferredBlock<FenceGateBlock> fenceGate = BLOCKS.register("fence_gate_" + name,
                () -> new FenceGateBlock(RusticBlockProperties.woodResistant(2.0F),
                        SoundEvents.FENCE_GATE_OPEN, SoundEvents.FENCE_GATE_CLOSE));
        // The slab does not inherit the planks' properties the way the stairs do: 1.12.2's
        // {@code BlockSlabBase} built itself off {@code Material.WOOD} and then set its own hardness of
        // 2.0F, never touching the resistance field - so the plank's {@code setResistance(5F)} did not
        // reach it, while {@code BlockStairsBase} copied the source block's raw resistance verbatim. Hence
        // {@code wood(2.0F)} (resistance 0.0F) here and {@code ofBase(planks)} (resistance 3.0F) below.
        DeferredBlock<SlabBlock> slab = BLOCKS.register(name + "_slab",
                () -> new SlabBlock(RusticBlockProperties.wood(2.0F)));
        DeferredBlock<StairBlock> stairs = BLOCKS.register("stairs_" + name,
                () -> new StairBlock(planks.get().defaultBlockState(), RusticBlockProperties.ofBase(planks.get())));
        // Phase 13: the door. 1.12.2's BlockDoorRustic was Material.WOOD, hardness 3F and SoundType.WOOD
        // with no setResistance, so RusticBlockProperties.wood(3.0F) (map colour WOOD, wood sound, strength
        // 3/0) plus the door's own noOcclusion matches it; BlockSetType.OAK re-applies the wood sound and the
        // open/close sounds and gives vanilla's two-half/hinge placement. The legacy StateMap ignored POWERED,
        // which is exactly what BlockStateProvider.doorBlock writes. It went to decorTab, hence decorBlocks().
        DeferredBlock<DoorBlock> door = BLOCKS.register(name + "_door",
                () -> new DoorBlock(BlockSetType.OAK, RusticBlockProperties.wood(3.0F).noOcclusion()));
        return new WoodFamily(name, planks, log, leaves, sapling, fence, fenceGate, slab, stairs, door);
    }

    /** The olive family, named out for the phases that need one particular block. */
    public static WoodFamily olive() {
        return WOOD_FAMILIES.get(0);
    }

    /** The ironwood family, named out for the phases that need one particular block. */
    public static WoodFamily ironwood() {
        return WOOD_FAMILIES.get(1);
    }

    /**
     * The configured-tree feature key a {@code sapling_<name>} matures into, the code half of the
     * agreement between the {@link TreeGrower} and {@code rustic.data.RusticWorldGenProvider}. The only two
     * Rustic woods with world-grown trees are olive and ironwood (1.12.2's {@code WorldGenAllTrees} placed
     * exactly those two); the apple tree is sapling-grown only and its key lives with
     * {@link RusticFarmBlocks#APPLE_SAPLING}.
     */
    private static ResourceKey<ConfiguredFeature<?, ?>> treeFeature(String name) {
        return switch (name) {
            case "olive" -> RusticWorldGen.OLIVE_TREE;
            case "ironwood" -> RusticWorldGen.IRONWOOD_TREE;
            default -> throw new IllegalArgumentException("No Rustic tree feature for wood: " + name);
        };
    }

    // ------------------------------------------------------------------
    // Mounted metal decor. 1.12.2's ModBlocks.init() registered this whole group with no config flag
    // in front of it, in the order below: the plain iron set first ("chain", "candle",
    // "candle_double", "candle_lever", "chandelier", "iron_torch"), then the same blocks again in gold
    // ("chain_gold", "candle_gold", "candle_double_gold", "candle_lever_gold", "chandelier_gold"), then
    // the lanterns ("iron_lantern", "golden_lantern") and finally the gargoyle. Every one of them was
    // built from Material.IRON or Material.CIRCUITS with a hand-set SoundType.METAL, so the tool that
    // works on all of them is a pickaxe. The wooden lantern that 1.12.2 registered with the furniture is
    // the one addition below, and it is axe-mined like the wood it is made of.
    //
    // Hardness and light are the legacy setHardness/setLightLevel calls, read off each legacy
    // constructor: chain 1.0F unlit, candle/candelabra/candle lever 1.0F at full light, iron torch 0.0F
    // at full light, chandelier 2.0F unlit (SoundType.ANVIL instead of METAL), lantern 2.0F at full
    // light, gargoyle 1.0F unlit. The legacy CUTOUT render layer and the isOpaqueCube == false answers
    // are noOcclusion() plus the render_type the model provider writes.
    // ------------------------------------------------------------------
    public static final DeferredBlock<BlockChain> CHAIN =
            BLOCKS.registerBlock("chain", BlockChain::new, iron(1.0F, false));
    public static final DeferredBlock<BlockCandle> CANDLE =
            BLOCKS.registerBlock("candle", BlockCandle::new, candleBlock(1.0F));
    public static final DeferredBlock<BlockCandleDouble> CANDLE_DOUBLE =
            BLOCKS.registerBlock("candle_double", BlockCandleDouble::new, candleBlock(1.0F));
    public static final DeferredBlock<BlockCandleLever> CANDLE_LEVER =
            BLOCKS.registerBlock("candle_lever", BlockCandleLever::new, candleBlock(1.0F));
    public static final DeferredBlock<BlockChandelier> CHANDELIER =
            BLOCKS.registerBlock("chandelier", BlockChandelier::new,
                    RusticBlockProperties.anvil(2.0F).noOcclusion());
    public static final DeferredBlock<BlockIronTorch> IRON_TORCH =
            BLOCKS.registerBlock("iron_torch", BlockIronTorch::new, candleBlock(0.0F));
    public static final DeferredBlock<BlockChain> CHAIN_GOLD =
            BLOCKS.registerBlock("chain_gold", BlockChain::new, iron(1.0F, false));
    public static final DeferredBlock<BlockCandle> CANDLE_GOLD =
            BLOCKS.registerBlock("candle_gold", BlockCandle::new, candleBlock(1.0F));
    public static final DeferredBlock<BlockCandleDouble> CANDLE_DOUBLE_GOLD =
            BLOCKS.registerBlock("candle_double_gold", BlockCandleDouble::new, candleBlock(1.0F));
    public static final DeferredBlock<BlockCandleLever> CANDLE_LEVER_GOLD =
            BLOCKS.registerBlock("candle_lever_gold", BlockCandleLever::new, candleBlock(1.0F));
    public static final DeferredBlock<BlockChandelier> CHANDELIER_GOLD =
            BLOCKS.registerBlock("chandelier_gold", BlockChandelier::new,
                    RusticBlockProperties.anvil(2.0F).noOcclusion());
    public static final DeferredBlock<BlockLantern> IRON_LANTERN =
            BLOCKS.registerBlock("iron_lantern", BlockLantern::new, iron(2.0F, true));
    public static final DeferredBlock<BlockLantern> GOLDEN_LANTERN =
            BLOCKS.registerBlock("golden_lantern", BlockLantern::new, iron(2.0F, true));
    public static final DeferredBlock<BlockGargoyle> GARGOYLE =
            BLOCKS.registerBlock("gargoyle", BlockGargoyle::new,
                    RusticBlockProperties.stone(1.0F).noOcclusion());

    /*
     * The wooden lantern is 1.12.2's last mount, but it was registered at the tail of the file among the
     * furniture rather than with this group, and it is wooden instead of metallic: Material.WOOD, a wood
     * sound, hardness 1.0F and full light, which is RusticBlockProperties.wood rather than iron().
     */
    public static final DeferredBlock<BlockWoodLantern> LANTERN_WOOD =
            BLOCKS.registerBlock("lantern_wood", BlockWoodLantern::new,
                    RusticBlockProperties.lit(RusticBlockProperties.wood(1.0F)).noOcclusion());

    // ------------------------------------------------------------------
    // The lattice and the rope, the two hanging blocks that are not metal mounts.
    //
    // 1.12.2 built the lattice from Material.IRON at hardness 2.0F with SoundType.METAL and hid it behind
    // Config.ENABLE_LATTICE; the rope was its own MaterialRope (cloth colour, cloth sound) at hardness 0.5F
    // with no config flag in front of it. Neither called setResistance, so both report a zero like the rest
    // of the decor. The rope is the one block in the mod with no tool: its custom Material matched none of
    // the seven vanilla entries in 1.12.2's mining-speed table, so nothing was ever faster on it than a
    // hand - see the null in decorFamilies() and the note on RusticBlockProperties.cloth.
    //
    // Both need noOcclusion() for the same reason the metal mounts do: 1.12.2's isOpaqueCube == false, and
    // the lattice alone also carries the CUTOUT layer, which its models now name.
    // ------------------------------------------------------------------
    public static final DeferredBlock<BlockLattice> IRON_LATTICE =
            BLOCKS.registerBlock("iron_lattice", BlockLattice::new,
                    RusticBlockProperties.metal(2.0F).noOcclusion());
    public static final DeferredBlock<BlockRope> ROPE =
            BLOCKS.registerBlock("rope", BlockRope::new,
                    RusticBlockProperties.cloth(0.5F).noOcclusion());

    // ------------------------------------------------------------------
    // Furniture. 1.12.2 registered eight chairs and then eight tables, the whole pair of groups inside
    // its own enable_* flag, and every one of them was Material.WOOD at hardness 1.0F with a wood sound,
    // a non-opaque cube and Blocks.FIRE.setFireInfo(block, 5, 20). The eight woods are the six vanilla
    // types plus Rustic's own two; the names are spelled out because they are the legacy ids rather than
    // anything a 1.21 type can be asked for - big_oak in particular is 1.12.2's name for what 1.21 calls
    // dark oak, and the id has to stay put for old worlds to keep their chairs.
    //
    // Only the two Rustic woods among them have their planks registered by this mod, and the furniture
    // does not care: a chair draws its own texture and the six vanilla planks textures come from the
    // vanilla domain, which is why the legacy model files point at blocks/planks_oak.
    // ------------------------------------------------------------------

    /** The eight wood names 1.12.2 made furniture from, in its registration order. */
    public static final List<String> FURNITURE_WOODS =
            List.of("oak", "big_oak", "birch", "spruce", "acacia", "jungle", "olive", "ironwood");

    /**
     * One wood's furniture pair. 1.12.2 registered all eight chairs before any table, which the port
     * flattens into one entry per wood: the two groups are still gated and listed separately below, so
     * the only thing that changes is the order the block registry walks them in.
     */
    public record Furniture(String name, DeferredBlock<BlockChair> chair, DeferredBlock<BlockTable> table) {
    }

    public static final List<Furniture> FURNITURE = furniture();

    private static List<Furniture> furniture() {
        List<Furniture> furniture = new ArrayList<>(FURNITURE_WOODS.size());
        for (String wood : FURNITURE_WOODS) {
            furniture.add(new Furniture(wood,
                    BLOCKS.registerBlock("chair_" + wood, BlockChair::new, furnitureProperties()),
                    BLOCKS.registerBlock("table_" + wood, BlockTable::new, furnitureProperties())));
        }
        return List.copyOf(furniture);
    }

    /**
     * What 1.12.2 handed both chair and table: Material.WOOD at hardness 1.0F, whose sound the legacy
     * constructors set to {@code SoundType.WOOD} by hand - the same thing the material already answered -
     * and whose {@code isOpaqueCube}/{@code isFullCube} false answers become {@code noOcclusion()}.
     */
    private static BlockBehaviour.Properties furnitureProperties() {
        return RusticBlockProperties.wood(1.0F).noOcclusion();
    }

    // ------------------------------------------------------------------
    // Fertile soil, which 1.12.2 registered among the farming blocks with no config flag of its own:
    // Material.GROUND at hardness 0.5F with SoundType.GROUND, the spade's material, and farmland's wet
    // texture on top. It goes to the agriculture tab, so it joins FARMING_FAMILIES below rather than the
    // decor groups above.
    // ------------------------------------------------------------------
    public static final DeferredBlock<BlockFertileSoil> FERTILE_SOIL =
            BLOCKS.registerBlock("fertile_soil", BlockFertileSoil::new,
                    RusticBlockProperties.dirt(0.5F));

    // ------------------------------------------------------------------
    // The silver set, which is the one decor family that still obeys its config flag at registration
    // time - see the class note and SILVER_DECOR.
    // ------------------------------------------------------------------

    /**
     * The six blocks 1.12.2 hid behind {@code Config.ENABLE_SILVER_DECOR}, named so that the phases
     * which consume them (the silver recipes of {@code Recipes.addSilverCraftingRecipes}, the book
     * entries) can reach them individually.
     */
    public record SilverDecor(
            DeferredBlock<BlockChain> chain,
            DeferredBlock<BlockCandle> candle,
            DeferredBlock<BlockCandleDouble> candleDouble,
            DeferredBlock<BlockCandleLever> candleLever,
            DeferredBlock<BlockChandelier> chandelier,
            DeferredBlock<BlockLantern> lantern
    ) {
        /** The six holders in legacy registration order, for the creative tab and datagen. */
        public List<DeferredBlock<?>> blocks() {
            return List.of(chain, candle, candleDouble, candleLever, chandelier, lantern);
        }
    }

    /**
     * The 1.12.2 {@code enable_silver_decoration_blocks} flag, read straight off the TOML file because
     * {@link rustic.config.RusticConfig} is not loadable yet while blocks are being registered.
     *
     * <p>This is the one deliberate exception to the rule stated at the top of this class: with the flag
     * off the six silver blocks are <em>not registered at all</em>, exactly as they were not in 1.12.2,
     * and {@code SILVER_DECOR} is null to say so. Consumers have to check it, which is what the legacy
     * code did too - its {@code ModBlocks.CANDLE_SILVER} field was simply null in that case, and
     * {@code addSilverCraftingRecipes()} opened with the same config test before touching it.
     *
     * <p>TODO (deferred): consider also gating the silver <em>block registration</em> on whether
     * {@code c:ingots/silver} is actually populated at registry time. Today the blocks always register
     * (when the config flag is on) but their recipes are condition-dropped, so a Rustic-only install
     * shows silver decor in JEI/creative tab with no way to craft it. Legacy had the same display
     * quirk, but a one-line {@code if (TagEmpty(c:ingots/silver)) return null} in {@code silverDecor()}
     * would eliminate it cleanly once the data lifecycle allows the check (blocks register before tags
     * resolve, so this needs either a lazy holder or a registry-completion event).
     */
    @Nullable
    public static final SilverDecor SILVER_DECOR = silverDecor();

    /** Whether the silver decor was registered; false only when the config file says so. */
    public static boolean silverDecorEnabled() {
        return SILVER_DECOR != null;
    }

    @Nullable
    private static SilverDecor silverDecor() {
        if (!ConfigPreread.flag(SECTION_COMPAT, KEY_SILVER_DECOR, true)) {
            return null;
        }
        return new SilverDecor(
                BLOCKS.registerBlock("chain_silver", BlockChain::new, iron(1.0F, false)),
                BLOCKS.registerBlock("candle_silver", BlockCandle::new, candleBlock(1.0F)),
                BLOCKS.registerBlock("candle_double_silver", BlockCandleDouble::new, candleBlock(1.0F)),
                BLOCKS.registerBlock("candle_lever_silver", BlockCandleLever::new, candleBlock(1.0F)),
                BLOCKS.registerBlock("chandelier_silver", BlockChandelier::new,
                        RusticBlockProperties.anvil(2.0F).noOcclusion()),
                BLOCKS.registerBlock("silver_lantern", BlockLantern::new, iron(2.0F, true)));
    }

    /**
     * The legacy {@code Material.IRON} blocks that hang: metal sound, pickaxe, no occlusion. The lantern
     * called {@code setLightLevel(1.0F)}, the chain did not.
     */
    private static BlockBehaviour.Properties iron(float hardness, boolean lit) {
        BlockBehaviour.Properties properties = RusticBlockProperties.metal(hardness).noOcclusion();
        return lit ? RusticBlockProperties.lit(properties) : properties;
    }

    /**
     * The three candle blocks, which 1.12.2 built from {@code Material.CIRCUITS} with a metal sound, full
     * light and the same hardness - the iron torch is the same shape again at hardness 0.0F.
     */
    private static BlockBehaviour.Properties candleBlock(float hardness) {
        return RusticBlockProperties.lit(RusticBlockProperties.circuits(hardness)).noOcclusion();
    }

    /**
     * The config section and key of the silver flag, spelled out here because {@code RusticConfig}
     * builds them with a {@code push()} call whose string is not reachable as a constant, and the
     * pre-read has to name the same pair.
     */
    private static final String SECTION_COMPAT = "Mod Compatability Related Options";
    private static final String KEY_SILVER_DECOR = "enable_silver_decoration_blocks";

    /**
     * A family of decorative blocks: the group that 1.12.2 built together behind one {@code enable_*}
     * flag, and which shared one {@code Material} and therefore one fastest tool. The flag now decides
     * creative tab membership rather than registration (see the class note above), and the material is
     * what the {@code minecraft:mineable/*} block tag in {@code rustic.data.RusticBlockTags} replaces.
     *
     * <p>The gate is a {@link Supplier} rather than the {@code BooleanValue} four of the families are
     * built from because the silver family has no live config value to hand out: its blocks are either
     * registered or absent, and a family that does not exist is not added to the list at all.
     *
     * <p>{@code tool} is nullable because 1.12.2's rope was made of a {@code Material} subclass of the
     * mod's own, which matched none of the entries in the engine's mining-speed table - so no tool is
     * faster on it, and writing one here would be an improvement rather than a port. The tag provider
     * skips a null rather than emitting an empty tag.
     */
    public record DecorFamily(Supplier<Boolean> gate, @Nullable TagKey<Block> tool, List<DeferredBlock<?>> blocks) {
    }

    /** The gate for the decor 1.12.2 registered unconditionally: nothing hides it from the tab. */
    private static final Supplier<Boolean> ALWAYS = () -> true;

    public static final List<DecorFamily> DECOR_FAMILIES = decorFamilies();

    /**
     * The groups that 1.12.2 filled {@code Rustic.farmingTab} with instead of the decor tab: the wood
     * family's log, leaves and sapling, and fertile soil. The same record as the decor groups, because it
     * answers the same two questions - which flag hides the group, and which tool mines it - and only the
     * tab it is read by differs.
     */
    public static final List<DecorFamily> FARMING_FAMILIES = farmingFamilies();

    /**
     * Every tab group in one list, for the block tag provider, which cares about the tool a block needs
     * but not about which creative tab it shows up in. The two lists are disjoint by construction: a wood
     * family contributes its decor half here and its farming half there.
     */
    public static List<DecorFamily> families() {
        List<DecorFamily> all = new ArrayList<>(DECOR_FAMILIES);
        all.addAll(FARMING_FAMILIES);
        return List.copyOf(all);
    }

    private static List<DecorFamily> decorFamilies() {
        List<DecorFamily> families = new ArrayList<>(List.of(
                new DecorFamily(RusticConfig.ENABLE_PILLARS::get, BlockTags.MINEABLE_WITH_PICKAXE, List.of(
                        STONE_PILLAR, ANDESITE_PILLAR, DIORITE_PILLAR, GRANITE_PILLAR)),
                new DecorFamily(RusticConfig.ENABLE_SLATE::get, BlockTags.MINEABLE_WITH_PICKAXE, List.of(
                        SLATE_PILLAR, SLATE, SLATE_ROOF, SLATE_TILE, SLATE_BRICK, SLATE_CHISELED,
                        SLATE_ROOF_STAIRS, SLATE_ROOF_SLAB, SLATE_BRICK_STAIRS, SLATE_BRICK_SLAB)),
                new DecorFamily(RusticConfig.ENABLE_CLAY_WALLS::get, BlockTags.MINEABLE_WITH_SHOVEL, List.of(
                        CLAY_WALL, CLAY_WALL_CROSS, CLAY_WALL_DIAG)),
                new DecorFamily(RusticConfig.ENABLE_PAINTED_WOOD::get, BlockTags.MINEABLE_WITH_AXE,
                        List.copyOf(PAINTED_WOOD.values())),
                new DecorFamily(ALWAYS, BlockTags.MINEABLE_WITH_PICKAXE, List.of(
                        CHAIN, CANDLE, CANDLE_DOUBLE, CANDLE_LEVER, CHANDELIER, IRON_TORCH,
                        CHAIN_GOLD, CANDLE_GOLD, CANDLE_DOUBLE_GOLD, CANDLE_LEVER_GOLD, CHANDELIER_GOLD,
                        IRON_LANTERN, GOLDEN_LANTERN, GARGOYLE))));
        // The wood family's decor half, which 1.12.2 registered without a config flag of its own: the
        // planks, fences, gates, slabs and stairs of both woods, in the legacy registration order.
        for (WoodFamily wood : WOOD_FAMILIES) {
            families.add(new DecorFamily(ALWAYS, BlockTags.MINEABLE_WITH_AXE, wood.decorBlocks()));
        }
        // The wooden lantern joins the axe group rather than the pickaxe one above: 1.12.2 had no tool tag
        // at all, the material decided which tool was effective, and this lantern was built from
        // Material.WOOD while every other mount of its group was iron or circuits.
        families.add(new DecorFamily(ALWAYS, BlockTags.MINEABLE_WITH_AXE, List.of(LANTERN_WOOD)));
        // The two furniture groups, which are the last of the legacy enable_* decor flags this file has to
        // honour. Both were Material.WOOD, so both are axe-mined like the painted wood they sat next to.
        List<DeferredBlock<?>> chairs = new ArrayList<>(FURNITURE.size());
        List<DeferredBlock<?>> tables = new ArrayList<>(FURNITURE.size());
        for (Furniture furniture : FURNITURE) {
            chairs.add(furniture.chair());
            tables.add(furniture.table());
        }
        families.add(new DecorFamily(RusticConfig.ENABLE_CHAIRS::get, BlockTags.MINEABLE_WITH_AXE, chairs));
        families.add(new DecorFamily(RusticConfig.ENABLE_TABLES::get, BlockTags.MINEABLE_WITH_AXE, tables));
        // The lattice is its own group because it is its own flag: 1.12.2 hid the single block behind
        // enable_lattice, and it is iron, so it takes the pickaxe of the mounted-metal set above.
        families.add(new DecorFamily(RusticConfig.ENABLE_LATTICE::get, BlockTags.MINEABLE_WITH_PICKAXE,
                List.of(IRON_LATTICE)));
        // ... and the rope is the family with no tool at all, for the reason given on DecorFamily.
        families.add(new DecorFamily(ALWAYS, null, List.of(ROPE)));
        if (SILVER_DECOR != null) {
            families.add(new DecorFamily(ALWAYS, BlockTags.MINEABLE_WITH_PICKAXE, SILVER_DECOR.blocks()));
        }
        return List.copyOf(families);
    }

    private static List<DecorFamily> farmingFamilies() {
        List<DecorFamily> families = new ArrayList<>();
        for (WoodFamily wood : WOOD_FAMILIES) {
            families.add(new DecorFamily(ALWAYS, BlockTags.MINEABLE_WITH_AXE, wood.farmingBlocks()));
        }
        // Fertile soil, which is why 1.12.2 put it in the agriculture tab: Material.GROUND, so a spade.
        families.add(new DecorFamily(ALWAYS, BlockTags.MINEABLE_WITH_SHOVEL, List.of(FERTILE_SOIL)));
        return List.copyOf(families);
    }

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
     * what wooden blocks still burn at. The wood family's numbers are the legacy {@code setFireInfo}
     * calls read off each class: planks, fence, gate, slab and stairs 5 and 20, the log 5 and 5, the
     * leaves 30 and 60, and the sapling none at all - {@code BlockSaplingRustic} never asked for it.
     */
    public static void setFlammables() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        for (DeferredBlock<Block> paintedWood : PAINTED_WOOD.values()) {
            fire.setFlammable(paintedWood.get(), 5, 20);
        }
        for (WoodFamily wood : WOOD_FAMILIES) {
            fire.setFlammable(wood.planks().get(), 5, 20);
            fire.setFlammable(wood.fence().get(), 5, 20);
            fire.setFlammable(wood.fenceGate().get(), 5, 20);
            fire.setFlammable(wood.slab().get(), 5, 20);
            fire.setFlammable(wood.stairs().get(), 5, 20);
            fire.setFlammable(wood.log().get(), 5, 5);
            fire.setFlammable(wood.leaves().get(), 30, 60);
        }
        // 1.12.2 asked for the same pair on every chair and every table, in their own constructors.
        for (Furniture furniture : FURNITURE) {
            fire.setFlammable(furniture.chair().get(), 5, 20);
            fire.setFlammable(furniture.table().get(), 5, 20);
        }
        // The rope's own constructor asked for 20 and 60, which is what the legacy MaterialRope's
        // setBurning() was paired with - cloth catches fast and burns long, like leaves.
        fire.setFlammable(ROPE.get(), 20, 60);
    }

    /** All Rustic blocks in registration order, used to create the matching block items and datagen. */
    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return BLOCKS.getEntries();
    }

    private RusticBlocks() {
    }
}
