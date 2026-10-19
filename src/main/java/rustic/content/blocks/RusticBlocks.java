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

public final class RusticBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Rustic.MODID);

    public static final DeferredBlock<RotatedPillarBlock> STONE_PILLAR =
            BLOCKS.registerBlock("stone_pillar", RotatedPillarBlock::new, RusticBlockProperties.stone(2.0F));
    public static final DeferredBlock<RotatedPillarBlock> ANDESITE_PILLAR =
            BLOCKS.registerBlock("andesite_pillar", RotatedPillarBlock::new, RusticBlockProperties.stone(2.0F));
    public static final DeferredBlock<RotatedPillarBlock> DIORITE_PILLAR =
            BLOCKS.registerBlock("diorite_pillar", RotatedPillarBlock::new, RusticBlockProperties.stone(2.0F));
    public static final DeferredBlock<RotatedPillarBlock> GRANITE_PILLAR =
            BLOCKS.registerBlock("granite_pillar", RotatedPillarBlock::new, RusticBlockProperties.stone(2.0F));

    public static final DeferredBlock<RotatedPillarBlock> SLATE_PILLAR =
            BLOCKS.registerBlock("slate_pillar", RotatedPillarBlock::new, RusticBlockProperties.stone(2.0F));

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

    public static final DeferredBlock<Block> CLAY_WALL =
            BLOCKS.registerSimpleBlock("clay_wall", RusticBlockProperties.clay(1.0F));
    public static final DeferredBlock<Block> CLAY_WALL_CROSS =
            BLOCKS.registerSimpleBlock("clay_wall_cross", RusticBlockProperties.clay(1.0F));
    public static final DeferredBlock<BlockClayWallDiag> CLAY_WALL_DIAG =
            BLOCKS.registerBlock("clay_wall_diag", BlockClayWallDiag::new, RusticBlockProperties.clay(1.0F));

    public static final Map<DyeColor, DeferredBlock<Block>> PAINTED_WOOD = paintedWood();

    private static Map<DyeColor, DeferredBlock<Block>> paintedWood() {
        Map<DyeColor, DeferredBlock<Block>> paintedWood = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            paintedWood.put(color, BLOCKS.registerSimpleBlock(paintedWoodPath(color),
                    RusticBlockProperties.wood(2.0F)));
        }
        return Collections.unmodifiableMap(paintedWood);
    }

    public static String paintedWoodPath(DyeColor color) {
        return "painted_wood_" + (color == DyeColor.LIGHT_GRAY ? "silver" : color.getName());
    }

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

        public List<DeferredBlock<?>> decorBlocks() {
            return List.of(planks, fence, fenceGate, slab, stairs, door);
        }

        public List<DeferredBlock<?>> farmingBlocks() {
            return List.of(log, leaves, sapling);
        }
    }

    public static final List<WoodFamily> WOOD_FAMILIES = List.of(woodFamily("olive"), woodFamily("ironwood"));

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

        DeferredBlock<FenceGateBlock> fenceGate = BLOCKS.register("fence_gate_" + name,
                () -> new FenceGateBlock(RusticBlockProperties.woodResistant(2.0F),
                        SoundEvents.FENCE_GATE_OPEN, SoundEvents.FENCE_GATE_CLOSE));

        DeferredBlock<SlabBlock> slab = BLOCKS.register(name + "_slab",
                () -> new SlabBlock(RusticBlockProperties.wood(2.0F)));
        DeferredBlock<StairBlock> stairs = BLOCKS.register("stairs_" + name,
                () -> new StairBlock(planks.get().defaultBlockState(), RusticBlockProperties.ofBase(planks.get())));

        DeferredBlock<DoorBlock> door = BLOCKS.register(name + "_door",
                () -> new DoorBlock(BlockSetType.OAK, RusticBlockProperties.wood(3.0F).noOcclusion()));
        return new WoodFamily(name, planks, log, leaves, sapling, fence, fenceGate, slab, stairs, door);
    }

    public static WoodFamily olive() {
        return WOOD_FAMILIES.get(0);
    }

    public static WoodFamily ironwood() {
        return WOOD_FAMILIES.get(1);
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> treeFeature(String name) {
        return switch (name) {
            case "olive" -> RusticWorldGen.OLIVE_TREE;
            case "ironwood" -> RusticWorldGen.IRONWOOD_TREE;
            default -> throw new IllegalArgumentException("No Rustic tree feature for wood: " + name);
        };
    }

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

    public static final DeferredBlock<BlockWoodLantern> LANTERN_WOOD =
            BLOCKS.registerBlock("lantern_wood", BlockWoodLantern::new,
                    RusticBlockProperties.lit(RusticBlockProperties.wood(1.0F)).noOcclusion());

    public static final DeferredBlock<BlockLattice> IRON_LATTICE =
            BLOCKS.registerBlock("iron_lattice", BlockLattice::new,
                    RusticBlockProperties.metal(2.0F).noOcclusion());
    public static final DeferredBlock<BlockRope> ROPE =
            BLOCKS.registerBlock("rope", BlockRope::new,
                    RusticBlockProperties.cloth(0.5F).noOcclusion());

    public static final List<String> FURNITURE_WOODS =
            List.of("oak", "big_oak", "birch", "spruce", "acacia", "jungle", "olive", "ironwood");

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

    private static BlockBehaviour.Properties furnitureProperties() {
        return RusticBlockProperties.wood(1.0F).noOcclusion();
    }

    public static final DeferredBlock<BlockFertileSoil> FERTILE_SOIL =
            BLOCKS.registerBlock("fertile_soil", BlockFertileSoil::new,
                    RusticBlockProperties.dirt(0.5F));

    public record SilverDecor(
            DeferredBlock<BlockChain> chain,
            DeferredBlock<BlockCandle> candle,
            DeferredBlock<BlockCandleDouble> candleDouble,
            DeferredBlock<BlockCandleLever> candleLever,
            DeferredBlock<BlockChandelier> chandelier,
            DeferredBlock<BlockLantern> lantern
    ) {

        public List<DeferredBlock<?>> blocks() {
            return List.of(chain, candle, candleDouble, candleLever, chandelier, lantern);
        }
    }

    @Nullable
    public static final SilverDecor SILVER_DECOR = silverDecor();

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

    private static BlockBehaviour.Properties iron(float hardness, boolean lit) {
        BlockBehaviour.Properties properties = RusticBlockProperties.metal(hardness).noOcclusion();
        return lit ? RusticBlockProperties.lit(properties) : properties;
    }

    private static BlockBehaviour.Properties candleBlock(float hardness) {
        return RusticBlockProperties.lit(RusticBlockProperties.circuits(hardness)).noOcclusion();
    }

    private static final String SECTION_COMPAT = "Mod Compatability Related Options";
    private static final String KEY_SILVER_DECOR = "enable_silver_decoration_blocks";

    public record DecorFamily(Supplier<Boolean> gate, @Nullable TagKey<Block> tool, List<DeferredBlock<?>> blocks) {
    }

    private static final Supplier<Boolean> ALWAYS = () -> true;

    public static final List<DecorFamily> DECOR_FAMILIES = decorFamilies();

    public static final List<DecorFamily> FARMING_FAMILIES = farmingFamilies();

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

        for (WoodFamily wood : WOOD_FAMILIES) {
            families.add(new DecorFamily(ALWAYS, BlockTags.MINEABLE_WITH_AXE, wood.decorBlocks()));
        }

        families.add(new DecorFamily(ALWAYS, BlockTags.MINEABLE_WITH_AXE, List.of(LANTERN_WOOD)));

        List<DeferredBlock<?>> chairs = new ArrayList<>(FURNITURE.size());
        List<DeferredBlock<?>> tables = new ArrayList<>(FURNITURE.size());
        for (Furniture furniture : FURNITURE) {
            chairs.add(furniture.chair());
            tables.add(furniture.table());
        }
        families.add(new DecorFamily(RusticConfig.ENABLE_CHAIRS::get, BlockTags.MINEABLE_WITH_AXE, chairs));
        families.add(new DecorFamily(RusticConfig.ENABLE_TABLES::get, BlockTags.MINEABLE_WITH_AXE, tables));

        families.add(new DecorFamily(RusticConfig.ENABLE_LATTICE::get, BlockTags.MINEABLE_WITH_PICKAXE,
                List.of(IRON_LATTICE)));

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

        families.add(new DecorFamily(ALWAYS, BlockTags.MINEABLE_WITH_SHOVEL, List.of(FERTILE_SOIL)));
        return List.copyOf(families);
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

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

        for (Furniture furniture : FURNITURE) {
            fire.setFlammable(furniture.chair().get(), 5, 20);
            fire.setFlammable(furniture.table().get(), 5, 20);
        }

        fire.setFlammable(ROPE.get(), 20, 60);
    }

    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return BLOCKS.getEntries();
    }

    private RusticBlocks() {
    }
}
