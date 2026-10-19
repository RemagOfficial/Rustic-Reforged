package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.util.TriState;
import rustic.config.RusticConfig;
import rustic.content.RusticCreativeTabs;
import rustic.content.blocks.BlockCandle;
import rustic.content.blocks.BlockCandleDouble;
import rustic.content.blocks.BlockCandleLever;
import rustic.content.blocks.BlockChain;
import rustic.content.blocks.BlockChandelier;
import rustic.content.blocks.BlockChair;
import rustic.content.blocks.BlockClayWallDiag;
import rustic.content.blocks.BlockFertileSoil;
import rustic.content.blocks.BlockGargoyle;
import rustic.content.blocks.BlockIronTorch;
import rustic.content.blocks.BlockLantern;
import rustic.content.blocks.BlockLattice;
import rustic.content.blocks.BlockRope;
import rustic.content.blocks.BlockRopeBase;
import rustic.content.blocks.BlockRotatable;
import rustic.content.blocks.BlockTable;
import rustic.content.blocks.BlockWoodLantern;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.fluids.RusticFluidBlocks;



public class RusticDecorRegistrationTest {

    
    private record Expected(Class<? extends Block> type, float hardness, Float resistance) {
        
        Expected(Class<? extends Block> type, float hardness) {
            this(type, hardness, 0.0F);
        }



        static Expected derived(Class<? extends Block> type, float hardness) {
            return new Expected(type, hardness, null);
        }

        
        static Expected resistant(Class<? extends Block> type, float hardness) {
            return new Expected(type, hardness, 3.0F);
        }
    }



    private static final Map<String, Expected> EXPECTED = expected();

    private static Map<String, Expected> expected() {
        Map<String, Expected> expected = new LinkedHashMap<>();

        for (String type : List.of("stone", "andesite", "diorite", "granite", "slate")) {
            expected.put(type + "_pillar", new Expected(RotatedPillarBlock.class, 2.0F));
        }

        for (String slate : List.of("slate", "slate_roof", "slate_tile", "slate_brick", "slate_chiseled")) {
            expected.put(slate, new Expected(Block.class, 2.0F));
        }
        expected.put("stairs_slate_roof", new Expected(StairBlock.class, 2.0F));
        expected.put("slate_roof_slab", new Expected(SlabBlock.class, 2.0F));
        expected.put("stairs_slate_brick", new Expected(StairBlock.class, 2.0F));
        expected.put("slate_brick_slab", new Expected(SlabBlock.class, 2.0F));

        expected.put("clay_wall", new Expected(Block.class, 1.0F));
        expected.put("clay_wall_cross", new Expected(Block.class, 1.0F));
        expected.put("clay_wall_diag", new Expected(BlockClayWallDiag.class, 1.0F));

        for (String colour : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
                "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            expected.put("painted_wood_" + colour, new Expected(Block.class, 2.0F));
        }
        return Map.copyOf(expected);
    }

    
    private static final List<String> REMOVED_IDS = List.of("slate_roof_doubleslab", "slate_brick_doubleslab");



    private static final Map<String, Expected> EXPECTED_WOOD = expectedWood();

    private static Map<String, Expected> expectedWood() {
        Map<String, Expected> expected = new LinkedHashMap<>();
        for (String wood : List.of("olive", "ironwood")) {
            expected.put("planks_" + wood, Expected.resistant(Block.class, 2.0F));
            expected.put("fence_" + wood, Expected.resistant(FenceBlock.class, 2.0F));
            expected.put("fence_gate_" + wood, Expected.resistant(FenceGateBlock.class, 2.0F));

            expected.put(wood + "_slab", new Expected(SlabBlock.class, 2.0F));
            expected.put("stairs_" + wood, Expected.resistant(StairBlock.class, 2.0F));

            expected.put(wood + "_door", new Expected(DoorBlock.class, 3.0F));
            expected.put("log_" + wood, Expected.derived(RotatedPillarBlock.class, 2.0F));
            expected.put("leaves_" + wood, Expected.derived(LeavesBlock.class, 0.2F));
            expected.put("sapling_" + wood, Expected.derived(SaplingBlock.class, 0.0F));
        }

        for (String suffix : List.of("", "_gold", "_silver")) {
            expected.put("chain" + suffix, new Expected(BlockChain.class, 1.0F));
            expected.put("candle" + suffix, new Expected(BlockCandle.class, 1.0F));
            expected.put("candle_double" + suffix, new Expected(BlockCandleDouble.class, 1.0F));
            expected.put("candle_lever" + suffix, new Expected(BlockCandleLever.class, 1.0F));
            expected.put("chandelier" + suffix, new Expected(BlockChandelier.class, 2.0F));
        }
        expected.put("iron_torch", new Expected(BlockIronTorch.class, 0.0F));
        expected.put("iron_lantern", new Expected(BlockLantern.class, 2.0F));
        expected.put("golden_lantern", new Expected(BlockLantern.class, 2.0F));
        expected.put("silver_lantern", new Expected(BlockLantern.class, 2.0F));
        expected.put("gargoyle", new Expected(BlockGargoyle.class, 1.0F));

        expected.put("lantern_wood", new Expected(BlockWoodLantern.class, 1.0F));
        for (String wood : List.of("oak", "big_oak", "birch", "spruce", "acacia", "jungle", "olive", "ironwood")) {
            expected.put("chair_" + wood, new Expected(BlockChair.class, 1.0F));
            expected.put("table_" + wood, new Expected(BlockTable.class, 1.0F));
        }

        expected.put("iron_lattice", new Expected(BlockLattice.class, 2.0F));
        expected.put("rope", new Expected(BlockRope.class, 0.5F));
        expected.put("fertile_soil", new Expected(BlockFertileSoil.class, 0.5F));
        return Map.copyOf(expected);
    }

    
    private static final Map<String, Expected> EXPECTED_ALL = merged();

    private static Map<String, Expected> merged() {
        Map<String, Expected> all = new LinkedHashMap<>(EXPECTED);
        all.putAll(EXPECTED_WOOD);
        return Map.copyOf(all);
    }



    private static final List<String> FARMING_TAB_IDS = List.of(
            "log_olive", "leaves_olive", "sapling_olive",
            "log_ironwood", "leaves_ironwood", "sapling_ironwood",
            "fertile_soil");



    private static final Map<String, String> TOOLS = tools();

    private static Map<String, String> tools() {
        Map<String, String> tools = new LinkedHashMap<>();
        EXPECTED.keySet().forEach(id -> tools.put(id, id.startsWith("painted_wood_")
                ? "axe" : id.startsWith("clay_wall") ? "shovel" : "pickaxe"));
        for (String wood : List.of("olive", "ironwood")) {
            for (String part : List.of("planks_", "fence_", "fence_gate_", "stairs_", "log_", "leaves_", "sapling_")) {
                tools.put(part + wood, "axe");
            }
            tools.put(wood + "_slab", "axe");
            tools.put(wood + "_door", "axe");
        }
        for (String suffix : List.of("", "_gold", "_silver")) {
            tools.put("chain" + suffix, "pickaxe");
            tools.put("candle" + suffix, "pickaxe");
            tools.put("candle_double" + suffix, "pickaxe");
            tools.put("candle_lever" + suffix, "pickaxe");
            tools.put("chandelier" + suffix, "pickaxe");
        }
        tools.put("iron_torch", "pickaxe");
        tools.put("iron_lantern", "pickaxe");
        tools.put("golden_lantern", "pickaxe");
        tools.put("silver_lantern", "pickaxe");

        tools.put("gargoyle", "pickaxe");
        tools.put("lantern_wood", "axe");
        for (String wood : List.of("oak", "big_oak", "birch", "spruce", "acacia", "jungle", "olive", "ironwood")) {
            tools.put("chair_" + wood, "axe");
            tools.put("table_" + wood, "axe");
        }
        tools.put("iron_lattice", "pickaxe");
        tools.put("rope", null);
        tools.put("fertile_soil", "shovel");
        return Collections.unmodifiableMap(tools);
    }

    
    private static final Map<String, int[]> FIRE = fire();

    private static Map<String, int[]> fire() {
        Map<String, int[]> fire = new LinkedHashMap<>();
        EXPECTED.keySet().stream().filter(id -> id.startsWith("painted_wood_")).forEach(id -> fire.put(id, new int[]{5, 20}));
        for (String wood : List.of("olive", "ironwood")) {
            fire.put("planks_" + wood, new int[]{5, 20});
            fire.put("fence_" + wood, new int[]{5, 20});
            fire.put("fence_gate_" + wood, new int[]{5, 20});
            fire.put(wood + "_slab", new int[]{5, 20});
            fire.put("stairs_" + wood, new int[]{5, 20});

            fire.put("log_" + wood, new int[]{5, 5});
            fire.put("leaves_" + wood, new int[]{30, 60});
        }
        for (String wood : List.of("oak", "big_oak", "birch", "spruce", "acacia", "jungle", "olive", "ironwood")) {
            fire.put("chair_" + wood, new int[]{5, 20});
            fire.put("table_" + wood, new int[]{5, 20});
        }

        fire.put("rope", new int[]{20, 60});
        return Collections.unmodifiableMap(fire);
    }




    @Test
    public void everyLegacyDecorBlockIsRegisteredUnderItsLegacyIdWithALegacyShapedClass() {
        Map<String, Block> registered = rusticBlocks();

        assertEquals(EXPECTED_ALL.size(), registered.size(),
                "rustic block count changed without a baseline update: expected " + EXPECTED_ALL.keySet()
                        + " but found " + registered.keySet());

        for (Map.Entry<String, Expected> entry : EXPECTED_ALL.entrySet()) {
            Block block = registered.get(entry.getKey());
            assertNotNull(block, "rustic:" + entry.getKey() + " is not registered");
            assertSame(entry.getValue().type(), block.getClass(),
                    "rustic:" + entry.getKey() + " is a " + block.getClass().getSimpleName());
        }
    }

    @Test
    public void theLegacyHiddenDoubleSlabsAreGone() {
        Map<String, Block> registered = rusticBlocks();
        for (String id : REMOVED_IDS) {
            assertFalse(registered.containsKey(id),
                    "rustic:" + id + " must not come back: SlabBlock.DOUBLE replaced it");
        }
    }



    @Test
    public void everyBlockHasABlockItemUnderTheSameId() {
        Map<String, Block> registered = rusticBlocks();

        for (Map.Entry<String, Block> entry : registered.entrySet()) {
            Item item = entry.getValue().asItem();
            assertTrue(item instanceof BlockItem,
                    "rustic:" + entry.getKey() + " has no block item (it resolves to " + item + ")");
            assertEquals(entry.getKey(), BuiltInRegistries.ITEM.getKey(item).getPath(),
                    "block item id drifted away from the block id");
            assertEquals(((BlockItem) item).getBlock(), entry.getValue());
        }

        assertEquals(registered.size(), rusticItems().size(),
                "a rustic item exists that is not a block item, which is a later phase's work");
    }



    @Test
    public void theWoodenDoorsAreTwoHighAndIgnorePoweredLikeTheLegacyBlockDoor() {
        for (String wood : List.of("olive", "ironwood")) {
            Block block = rusticBlocks().get(wood + "_door");
            assertSame(DoorBlock.class, block.getClass(), wood + "_door must be a vanilla DoorBlock");
            Item item = block.asItem();
            assertTrue(item instanceof DoubleHighBlockItem,
                    wood + "_door's item must place both halves, but is " + item.getClass().getSimpleName());
            assertEquals(wood + "_door", BuiltInRegistries.ITEM.getKey(item).getPath(), wood + " door item id");

            List<String> properties = block.getStateDefinition().getProperties().stream()
                    .map(Property::getName).sorted().toList();
            assertEquals(List.of("facing", "half", "hinge", "open", "powered"), properties,
                    wood + "_door lost one of the legacy door's five states");


            JsonObject variants = json("/assets/rustic/blockstates/" + wood + "_door.json")
                    .getAsJsonObject("variants");
            for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
                assertFalse(variant.getKey().contains("powered"),
                        wood + "_door blockstate variant keys on POWERED, which the legacy StateMap ignored: "
                                + variant.getKey());
            }
        }
    }





    @Test
    public void hardnessAndExplosionResistanceFollowTheLegacyNumbers() {
        EXPECTED_ALL.forEach((id, expected) -> {
            Block block = rusticBlocks().get(id);
            assertNotNull(block, id);
            assertEquals(expected.hardness(), block.defaultDestroyTime(), 0.0001F, id + " hardness");
            if (expected.resistance() == null) {

                Block source = vanillaSource(id);
                assertEquals(source.getExplosionResistance(source.defaultBlockState(), null, null, null),
                        block.getExplosionResistance(block.defaultBlockState(), null, null, null),
                        0.0001F, id + " should keep " + BuiltInRegistries.BLOCK.getKey(source) + "'s resistance");
            } else {
                assertEquals(expected.resistance(), block.getExplosionResistance(block.defaultBlockState(),
                        null, null, null), 0.0001F, id + " resistance");
            }
        });
    }



    @Test
    public void soundsMatchTheLegacyMaterialsAndTheirOverrides() {
        EXPECTED_ALL.forEach((id, expected) -> {
            Block block = rusticBlocks().get(id);
            SoundType sound = block.getSoundType(block.defaultBlockState(), null, null, null);
            if (expected.resistance() == null) {
                return;

            }
            assertSame(expectedSound(id), sound, id);
        });
    }



    private static SoundType expectedSound(String id) {
        if (id.startsWith("chandelier")) {
            return SoundType.ANVIL;
        }
        if (id.startsWith("painted_wood_") || id.equals("lantern_wood") || id.startsWith("chair_")
                || id.startsWith("table_")) {
            return SoundType.WOOD;
        }
        for (String wood : List.of("olive", "ironwood")) {
            if (id.equals("planks_" + wood) || id.equals("fence_" + wood) || id.equals("fence_gate_" + wood)
                    || id.equals(wood + "_slab") || id.equals("stairs_" + wood) || id.equals(wood + "_door")) {
                return SoundType.WOOD;
            }
        }
        if (id.startsWith("clay_wall") || id.equals("fertile_soil")) {
            return SoundType.GRAVEL;
        }

        if (id.startsWith("stairs_slate")) {
            return SoundType.STONE;
        }

        if (id.equals("rope")) {
            return SoundType.WOOL;
        }
        if (id.equals("gargoyle") || id.endsWith("_pillar") || id.startsWith("slate")) {
            return SoundType.STONE;
        }
        return SoundType.METAL;
    }



    @Test
    public void noDecorBlockEverRequiresTheCorrectToolForDrops() {
        rusticBlocks().forEach((id, block) -> assertFalse(block.defaultBlockState().requiresCorrectToolForDrops(),
                id + " would stop dropping its item when mined by hand"));
    }





    @Test
    public void pillarsStoreTheirAxisTheWayTheLegacyMetadataDid() {
        Map<String, Block> registered = rusticBlocks();
        for (String type : List.of("stone", "andesite", "diorite", "granite", "slate")) {
            Block pillar = registered.get(type + "_pillar");
            assertEquals(List.of(RotatedPillarBlock.AXIS), List.copyOf(pillar.getStateDefinition().getProperties()),
                    type + "_pillar properties");
            assertEquals(Direction.Axis.Y, pillar.defaultBlockState().getValue(RotatedPillarBlock.AXIS),
                    type + "_pillar default, which was legacy metadata 0");

            assertEquals(List.of(Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z),
                    new ArrayList<>(RotatedPillarBlock.AXIS.getPossibleValues()),
                    type + "_pillar lost an axis, so it can no longer lie on its side");
        }
    }

    @Test
    public void slabsAndStairsCarryTheStatesThatReplacedTheHalfAndDoubleMetadata() {
        Map<String, Block> registered = rusticBlocks();
        for (String slab : List.of("slate_roof_slab", "slate_brick_slab")) {
            SlabBlock block = (SlabBlock) registered.get(slab);
            assertEquals(SlabType.BOTTOM, block.defaultBlockState().getValue(BlockStateProperties.SLAB_TYPE),
                    slab + " defaults to the bottom half, which was metadata 0");
            assertSame(BlockStateProperties.SLAB_TYPE, block.getStateDefinition().getProperty("type"),
                    slab + " must use vanilla's slab type property");
            assertTrue(BlockStateProperties.SLAB_TYPE.getPossibleValues().contains(SlabType.DOUBLE),
                    slab + " lost the double slab state");
        }
        for (String stairs : List.of("stairs_slate_roof", "stairs_slate_brick")) {
            assertEquals(4, registered.get(stairs).getStateDefinition().getProperties().size(),
                    stairs + " should have facing, half, shape and waterlogged");
        }
    }



    @Test
    public void theDiagonalWallStoresOneHorizontalFacing() {
        BlockClayWallDiag diag = (BlockClayWallDiag) rusticBlocks().get("clay_wall_diag");

        assertEquals(Direction.NORTH, diag.defaultBlockState().getValue(BlockRotatable.FACING),
                "the legacy setDefaultState put the wall facing north");
        assertEquals(List.of(BlockRotatable.FACING), List.copyOf(diag.getStateDefinition().getProperties()),
                "the diag wall must expose exactly one property, facing");
        assertSame(BlockStateProperties.HORIZONTAL_FACING, diag.getStateDefinition().getProperty("facing"),
                "the legacy PropertyDirection.create(\"facing\", Plane.HORIZONTAL) is HORIZONTAL_FACING");
        assertEquals(List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST),
                new ArrayList<>(BlockStateProperties.HORIZONTAL_FACING.getPossibleValues()),
                "facing must stay horizontal");


        assertEquals(Direction.EAST, diag.rotate(diag.defaultBlockState(), null, null, Rotation.CLOCKWISE_90)
                .getValue(BlockRotatable.FACING));
        assertEquals(Direction.WEST, diag.rotate(diag.defaultBlockState(), null, null, Rotation.COUNTERCLOCKWISE_90)
                .getValue(BlockRotatable.FACING));
        assertEquals(Direction.SOUTH, diag.rotate(diag.defaultBlockState(), null, null, Rotation.CLOCKWISE_180)
                .getValue(BlockRotatable.FACING));
    }





    @Test
    public void diagonalWallPlacementReproducesTheLegacyClickArithmetic() {
        BlockClayWallDiag diag = (BlockClayWallDiag) rusticBlocks().get("clay_wall_diag");

        assertEquals(Direction.EAST, diagFacing(diag, Direction.UP, 0.75D, 0.5D, 0.5D));
        assertEquals(Direction.WEST, diagFacing(diag, Direction.UP, 0.25D, 0.5D, 0.5D));
        assertEquals(Direction.SOUTH, diagFacing(diag, Direction.UP, 0.5D, 0.5D, 0.75D));
        assertEquals(Direction.NORTH, diagFacing(diag, Direction.UP, 0.5D, 0.5D, 0.25D));

        assertEquals(Direction.WEST, diagFacing(diag, Direction.UP, 0.5D, 0.5D, 0.5D));

        assertEquals(Direction.EAST, diagFacing(diag, Direction.DOWN, 0.9D, 0.1D, 0.4D));

        assertEquals(Direction.WEST, diagFacing(diag, Direction.UP, 0.1D, 0.5D, 0.7D));

        for (Direction side : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            assertEquals(side.getOpposite(), diagFacing(diag, side, 0.5D, 0.5D, 0.5D), "clicked " + side);
        }
    }

    private static Direction diagFacing(BlockClayWallDiag diag, Direction clickedFace, double hitX, double hitY,
                                        double hitZ) {
        return diag.getStateForAdvancedRotationPlacement(diag.defaultBlockState(), BlockRotatable.FACING,
                clickedFace, hitX, hitY, hitZ).getValue(BlockRotatable.FACING);
    }





    @Test
    public void paintedWoodBurnsWithTheLegacyFireNumbersAndNothingElseDoes() {
        EXPECTED_ALL.forEach((id, ignored) -> {
            Block block = rusticBlocks().get(id);
            BlockState state = block.defaultBlockState();
            int spread = block.getFireSpreadSpeed(state, null, null, Direction.NORTH);
            int burn = block.getFlammability(state, null, null, Direction.NORTH);
            int[] legacy = FIRE.get(id);
            if (legacy == null) {
                assertEquals(0, spread, id + " must not spread fire");
                assertEquals(0, burn, id + " must not burn");
            } else {
                assertEquals(legacy[0], spread, id + " fire spread chance");
                assertEquals(legacy[1], burn, id + " burn chance");
            }
        });


        assertEquals(5, Blocks.OAK_PLANKS.getFireSpreadSpeed(Blocks.OAK_PLANKS.defaultBlockState(), null, null,
                Direction.NORTH));
        assertEquals(20, Blocks.OAK_PLANKS.getFlammability(Blocks.OAK_PLANKS.defaultBlockState(), null, null,
                Direction.NORTH));
    }



    @Test
    public void theDecorTabHoldsEveryBlockOfEveryFamily() {
        List<String> decorIds = EXPECTED_ALL.keySet().stream()
                .filter(id -> !FARMING_TAB_IDS.contains(id))
                .map(id -> "rustic:" + id)
                .toList();
        assertEquals(idsOf(decorIds), decorTabItemIds(), "the DECOR tab is not the legacy decor list");
    }



    @Test
    public void disablingAFamilyOnlyRemovesItFromTheTab() {
        int full = decorTabItemIds().size();
        assertEquals(EXPECTED_ALL.size() - FARMING_TAB_IDS.size(), full, "the tab is not every decor family");
        gateLeavesTheRegistry(RusticConfig.ENABLE_CLAY_WALLS, 3,
                List.of("clay_wall", "clay_wall_cross", "clay_wall_diag"));

        gateLeavesTheRegistry(RusticConfig.ENABLE_CHAIRS, 8,
                List.of("chair_oak", "chair_big_oak", "chair_birch", "chair_spruce", "chair_acacia",
                        "chair_jungle", "chair_olive", "chair_ironwood"));
        gateLeavesTheRegistry(RusticConfig.ENABLE_LATTICE, 1, List.of("iron_lattice"));
        assertEquals(full, decorTabItemIds().size(), "the tab must come back with every flag restored");
    }

    
    private static void gateLeavesTheRegistry(ModConfigSpec.BooleanValue gate, int removed, List<String> ids) {
        int before = decorTabItemIds().size();
        assertTrue(gate.get(), "the shipped default of " + gate + " is on");
        try {
            gate.set(false);
            List<String> without = decorTabItemIds();
            assertEquals(before - removed, without.size(), ids + " should have left the tab");
            for (String id : ids) {
                assertFalse(without.contains("rustic:" + id), id + " stayed in the tab");
                assertNotNull(rusticBlocks().get(id), id + " must stay registered for old worlds");
            }
        } finally {
            gate.set(true);
        }
        assertEquals(before, decorTabItemIds().size(), "the tab must come back with the flag restored");
    }

    
    private static List<String> decorTabItemIds() {
        CreativeModeTab decor = RusticCreativeTabs.DECOR.get();
        decor.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        return decor.getDisplayItems().stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                .filter(key -> !machineIds().contains(key.getPath()))
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }



    @Test
    public void theFarmingTabStillHoldsTheAgricultureHalfOfTheWoodFamilies() {
        CreativeModeTab farming = RusticCreativeTabs.FARMING.get();
        farming.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        List<String> shown = farming.getDisplayItems().stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .toList();
        for (String id : FARMING_TAB_IDS) {
            assertTrue(shown.contains("rustic:" + id),
                    "the wood-family agriculture half left the FARMING tab: rustic:" + id);
        }

    }





    @Test
    public void datagenProducedAStateModelAndLootTableForEveryBlock() {
        EXPECTED_ALL.keySet().forEach(id -> {
            json("/assets/rustic/blockstates/" + id + ".json");
            json("/assets/rustic/models/item/" + id + ".json");

            JsonObject loot = json("/data/rustic/loot_table/blocks/" + id + ".json");
            assertEquals("rustic:blocks/" + id, loot.get("random_sequence").getAsString(), id + " loot seed");

            JsonArray entries = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries");
            if (id.startsWith("leaves_")) {

                JsonObject alternatives = entries.get(0).getAsJsonObject();
                assertEquals("minecraft:alternatives", alternatives.get("type").getAsString(), id);
                JsonArray children = alternatives.getAsJsonArray("children");
                assertEquals("rustic:" + id, children.get(0).getAsJsonObject().get("name").getAsString(), id);
                assertEquals("rustic:sapling_" + id.substring("leaves_".length()),
                        children.get(1).getAsJsonObject().get("name").getAsString(), id);
                return;
            }
            String dropped = entries.get(0).getAsJsonObject().get("name").getAsString();
            assertEquals("rustic:" + id, dropped, id + " must drop itself, as 1.12.2's default did");
        });
    }



    @Test
    public void aDoubleSlabDropsTwoOfItself() {
        for (String slab : List.of("slate_roof_slab", "slate_brick_slab")) {
            JsonObject loot = json("/data/rustic/loot_table/blocks/" + slab + ".json");
            JsonArray functions = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("functions");
            JsonObject setCount = null;
            for (JsonElement element : functions) {
                JsonObject function = element.getAsJsonObject();
                if ("minecraft:set_count".equals(function.get("function").getAsString())) {
                    setCount = function;
                }
            }
            assertNotNull(setCount, slab + " has no set_count function, so a double slab would drop one item");
            assertEquals(2.0D, setCount.get("count").getAsDouble(), slab);
            JsonObject condition = setCount.getAsJsonArray("conditions").get(0).getAsJsonObject();
            assertEquals("minecraft:block_state_property", condition.get("condition").getAsString());
            assertEquals("rustic:" + slab, condition.get("block").getAsString(),
                    "the condition names the block by registry id, namespace included");
            assertEquals("double", condition.getAsJsonObject("properties").get("type").getAsString());
        }
    }



    @Test
    public void theMineableTagsSayWhichToolTheLegacyMaterialImplied() {

        assertEquals(idsOf(TOOLS.keySet()), idsOf(rusticBlocks().keySet()),
                "a registered block has no golden tool, so the mineable tags cannot be checked");

        Map<String, List<String>> byTool = new TreeMap<>();
        TOOLS.forEach((id, tool) -> {
            if (tool != null) {
                byTool.computeIfAbsent(tool, key -> new ArrayList<>()).add("rustic:" + id);
            }
        });
        for (String tool : List.of("axe", "pickaxe", "shovel")) {
            assertEquals(idsOf(byTool.getOrDefault(tool, List.of())), distinctTag("mineable/" + tool),
                    "mineable/" + tool + ": 1.12.2's Material decided which tool was effective");
        }

        for (String tool : List.of("axe", "pickaxe", "shovel")) {
            assertFalse(distinctTag("mineable/" + tool).contains("rustic:rope"),
                    "rustic:rope is in mineable/" + tool);
        }
    }

    
    @Test
    public void theSlabAndStairShapeTagsListTheRusticOnes() {

        assertEquals(idsOf(List.of("rustic:slate_roof_slab", "rustic:slate_brick_slab", "rustic:olive_slab",
                "rustic:ironwood_slab")), distinctTag("slabs"), "slabs");
        assertEquals(idsOf(List.of("rustic:stairs_slate_roof", "rustic:stairs_slate_brick",
                "rustic:stairs_olive", "rustic:stairs_ironwood")), distinctTag("stairs"), "stairs");
        assertEquals(idsOf(List.of("rustic:sapling_olive", "rustic:sapling_ironwood")),
                distinctTag("saplings"), "treeSapling");
        assertEquals(idsOf(List.of("rustic:fence_olive", "rustic:fence_ironwood")),
                distinctTag("wooden_fences"), "fenceWood");
    }



    private static Block vanillaSource(String id) {
        if (id.startsWith("log_")) {
            return Blocks.OAK_LOG;
        }
        if (id.startsWith("leaves_")) {
            return Blocks.OAK_LEAVES;
        }
        return Blocks.OAK_SAPLING;
    }





    @Test
    public void theRopeStoresOneAxisAndItsSag() {
        BlockRope rope = (BlockRope) rusticBlocks().get("rope");

        assertEquals(List.of(BlockRopeBase.AXIS, BlockRopeBase.DANGLE),
                List.copyOf(rope.getStateDefinition().getProperties()), "rope properties");
        assertEquals(Direction.Axis.Y, rope.defaultBlockState().getValue(BlockRopeBase.AXIS),
                "a rope defaults to hanging straight down, which was legacy metadata 0");
        assertFalse(rope.defaultBlockState().getValue(BlockRopeBase.DANGLE), "a fresh rope does not sag");
        assertEquals(7, json("/assets/rustic/blockstates/rope.json").getAsJsonArray("multipart").size(),
                "the rope blockstate lost one of the legacy getActualState's answers");


        List<String> climbable = new ArrayList<>(List.of("rustic:chain", "rustic:chain_gold", "rustic:rope"));
        if (RusticBlocks.silverDecorEnabled()) {
            climbable.add("rustic:chain_silver");
        }
        assertEquals(idsOf(climbable), distinctTag("climbable"), "every BlockRopeBase line was a ladder");
        assertFalse(distinctTag("climbable").contains("rustic:iron_lattice"),
                "the lattice is not a rope base and never answered the ladder question");
    }



    @Test
    public void theLatticeStoresSixArmsAndItsLeavesFlag() {
        BlockLattice lattice = (BlockLattice) rusticBlocks().get("iron_lattice");

        List<Property<?>> expectedArms = List.<Property<?>>of(BlockLattice.DOWN, BlockLattice.UP,
                BlockLattice.NORTH, BlockLattice.SOUTH, BlockLattice.WEST, BlockLattice.EAST,
                BlockLattice.LEAVES);
        List<String> expectedNames = expectedArms.stream().map(Property::getName).sorted().toList();
        List<String> actualNames = lattice.getStateDefinition().getProperties().stream()
                .map(Property::getName).sorted().toList();
        assertEquals(expectedNames, actualNames, "lattice properties, the legacy six arms plus the foliage");

        BlockState state = lattice.defaultBlockState();
        assertFalse(state.getValue(BlockLattice.LEAVES), "an unplanted lattice is bare");
        for (Property<?> arm : expectedArms) {
            assertFalse(state.getValue((BooleanProperty) arm),
                    "a lone lattice reaches out to nothing: " + arm.getName());
        }
        assertEquals(14, json("/assets/rustic/blockstates/iron_lattice.json").getAsJsonArray("multipart").size(),
                "the lattice blockstate is not the legacy extended state's answer set");
    }



    @Test
    public void theFertileSoilTakesEveryGroundPlantAndRefusesTheTwoItDidIn1122() {
        BlockFertileSoil soil = (BlockFertileSoil) rusticBlocks().get("fertile_soil");
        BlockPos pos = BlockPos.ZERO;

        assertTrue(soil.isFertile(soil.defaultBlockState(), null, pos),
                "1.12.2's isFertile answered true unconditionally, which is what the crop bonus reads");

        for (BlockState plant : List.of(Blocks.OAK_SAPLING.defaultBlockState(), Blocks.WHEAT.defaultBlockState(),
                Blocks.CACTUS.defaultBlockState(), Blocks.SUGAR_CANE.defaultBlockState(),
                Blocks.BROWN_MUSHROOM.defaultBlockState())) {
            assertSame(TriState.TRUE, sustain(soil, pos, plant),
                    BuiltInRegistries.BLOCK.getKey(plant.getBlock()).getPath());
        }
        assertSame(TriState.FALSE, sustain(soil, pos, Blocks.NETHER_WART.defaultBlockState()), "nether wart");
        assertSame(TriState.FALSE, sustain(soil, pos, Blocks.LILY_PAD.defaultBlockState()), "waterlily");

        assertSame(TriState.DEFAULT, sustain(soil, pos, Blocks.STONE.defaultBlockState()), "not a plant at all");
    }

    private static TriState sustain(BlockFertileSoil soil, BlockPos pos, BlockState plant) {
        return soil.canSustainPlant(soil.defaultBlockState(), (BlockGetter) null, pos,
                Direction.UP, plant);
    }



    @Test
    public void theGeneratedDiagonalWallBlockstateKeepsTheLegacyRotationTable() {
        JsonObject variants = json("/assets/rustic/blockstates/clay_wall_diag.json").getAsJsonObject("variants");

        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("facing=north", 0);
        expected.put("facing=east", 90);
        expected.put("facing=south", 180);
        expected.put("facing=west", 270);
        assertEquals(expected.keySet(), variants.keySet(), "the diag wall lost or gained a facing variant");

        expected.forEach((variant, yRot) -> {
            JsonElement model = variants.get(variant);

            JsonObject single;
            if (model.isJsonArray()) {
                assertEquals(1, model.getAsJsonArray().size(), variant + " should resolve to one model");
                single = model.getAsJsonArray().get(0).getAsJsonObject();
            } else {
                single = model.getAsJsonObject();
            }

            assertEquals(yRot.intValue(), single.has("y") ? single.get("y").getAsInt() : 0,
                    variant + " y rotation");
        });
    }



    @Test
    public void theGeneratedDiagonalWallModelKeepsTheLegacyUvFlipOnWestOnly() {
        JsonObject model = json("/assets/rustic/models/block/clay_wall_diag.json");
        JsonObject faces = model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");

        assertEquals(List.of(16.0D, 0.0D, 0.0D, 16.0D), numbers(faces.getAsJsonObject("west").getAsJsonArray("uv")));
        assertEquals(List.of(0.0D, 0.0D, 16.0D, 16.0D), numbers(faces.getAsJsonObject("east").getAsJsonArray("uv")),
                "east must keep the default uv, or the flip cancels out");
        assertEquals("rustic:block/clay_wall", model.getAsJsonObject("textures").get("0").getAsString());
        assertEquals("rustic:block/clay_wall_diag", model.getAsJsonObject("textures").get("1").getAsString());
        assertEquals("rustic:block/clay_wall_diag", model.getAsJsonObject("textures").get("particle").getAsString());
    }

    private static List<Double> numbers(JsonArray array) {
        List<Double> out = new ArrayList<>();
        array.forEach(element -> out.add(element.getAsDouble()));
        return out;
    }

    private static List<String> tag(String name) {
        JsonObject json = json("/data/minecraft/tags/block/" + name + ".json");
        List<String> ids = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("values")) {
            ids.add(element.isJsonObject() ? element.getAsJsonObject().get("id").getAsString() : element.getAsString());
        }
        return ids;
    }



    private static JsonObject json(String classpathResource) {
        try (Reader reader = new InputStreamReader(resourceStream(classpathResource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            return fail("could not read " + classpathResource, e);
        }
    }

    private static java.io.InputStream resourceStream(String resource) {
        java.io.InputStream in = RusticDecorRegistrationTest.class.getResourceAsStream(resource);
        assertNotNull(in, "missing resource on the classpath: " + resource
                + " - if this block is new, re-run runData rather than writing the file by hand");
        return in;
    }

    private static List<String> idsOf(java.util.Collection<String> prefixedIds) {
        return prefixedIds.stream().sorted().toList();
    }



    private static List<String> distinctTag(String name) {
        return tag(name).stream()
                .filter(id -> !machineIds().contains(id.substring(id.lastIndexOf(':') + 1)))
                .filter(id -> !farmIds().contains(id.substring(id.lastIndexOf(':') + 1)))
                .filter(id -> !phase6Ids().contains(id.substring(id.lastIndexOf(':') + 1)))
                .distinct().sorted().toList();
    }

    
    private static Map<String, Block> rusticBlocks() {
        Map<String, Block> out = new TreeMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key != null && Rustic.MODID.equals(key.getNamespace()) && !machineIds().contains(key.getPath())
                    && !farmIds().contains(key.getPath()) && !phase6Ids().contains(key.getPath())
                    && !phase7Ids().contains(key.getPath())) {
                out.put(key.getPath(), block);
            }
        }
        return out;
    }

    private static Map<String, Item> rusticItems() {
        Map<String, Item> out = new TreeMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (key != null && Rustic.MODID.equals(key.getNamespace()) && !machineIds().contains(key.getPath())
                    && !farmIds().contains(key.getPath()) && !phase6Ids().contains(key.getPath())
                    && !phase7Ids().contains(key.getPath())) {
                out.put(key.getPath(), item);
            }
        }
        return out;
    }



    private static java.util.Set<String> machineIds() {
        return RusticMachineBlocks.entries().stream()
                .map(holder -> holder.getId().getPath())
                .collect(java.util.stream.Collectors.toSet());
    }



    private static java.util.Set<String> farmIds() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        RusticFarmBlocks.entries().forEach(holder -> ids.add(holder.getId().getPath()));
        ids.addAll(List.of("tomato", "chili_pepper", "ghost_pepper", "tomato_seeds", "chili_pepper_seeds",
                "grapes", "wildberries", "bee"));
        return ids;
    }



    private static java.util.Set<String> phase6Ids() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        RusticFluidBlocks.entries().forEach(holder -> ids.add(holder.getId().getPath()));
        ids.addAll(List.of("olive_oil_bucket", "ironberry_juice_bucket", "wildberry_juice_bucket",
                "grape_juice_bucket", "apple_juice_bucket", "ale_wort_bucket", "honey_bucket",
                "golden_apple_juice_bucket", "vanta_oil_bucket", "fluid_bottle", "elixir",
                "olives", "ironberries"));
        return ids;
    }



    private static java.util.Set<String> phase7Ids() {
        return java.util.Set.of("honeycomb", "beeswax", "tallow", "dust_tiny_iron", "dust_gold",
                "apple_seeds", "grape_seeds");
    }
}
