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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.common.ModConfigSpec;
import rustic.config.RusticConfig;
import rustic.content.RusticCreativeTabs;
import rustic.content.blocks.BlockClayWallDiag;
import rustic.content.blocks.BlockRotatable;
import rustic.content.blocks.RusticBlocks;

/**
 * Phase 2 characterisation test for the decor slice: the 33 blocks 1.12.2 built in
 * {@code ModBlocks.init()} behind the {@code ENABLE_SLATE / ENABLE_PILLARS / ENABLE_CLAY_WALLS /
 * ENABLE_PAINTED_WOOD} flags, plus their items, their numbers, their block states and the datagen
 * output that replaced the hand-written asset files.
 *
 * <p>The expected values are hand-listed below from the legacy source rather than read back out of
 * {@link RusticBlocks}, so that a rename or a dropped block fails here instead of quietly agreeing
 * with itself. The one deliberate disagreement is the id list: 1.12.2 registered
 * {@code slate_roof_doubleslab} and {@code slate_brick_doubleslab} as hidden blocks behind
 * {@code ItemBlockSlabBase}, and 1.21 stores {@code SlabType.DOUBLE} in the slab itself, so those two
 * ids must be absent now - asserted in {@link #theLegacyHiddenDoubleSlabsAreGone()}.
 *
 * <p>Runs in ModDevGradle's FML unit-test JVM, which is what makes {@code BuiltInRegistries}
 * populated and the creative tab's {@code displayItems} predicate readable. Resource assertions read
 * from the classpath, because {@code src/generated/resources} is a main source directory: they check
 * the committed datagen output, so forgetting to re-run {@code runData} after adding a block fails
 * here rather than in someone's game.
 */
public class RusticDecorRegistrationTest {

    /** What 1.12.2 made a block out of, and how hard it was. */
    private record Expected(Class<? extends Block> type, float hardness) {
    }

    /**
     * The golden list. Hardness comes from the legacy {@code setHardness} calls ({@code ModBlocks}
     * L201-222 for slate and clay, L255-286 for painted wood, {@code BlockPillar} L22,
     * {@code BlockSlabBase} L30), and every one of those blocks reported an explosion resistance of
     * <em>zero</em>: 1.12.2 kept resistance in its own {@code blockResistance} field, the {@code Material}
     * constructor never touched it, and nothing in this family called {@code setResistance}. That is why
     * the resistance is asserted as an absolute value below and not as a multiple of hardness.
     *
     * <p>The two stair blocks used to be the one family whose legacy numbers were not readable from
     * Rustic's own source - {@code new BlockStairsBase(SLATE_ROOF.getDefaultState(), ...)} inherits from
     * vanilla. Verified from the 1.12.2 bytecode: {@code BlockStairs}'s constructor copies the source
     * block's {@code blockHardness} verbatim and its raw {@code blockResistance} verbatim, so the stairs
     * really are slate's 2.0F / 0.0F.
     */
    private static final Map<String, Expected> EXPECTED = expected();

    private static Map<String, Expected> expected() {
        Map<String, Expected> expected = new LinkedHashMap<>();
        // BlockPillar(type) -> "<type>_pillar", Material.ROCK, hardness 2.0
        for (String type : List.of("stone", "andesite", "diorite", "granite", "slate")) {
            expected.put(type + "_pillar", new Expected(RotatedPillarBlock.class, 2.0F));
        }
        // Material.ROCK cubes, hardness 2.0
        for (String slate : List.of("slate", "slate_roof", "slate_tile", "slate_brick", "slate_chiseled")) {
            expected.put(slate, new Expected(Block.class, 2.0F));
        }
        expected.put("stairs_slate_roof", new Expected(StairBlock.class, 2.0F));
        expected.put("slate_roof_slab", new Expected(SlabBlock.class, 2.0F));
        expected.put("stairs_slate_brick", new Expected(StairBlock.class, 2.0F));
        expected.put("slate_brick_slab", new Expected(SlabBlock.class, 2.0F));
        // Material.CLAY with SoundType.GROUND, hardness 1.0
        expected.put("clay_wall", new Expected(Block.class, 1.0F));
        expected.put("clay_wall_cross", new Expected(Block.class, 1.0F));
        expected.put("clay_wall_diag", new Expected(BlockClayWallDiag.class, 1.0F));
        // sixteen Material.WOOD colours, hardness 2.0, spelled the way 1.12.2 spelled them
        for (String colour : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
                "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            expected.put("painted_wood_" + colour, new Expected(Block.class, 2.0F));
        }
        return Map.copyOf(expected);
    }

    /** The legacy ids that must no longer exist, and why. */
    private static final List<String> REMOVED_IDS = List.of("slate_roof_doubleslab", "slate_brick_doubleslab");

    // ---------------------------------------------------------------- registration

    @Test
    public void everyLegacyDecorBlockIsRegisteredUnderItsLegacyIdWithALegacyShapedClass() {
        Map<String, Block> registered = rusticBlocks();

        assertEquals(EXPECTED.size(), registered.size(),
                "rustic block count changed without a baseline update: expected " + EXPECTED.keySet()
                        + " but found " + registered.keySet());

        for (Map.Entry<String, Expected> entry : EXPECTED.entrySet()) {
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

    /**
     * 1.12.2 registered an {@code ItemBlock} for every block from inside {@code BlockBase.register()}
     * with the block's own registry name, so a block with no item was impossible. {@code RusticItems}
     * reproduces that loop, and this is the assertion that keeps it honest - including for the two slab
     * families, whose legacy item was the special {@code ItemBlockSlabBase} that swapped in the hidden
     * double slab and is replaced by a plain {@link BlockItem} here.
     */
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

    // ---------------------------------------------------------------- the numbers 1.12.2 set

    /**
     * Hardness copies straight across, but resistance has no equivalent call in 1.12.2's
     * {@code setHardness(x)}, so it has to be restated: a block that never called
     * {@code setResistance} reported {@code getExplosionResistance() == 0} (the field defaulted to 0 and
     * the getter divided it by 5), while {@code Properties.strength(x)} sets the resistance <em>equal
     * to</em> the hardness. Writing {@code strength(2.0F)} for slate roof would therefore have made every
     * block in the slice blast-proof, which no screenshot would ever show.
     */
    @Test
    public void hardnessAndExplosionResistanceFollowTheLegacyNumbers() {
        EXPECTED.forEach((id, expected) -> {
            Block block = rusticBlocks().get(id);
            assertNotNull(block, id);
            assertEquals(expected.hardness(), block.defaultDestroyTime(), 0.0001F, id + " hardness");
            assertEquals(0.0F, block.getExplosionResistance(block.defaultBlockState(), null, null, null),
                    0.0001F, id + " resistance");
        });
    }

    /**
     * The sound came from the {@code Material} in 1.12.2, with two explicit overrides in this slice:
     * the clay walls' {@code SoundType.GROUND} and painted wood's {@code SoundType.WOOD}. 1.12.2's
     * GROUND played the gravel samples, which 1.21 simply calls {@link SoundType#GRAVEL}.
     */
    @Test
    public void soundsMatchTheLegacyMaterialsAndTheirOverrides() {
        EXPECTED.forEach((id, expected) -> {
            Block block = rusticBlocks().get(id);
            SoundType sound = block.getSoundType(block.defaultBlockState(), null, null, null);
            if (id.startsWith("painted_wood_")) {
                assertSame(SoundType.WOOD, sound, id);
            } else if (id.startsWith("clay_wall")) {
                assertSame(SoundType.GRAVEL, sound, id);
            } else {
                assertSame(SoundType.STONE, sound, id);
            }
        });
    }

    /**
     * The legacy blocks never set a harvest level, so they always dropped their item. 1.21 can say
     * "needs the right tool" two ways - the property and an {@code incorrect_for_*} tag - and neither
     * may appear, or hand-mining a slate block would start losing the item.
     */
    @Test
    public void noDecorBlockEverRequiresTheCorrectToolForDrops() {
        rusticBlocks().forEach((id, block) -> assertFalse(block.defaultBlockState().requiresCorrectToolForDrops(),
                id + " would stop dropping its item when mined by hand"));
    }

    // ---------------------------------------------------------------- block states, which replaced metadata

    /**
     * {@code BlockPillar}'s hand-written axis property, its {@code getMetaFromState} pair and its
     * "rotate around the clicked face" placement are vanilla {@link RotatedPillarBlock}'s, including
     * the Y default that legacy metadata 0 stood for.
     */
    @Test
    public void pillarsStoreTheirAxisTheWayTheLegacyMetadataDid() {
        Map<String, Block> registered = rusticBlocks();
        for (String type : List.of("stone", "andesite", "diorite", "granite", "slate")) {
            Block pillar = registered.get(type + "_pillar");
            assertEquals(List.of(RotatedPillarBlock.AXIS), List.copyOf(pillar.getStateDefinition().getProperties()),
                    type + "_pillar properties");
            assertEquals(Direction.Axis.Y, pillar.defaultBlockState().getValue(RotatedPillarBlock.AXIS),
                    type + "_pillar default, which was legacy metadata 0");
            // 1.12.2: super.getStateForPlacement(...).withProperty(AXIS, facing.getAxis())
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

    /**
     * The diagonal wall's {@code PropertyDirection.create("facing", Plane.HORIZONTAL)} is
     * {@link BlockStateProperties#HORIZONTAL_FACING}, whose four values and NORTH default (legacy
     * {@code setDefaultState}) are what the generated blockstate variants key off.
     */
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

        // 1.12.2's withRotation(state, rot) was rot.rotate(facing); structure rotation must agree.
        assertEquals(Direction.EAST, diag.rotate(diag.defaultBlockState(), null, null, Rotation.CLOCKWISE_90)
                .getValue(BlockRotatable.FACING));
        assertEquals(Direction.WEST, diag.rotate(diag.defaultBlockState(), null, null, Rotation.COUNTERCLOCKWISE_90)
                .getValue(BlockRotatable.FACING));
        assertEquals(Direction.SOUTH, diag.rotate(diag.defaultBlockState(), null, null, Rotation.CLOCKWISE_180)
                .getValue(BlockRotatable.FACING));
    }

    // ---------------------------------------------------------------- placement arithmetic

    /**
     * The one genuinely custom piece of logic in the slice, and a pure function, so the legacy
     * arithmetic can be replayed exactly: a top or bottom click has no facing to read, so 1.12.2
     * looked at how far from the centre of the face the click landed on each axis, took whichever
     * offset was larger, and used its sign.
     */
    @Test
    public void diagonalWallPlacementReproducesTheLegacyClickArithmetic() {
        BlockClayWallDiag diag = (BlockClayWallDiag) rusticBlocks().get("clay_wall_diag");

        assertEquals(Direction.EAST, diagFacing(diag, Direction.UP, 0.75D, 0.5D, 0.5D));
        assertEquals(Direction.WEST, diagFacing(diag, Direction.UP, 0.25D, 0.5D, 0.5D));
        assertEquals(Direction.SOUTH, diagFacing(diag, Direction.UP, 0.5D, 0.5D, 0.75D));
        assertEquals(Direction.NORTH, diagFacing(diag, Direction.UP, 0.5D, 0.5D, 0.25D));
        // the exact centre is the tie case: both offsets are 0, so the x branch wins and is not positive
        assertEquals(Direction.WEST, diagFacing(diag, Direction.UP, 0.5D, 0.5D, 0.5D));
        // a bottom click runs the same arithmetic, which is what made the legacy branch axis-test
        assertEquals(Direction.EAST, diagFacing(diag, Direction.DOWN, 0.9D, 0.1D, 0.4D));
        // x wins when it is the larger offset, even though z is also off-centre
        assertEquals(Direction.WEST, diagFacing(diag, Direction.UP, 0.1D, 0.5D, 0.7D));
        // a side click says which way the player was standing, so the wall turns to meet them
        for (Direction side : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            assertEquals(side.getOpposite(), diagFacing(diag, side, 0.5D, 0.5D, 0.5D), "clicked " + side);
        }
    }

    private static Direction diagFacing(BlockClayWallDiag diag, Direction clickedFace, double hitX, double hitY,
                                        double hitZ) {
        return diag.getStateForAdvancedRotationPlacement(diag.defaultBlockState(), BlockRotatable.FACING,
                clickedFace, hitX, hitY, hitZ).getValue(BlockRotatable.FACING);
    }

    // ---------------------------------------------------------------- flammability

    /**
     * {@code Blocks.FIRE.setFireInfo(paintedWood, 5, 20)} per block in 1.12.2. 1.21.1 keeps that table
     * on the fire block, and NeoForge reads it back as {@code getFireSpreadSpeed} = the encouragement
     * and {@code getFlammability} = the flammability, which is why the pair is asserted in that order.
     * The stone and clay families were never flammable and must not become so.
     */
    @Test
    public void paintedWoodBurnsWithTheLegacyFireNumbersAndNothingElseDoes() {
        EXPECTED.forEach((id, ignored) -> {
            Block block = rusticBlocks().get(id);
            BlockState state = block.defaultBlockState();
            int spread = block.getFireSpreadSpeed(state, null, null, Direction.NORTH);
            int burn = block.getFlammability(state, null, null, Direction.NORTH);
            if (id.startsWith("painted_wood_")) {
                assertEquals(5, spread, id + " fire spread chance");
                assertEquals(20, burn, id + " burn chance");
            } else {
                assertEquals(0, spread, id + " must not spread fire");
                assertEquals(0, burn, id + " must not burn");
            }
        });

        // the same pair vanilla uses for planks, which is what the legacy numbers lined up with
        assertEquals(5, Blocks.OAK_PLANKS.getFireSpreadSpeed(Blocks.OAK_PLANKS.defaultBlockState(), null, null,
                Direction.NORTH));
        assertEquals(20, Blocks.OAK_PLANKS.getFlammability(Blocks.OAK_PLANKS.defaultBlockState(), null, null,
                Direction.NORTH));
    }

    // ---------------------------------------------------------------- creative tab

    @Test
    public void theDecorTabHoldsEveryBlockOfEveryFamily() {
        assertEquals(idsOf(EXPECTED.keySet().stream().map(id -> "rustic:" + id).toList()), decorTabItemIds(),
                "the DECOR tab is not the legacy decor list");
    }

    /**
     * The {@code enable_*} flags used to decide whether a family was registered at all, which is why
     * turning one off in an existing world deleted the blocks. Now they only gate the tab, so a disabled
     * family must vanish from the creative menu while staying in the registry.
     */
    @Test
    public void disablingAFamilyOnlyRemovesItFromTheTab() {
        ModConfigSpec.BooleanValue gate = RusticConfig.ENABLE_CLAY_WALLS;
        assertTrue(gate.get(), "the shipped default is on");
        try {
            gate.set(false);
            List<String> withoutClay = decorTabItemIds();
            assertEquals(30, withoutClay.size(), "three clay walls should have left the tab");
            for (String id : List.of("clay_wall", "clay_wall_cross", "clay_wall_diag")) {
                assertFalse(withoutClay.contains("rustic:" + id), id + " stayed in the tab");
                assertNotNull(rusticBlocks().get(id), id + " must stay registered for old worlds");
            }
        } finally {
            gate.set(true);
        }
        assertEquals(33, decorTabItemIds().size(), "the tab must come back with the flag restored");
    }

    /** Rebuilds the tab's contents the way the creative menu does, then maps them to ids. */
    private static List<String> decorTabItemIds() {
        CreativeModeTab decor = RusticCreativeTabs.DECOR.get();
        decor.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        return decor.getDisplayItems().stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .sorted()
                .toList();
    }

    @Test
    public void theFarmingAndAlchemyTabsExistAndAreStillEmpty() {
        // Phase 5 and Phase 6 own their contents; the point here is that the three legacy tabs all
        // registered, so the menu does not lose the two families whose blocks are not ported yet.
        assertNotNull(RusticCreativeTabs.FARMING.get());
        assertNotNull(RusticCreativeTabs.ALCHEMY.get());
        for (CreativeModeTab tab : List.of(RusticCreativeTabs.FARMING.get(), RusticCreativeTabs.ALCHEMY.get())) {
            tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
            assertEquals(List.of(), tab.getDisplayItems().stream()
                            .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                            .filter(id -> id.startsWith("rustic:"))
                            .toList(),
                    tab.getDisplayName().getString() + " already has rustic content, which the phase list does not "
                            + "expect");
        }
    }

    // ---------------------------------------------------------------- datagen output

    /**
     * Every block needs a blockstate, an item model and a loot table or it renders as the missing-texture
     * cube and drops nothing. All three are generated, so this is the check that they were generated -
     * re-run {@code runData} when it fails, do not hand-write the files.
     */
    @Test
    public void datagenProducedAStateModelAndLootTableForEveryBlock() {
        EXPECTED.keySet().forEach(id -> {
            json("/assets/rustic/blockstates/" + id + ".json");
            json("/assets/rustic/models/item/" + id + ".json");

            JsonObject loot = json("/data/rustic/loot_table/blocks/" + id + ".json");
            String dropped = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString();
            assertEquals("rustic:" + id, dropped, id + " must drop itself, as 1.12.2's default did");
            assertEquals("rustic:blocks/" + id, loot.get("random_sequence").getAsString(), id + " loot seed");
        });
    }

    /**
     * The double-slab replacement in loot terms: 1.12.2's hidden double slab block dropped one of the
     * single slab item, and the legacy {@code ItemBlockSlabBase} turned two slabs into it. 1.21 keeps one
     * block and asks the loot table for two items when the state is {@code type=double}.
     */
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

    /**
     * The {@code minecraft:mineable/*} tags are where the legacy {@code Material} went: ROCK mined with a
     * pickaxe, WOOD with an axe, CLAY with a shovel. A wrong entry here is a silent gameplay change
     * (wrong tool speed), which is exactly the kind of thing the user cannot spot in a screenshot.
     */
    @Test
    public void theMineableTagsSayWhichToolTheLegacyMaterialImplied() {
        // Tag order carries no meaning to the game, so every list is compared sorted.
        List<String> pickaxe = idsOf(tag("mineable/pickaxe"));
        List<String> axe = idsOf(tag("mineable/axe"));
        List<String> shovel = idsOf(tag("mineable/shovel"));

        assertEquals(idsOf(EXPECTED.keySet().stream()
                        .filter(id -> id.startsWith("painted_wood_"))
                        .map(id -> "rustic:" + id)
                        .toList()),
                axe, "axe: 1.12.2 Material.WOOD was axe-mined, and only painted wood uses it here");
        assertEquals(List.of("rustic:clay_wall", "rustic:clay_wall_cross", "rustic:clay_wall_diag"), shovel,
                "shovel: Material.CLAY was spade-mined");
        // the four stone pillars sit behind ENABLE_PILLARS and the slate set behind ENABLE_SLATE, so
        // both families are listed rather than derived from each other
        assertEquals(14, pickaxe.size(), "pickaxe");
        for (String id : List.of("rustic:stone_pillar", "rustic:andesite_pillar", "rustic:diorite_pillar",
                "rustic:granite_pillar", "rustic:slate_pillar", "rustic:slate", "rustic:slate_roof",
                "rustic:slate_tile", "rustic:slate_brick", "rustic:slate_chiseled", "rustic:stairs_slate_roof",
                "rustic:slate_roof_slab", "rustic:stairs_slate_brick", "rustic:slate_brick_slab")) {
            assertTrue(pickaxe.contains(id), id + " is not pickaxe-mined, but was Material.ROCK");
        }
    }

    /** The shape tags 1.12.2 spelled out through the ore dictionary entries {@code slabStone} and {@code stairs}. */
    @Test
    public void theSlabAndStairShapeTagsListTheRusticOnes() {
        assertEquals(List.of("rustic:slate_roof_slab", "rustic:slate_brick_slab"), tag("slabs"), "slabs");
        assertEquals(List.of("rustic:stairs_slate_roof", "rustic:stairs_slate_brick"), tag("stairs"), "stairs");
    }

    /**
     * The rotation the legacy blockstate file wrote by hand: {@code facing=north} meant "no y rotation",
     * east 90, south 180, west 270. Vanilla's {@code toYRot} puts south at 0, so the provider has to add
     * 180 or every diagonal wall in an old world turns a quarter turn.
     */
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
            // A variant with exactly one model is written as a plain object; only a weighted list is an
            // array, so both spellings are accepted but a random-selection weight list is not.
            JsonObject single;
            if (model.isJsonArray()) {
                assertEquals(1, model.getAsJsonArray().size(), variant + " should resolve to one model");
                single = model.getAsJsonArray().get(0).getAsJsonObject();
            } else {
                single = model.getAsJsonObject();
            }
            // y is omitted when it would be 0, which is exactly the legacy "facing=north, no rotation".
            assertEquals(yRot.intValue(), single.has("y") ? single.get("y").getAsInt() : 0,
                    variant + " y rotation");
        });
    }

    /**
     * The model 1.12.2 hand-authored (MrCrayfish's Model Creator, 85 lines) gives its west face
     * {@code uv: [16,0,0,16]} while east keeps {@code [0,0,16,16]}. That is not decoration: with a plain
     * {@code cube} parent the two faces' u axes run in opposite world directions, so the diagonal texture
     * would mirror itself across the block. The generated model has to write the flip, and nothing else.
     */
    @Test
    public void theGeneratedDiagonalWallModelKeepsTheLegacyUvFlipOnWestOnly() {
        JsonObject model = json("/assets/rustic/models/block/clay_wall_diag.json");
        JsonObject faces = model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");

        assertEquals(List.of(16.0D, 0.0D, 0.0D, 16.0D), numbers(faces.getAsJsonObject("west").getAsJsonArray("uv")));
        assertFalse(faces.getAsJsonObject("east").has("uv"),
                "east must keep the default uv, or the flip cancels out");
        assertEquals("rustic:block/clay_wall", model.getAsJsonObject("textures").get("plain").getAsString());
        assertEquals("rustic:block/clay_wall_diag", model.getAsJsonObject("textures").get("diagonal").getAsString());
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

    /**
     * Reads a JSON resource out of the merged main resources, which is where {@code src/generated}
     * lands as well as {@code src/main}. A missing file fails here with its name, because the usual
     * cause is a new block whose datagen was never re-run.
     */
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

    /** The rustic half of the block registry, keyed by path and sorted so failures read as a list. */
    private static Map<String, Block> rusticBlocks() {
        Map<String, Block> out = new TreeMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key != null && Rustic.MODID.equals(key.getNamespace())) {
                out.put(key.getPath(), block);
            }
        }
        return out;
    }

    private static Map<String, Item> rusticItems() {
        Map<String, Item> out = new TreeMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (key != null && Rustic.MODID.equals(key.getNamespace())) {
                out.put(key.getPath(), item);
            }
        }
        return out;
    }
}
