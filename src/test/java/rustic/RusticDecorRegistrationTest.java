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
    private record Expected(Class<? extends Block> type, float hardness, Float resistance) {
        /** Every block that got its numbers from a legacy {@code setHardness} call has a zero resistance. */
        Expected(Class<? extends Block> type, float hardness) {
            this(type, hardness, 0.0F);
        }

        /**
         * A block whose 1.21 properties are copied off a vanilla one with
         * {@code RusticBlockProperties.ofBase}, so hardness, resistance and sound all belong to vanilla
         * and are not re-asserted here: the log, the leaves and the sapling of each wood.
         */
        static Expected derived(Class<? extends Block> type, float hardness) {
            return new Expected(type, hardness, null);
        }

        /** Material.WOOD plus the legacy {@code setResistance(5F)}, which reported as 3.0F. */
        static Expected resistant(Class<? extends Block> type, float hardness) {
            return new Expected(type, hardness, 3.0F);
        }
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

    /**
     * Phase 3b: the wood family, the mounted metal decor, the furniture and the three hanging blocks, with
     * the hardness each legacy constructor set - {@code BlockCandle} 1F, {@code BlockChain} 1F,
     * {@code BlockChandelier} 2F, {@code BlockIronTorch} 0F, {@code BlockLantern} 2.0F,
     * {@code BlockGargoyle} 1.0F, {@code BlockWoodLantern} 1F, {@code BlockChair} 1F, {@code BlockTable} 1F,
     * {@code BlockPlanksRustic} 2F, {@code BlockFenceRustic} 2F, {@code BlockFenceGateRustic} 2F,
     * {@code BlockSlabBase} 2.0F, {@code BlockLattice} 2.0F, {@code BlockRope} 0.5F and
     * {@code BlockFertileSoil} 0.5F. The stairs take the planks' numbers the way vanilla's {@code BlockStairs}
     * copies them, and the log, leaves and sapling are asserted as vanilla's own ({@link #derived}).
     */
    private static final Map<String, Expected> EXPECTED_WOOD = expectedWood();

    private static Map<String, Expected> expectedWood() {
        Map<String, Expected> expected = new LinkedHashMap<>();
        for (String wood : List.of("olive", "ironwood")) {
            expected.put("planks_" + wood, Expected.resistant(Block.class, 2.0F));
            expected.put("fence_" + wood, Expected.resistant(FenceBlock.class, 2.0F));
            expected.put("fence_gate_" + wood, Expected.resistant(FenceGateBlock.class, 2.0F));
            // BlockSlabBase set its own hardness and never touched the resistance field, which is why the
            // slab is wood(2.0F) and not a copy of the planks' properties, unlike the stairs below.
            expected.put(wood + "_slab", new Expected(SlabBlock.class, 2.0F));
            expected.put("stairs_" + wood, Expected.resistant(StairBlock.class, 2.0F));
            expected.put("log_" + wood, Expected.derived(RotatedPillarBlock.class, 2.0F));
            expected.put("leaves_" + wood, Expected.derived(LeavesBlock.class, 0.2F));
            expected.put("sapling_" + wood, Expected.derived(SaplingBlock.class, 0.0F));
        }
        // The mounted metal decor: the iron set, the gold set, the silver set and the gargoyle. 1.12.2
        // registered them in this order, and every one of them called setHardness by hand without ever
        // touching the resistance field, so all of them land on the two-argument Expected.
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
        // The wooden lantern and the eight pairs of furniture, all of Material.WOOD without a resistance
        // call - the lantern at full light, the furniture burning like the planks they are made of.
        expected.put("lantern_wood", new Expected(BlockWoodLantern.class, 1.0F));
        for (String wood : List.of("oak", "big_oak", "birch", "spruce", "acacia", "jungle", "olive", "ironwood")) {
            expected.put("chair_" + wood, new Expected(BlockChair.class, 1.0F));
            expected.put("table_" + wood, new Expected(BlockTable.class, 1.0F));
        }
        // The lattice is iron at 2.0F, the rope is cloth at 0.5F and the fertile soil is dirt at 0.5F.
        expected.put("iron_lattice", new Expected(BlockLattice.class, 2.0F));
        expected.put("rope", new Expected(BlockRope.class, 0.5F));
        expected.put("fertile_soil", new Expected(BlockFertileSoil.class, 0.5F));
        return Map.copyOf(expected);
    }

    /** Every block the port has produced so far, the two golden lists merged. */
    private static final Map<String, Expected> EXPECTED_ALL = merged();

    private static Map<String, Expected> merged() {
        Map<String, Expected> all = new LinkedHashMap<>(EXPECTED);
        all.putAll(EXPECTED_WOOD);
        return Map.copyOf(all);
    }

    /**
     * The ids that go to the agriculture tab rather than the decor one, which is the only difference
     * between the two lists 1.12.2 filled from {@code Rustic.farmingTab}. Phase 5 still owns the tab's
     * contents, so these are the blocks the decor tab must <em>not</em> show.
     */
    private static final List<String> FARMING_TAB_IDS = List.of(
            "log_olive", "leaves_olive", "sapling_olive",
            "log_ironwood", "leaves_ironwood", "sapling_ironwood",
            "fertile_soil");

    /**
     * Which tool 1.12.2's {@code Material} made effective, as the {@code minecraft:mineable/*} tag that
     * replaces it. {@code null} means no tool at all: the rope's {@code MaterialRope} was a custom
     * subclass, and the legacy speed table is keyed on the vanilla constants, so nothing mined it faster
     * than a hand. Every registered block has to appear here, which is what makes the assertion below a
     * baseline rather than a description.
     */
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
        // Material.STONE, whatever the 1.21 mount looks like: the port gives it the pickaxe family.
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

    /** The legacy {@code Blocks.FIRE.setFireInfo(block, x, y)} calls of phase 3b, as {spread, burn}. */
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
            // BlockLogRustic asked for 5 and 5, BlockLeavesRustic for 30 and 60, and the sapling never
            // asked at all - the one plant block in the mod that fire does not take hold of.
            fire.put("log_" + wood, new int[]{5, 5});
            fire.put("leaves_" + wood, new int[]{30, 60});
        }
        for (String wood : List.of("oak", "big_oak", "birch", "spruce", "acacia", "jungle", "olive", "ironwood")) {
            fire.put("chair_" + wood, new int[]{5, 20});
            fire.put("table_" + wood, new int[]{5, 20});
        }
        // The rope's own constructor paired its custom MaterialRope.setBurning() with 20 and 60.
        fire.put("rope", new int[]{20, 60});
        return Collections.unmodifiableMap(fire);
    }


    // ---------------------------------------------------------------- registration

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
        EXPECTED_ALL.forEach((id, expected) -> {
            Block block = rusticBlocks().get(id);
            assertNotNull(block, id);
            assertEquals(expected.hardness(), block.defaultDestroyTime(), 0.0001F, id + " hardness");
            if (expected.resistance() == null) {
                // A block built with ofBase: whatever vanilla's own number is, that is what 1.12.2 handed
                // over by inheriting the source block, so the assertion is the inheritance itself.
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

    /**
     * The sound came from the {@code Material} in 1.12.2, with two explicit overrides in this slice:
     * the clay walls' {@code SoundType.GROUND} and painted wood's {@code SoundType.WOOD}. 1.12.2's
     * GROUND played the gravel samples, which 1.21 simply calls {@link SoundType#GRAVEL}.
     */
    @Test
    public void soundsMatchTheLegacyMaterialsAndTheirOverrides() {
        EXPECTED_ALL.forEach((id, expected) -> {
            Block block = rusticBlocks().get(id);
            SoundType sound = block.getSoundType(block.defaultBlockState(), null, null, null);
            if (expected.resistance() == null) {
                return; // inherits vanilla's sound along with its numbers, so there is nothing Rustic-side to check
            }
            assertSame(expectedSound(id), sound, id);
        });
    }

    /**
     * The sound 1.12.2's {@code Material} answered with, plus the overrides the mod put on top of it:
     * the chandelier's {@code SoundType.ANVIL}, the clay walls' and the fertile soil's {@code GROUND} -
     * which 1.21 simply calls {@link SoundType#GRAVEL} - and the metal mounts' {@code METAL}. Spelled out
     * id by id where a prefix would catch the wrong block, since {@code slate_roof_slab} and
     * {@code olive_slab} end the same way but sound nothing like each other.
     */
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
                    || id.equals(wood + "_slab") || id.equals("stairs_" + wood)) {
                return SoundType.WOOD;
            }
        }
        if (id.startsWith("clay_wall") || id.equals("fertile_soil")) {
            return SoundType.GRAVEL;
        }
        // The slate stairs inherit the slate's Material.ROCK rather than starting with one of their own.
        if (id.startsWith("stairs_slate")) {
            return SoundType.STONE;
        }
        // MaterialRope asked for SoundType.CLOTH by hand, which 1.21 names WOOL.
        if (id.equals("rope")) {
            return SoundType.WOOL;
        }
        if (id.equals("gargoyle") || id.endsWith("_pillar") || id.startsWith("slate")) {
            return SoundType.STONE;
        }
        return SoundType.METAL;
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

        // the same pair vanilla uses for planks, which is what the legacy numbers lined up with
        assertEquals(5, Blocks.OAK_PLANKS.getFireSpreadSpeed(Blocks.OAK_PLANKS.defaultBlockState(), null, null,
                Direction.NORTH));
        assertEquals(20, Blocks.OAK_PLANKS.getFlammability(Blocks.OAK_PLANKS.defaultBlockState(), null, null,
                Direction.NORTH));
    }

    // ---------------------------------------------------------------- creative tab

    @Test
    public void theDecorTabHoldsEveryBlockOfEveryFamily() {
        List<String> decorIds = EXPECTED_ALL.keySet().stream()
                .filter(id -> !FARMING_TAB_IDS.contains(id))
                .map(id -> "rustic:" + id)
                .toList();
        assertEquals(idsOf(decorIds), decorTabItemIds(), "the DECOR tab is not the legacy decor list");
    }

    /**
     * The {@code enable_*} flags used to decide whether a family was registered at all, which is why
     * turning one off in an existing world deleted the blocks. Now they only gate the tab, so a disabled
     * family must vanish from the creative menu while staying in the registry.
     */
    @Test
    public void disablingAFamilyOnlyRemovesItFromTheTab() {
        int full = decorTabItemIds().size();
        assertEquals(EXPECTED_ALL.size() - FARMING_TAB_IDS.size(), full, "the tab is not every decor family");
        gateLeavesTheRegistry(RusticConfig.ENABLE_CLAY_WALLS, 3,
                List.of("clay_wall", "clay_wall_cross", "clay_wall_diag"));
        // The two 3b flags: eight chairs of their own, and the lattice behind the switch that 1.12.2 put
        // in front of its single block.
        gateLeavesTheRegistry(RusticConfig.ENABLE_CHAIRS, 8,
                List.of("chair_oak", "chair_big_oak", "chair_birch", "chair_spruce", "chair_acacia",
                        "chair_jungle", "chair_olive", "chair_ironwood"));
        gateLeavesTheRegistry(RusticConfig.ENABLE_LATTICE, 1, List.of("iron_lattice"));
        assertEquals(full, decorTabItemIds().size(), "the tab must come back with every flag restored");
    }

    /** Turns one family off, checks it vanished from the tab but stayed registered, and puts it back. */
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

    /** Rebuilds the tab's contents the way the creative menu does, then maps them to ids. */
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

    /**
     * Phase 5 owns the crops and Phase 6 the brews, but the wood family's agriculture half and the fertile
     * soil already went to this tab with 3b. Phase 5 has now appended its own stake-crop poles, produce and
     * seeds to the same tab, so the decor baseline asserts the wood half as <em>membership</em> - that the
     * seven 3b ids are still offered - and leaves the exact crop list to {@code RusticFarmRegistrationTest}.
     */
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

        assertNotNull(RusticCreativeTabs.ALCHEMY.get());
        CreativeModeTab alchemy = RusticCreativeTabs.ALCHEMY.get();
        alchemy.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        assertEquals(List.of(), alchemy.getDisplayItems().stream()
                        .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                        .filter(id -> id.startsWith("rustic:"))
                        .toList(),
                "the ALCHEMY tab already has rustic content, which the phase list does not expect");
    }

    // ---------------------------------------------------------------- datagen output

    /**
     * Every block needs a blockstate, an item model and a loot table or it renders as the missing-texture
     * cube and drops nothing. All three are generated, so this is the check that they were generated -
     * re-run {@code runData} when it fails, do not hand-write the files.
     */
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
                // BlockLeavesRustic hand-rolled a harvestBlock instead of dropping itself: shears or silk
                // touch get the leaves, anything else rolls for that wood's sapling.
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
        // Tag order carries no meaning to the game, so every list is compared sorted. Distinct, because the
        // wood family reaches minecraft:mineable/axe twice - once as a family tool and once from the loop
        // that writes the vanilla class tags - and a tag file is allowed to say the same thing twice.
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
        // The rope is in none of them, because the legacy custom Material matched no entry in the mining
        // speed table at all - writing a tool here would be an improvement rather than a port.
        for (String tool : List.of("axe", "pickaxe", "shovel")) {
            assertFalse(distinctTag("mineable/" + tool).contains("rustic:rope"),
                    "rustic:rope is in mineable/" + tool);
        }
    }

    /** The shape tags 1.12.2 spelled out through the ore dictionary entries {@code slabStone} and {@code stairs}. */
    @Test
    public void theSlabAndStairShapeTagsListTheRusticOnes() {
        // The ore dictionary entries slabStone / slabWood / stairWood, which other mods' recipes read.
        assertEquals(idsOf(List.of("rustic:slate_roof_slab", "rustic:slate_brick_slab", "rustic:olive_slab",
                "rustic:ironwood_slab")), distinctTag("slabs"), "slabs");
        assertEquals(idsOf(List.of("rustic:stairs_slate_roof", "rustic:stairs_slate_brick",
                "rustic:stairs_olive", "rustic:stairs_ironwood")), distinctTag("stairs"), "stairs");
        assertEquals(idsOf(List.of("rustic:sapling_olive", "rustic:sapling_ironwood")),
                distinctTag("saplings"), "treeSapling");
        assertEquals(idsOf(List.of("rustic:fence_olive", "rustic:fence_ironwood")),
                distinctTag("wooden_fences"), "fenceWood");
    }

    /**
     * The vanilla block a derived phase-3b block inherited its properties from, so the assertion above
     * checks the inheritance rather than a restated number. Each wood's log, leaves and sapling were built
     * with {@code RusticBlockProperties.ofBase} on the oak equivalent, exactly as 1.12.2's classes extended
     * the vanilla block and never called {@code setHardness}/{@code setResistance}.
     */
    private static Block vanillaSource(String id) {
        if (id.startsWith("log_")) {
            return Blocks.OAK_LOG;
        }
        if (id.startsWith("leaves_")) {
            return Blocks.OAK_LEAVES;
        }
        return Blocks.OAK_SAPLING;
    }

    // ---------------------------------------------------------------- the hanging blocks (3b)

    /**
     * 1.12.2's {@code BlockRopeBase} stored one axis and computed the sagging flag on the fly in
     * {@code getActualState}. That mechanism is gone, so the sag is a listed property, the axis default is
     * still the legacy metadata 0, and the generated multipart file has to carry every answer the old
     * actual state could have produced - three axes, plus a dangle bar and a knot for each of the two
     * branches.
     */
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

        // 1.12.2's Forge isLadder == true for the whole rope base, which 1.21 reads back as a tag.
        List<String> climbable = new ArrayList<>(List.of("rustic:chain", "rustic:chain_gold", "rustic:rope"));
        if (RusticBlocks.silverDecorEnabled()) {
            climbable.add("rustic:chain_silver");
        }
        assertEquals(idsOf(climbable), distinctTag("climbable"), "every BlockRopeBase line was a ladder");
        assertFalse(distinctTag("climbable").contains("rustic:iron_lattice"),
                "the lattice is not a rope base and never answered the ladder question");
    }

    /**
     * The lattice's six connection flags were <em>unlisted</em> properties in 1.12.2, read out of the
     * neighbours by {@code getExtendedState}. They are stored here, which means the property list, the all
     * false base state and the 14 multipart conditions (a bare and a leafy centre, and an iron and a leafy
     * bar per direction) are what replaces the custom baked model.
     */
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

    /**
     * The two hooks 1.12.2 answered through Forge and NeoForge answers through {@code IBlockExtension},
     * with {@code TriState} replacing the boolean so that {@code DEFAULT} can mean the legacy
     * {@code super.canSustainPlant} fallthrough. The order of the legacy {@code EnumPlantType} switch is
     * what the two refusals test: a nether wart is a {@link CropBlock} and a waterlily a BushBlock, so both
     * would otherwise be accepted by the branch above them.
     */
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
        // The legacy Plausible.Nether/Water cases were the only refusals; anything else fell through.
        assertSame(TriState.DEFAULT, sustain(soil, pos, Blocks.STONE.defaultBlockState()), "not a plant at all");
    }

    private static TriState sustain(BlockFertileSoil soil, BlockPos pos, BlockState plant) {
        return soil.canSustainPlant(soil.defaultBlockState(), (BlockGetter) null, pos,
                Direction.UP, plant);
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

    /**
     * A tag's entries, sorted and deduplicated - the order and any repeats mean nothing to the game.
     *
     * <p>The {@code minecraft:mineable/*} files are shared with the Phase 4 machines (the barrel is
     * legitimately axe-mined), so the machine ids are filtered out here just as they are from
     * {@link #rusticBlocks()}: this is the decor baseline, and it asserts the decor half of each tag.
     * {@code RusticMachineRegistrationTest} covers the machine's own tag membership.
     */
    private static List<String> distinctTag(String name) {
        return tag(name).stream()
                .filter(id -> !machineIds().contains(id.substring(id.lastIndexOf(':') + 1)))
                .filter(id -> !farmIds().contains(id.substring(id.lastIndexOf(':') + 1)))
                .distinct().sorted().toList();
    }

    /** The rustic half of the block registry, keyed by path and sorted so failures read as a list. */
    private static Map<String, Block> rusticBlocks() {
        Map<String, Block> out = new TreeMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key != null && Rustic.MODID.equals(key.getNamespace()) && !machineIds().contains(key.getPath())
                    && !farmIds().contains(key.getPath())) {
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
                    && !farmIds().contains(key.getPath())) {
                out.put(key.getPath(), item);
            }
        }
        return out;
    }

    /**
     * The Phase 4 machine blocks live in their own register ({@code RusticMachineBlocks}) and are
     * characterised by {@code RusticMachineRegistrationTest}. This test is the decor baseline, so its
     * registry scans and its creative-tab comparison exclude these ids - a new machine then cannot
     * silently shift the decor count, and the decor tab assertion still checks every decor family is
     * present.
     */
    private static java.util.Set<String> machineIds() {
        return RusticMachineBlocks.entries().stream()
                .map(holder -> holder.getId().getPath())
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Phase 5's crop content lives in its own register ({@code RusticFarmBlocks}) plus a handful of
     * standalone produce / seed items, and is characterised by {@code RusticFarmRegistrationTest}. The decor
     * baseline excludes these ids exactly as it excludes the machine ids, so a new crop cannot silently
     * shift the decor block / item counts or the mineable-tag comparison.
     */
    private static java.util.Set<String> farmIds() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        RusticFarmBlocks.entries().forEach(holder -> ids.add(holder.getId().getPath()));
        ids.addAll(List.of("tomato", "chili_pepper", "ghost_pepper", "tomato_seeds", "chili_pepper_seeds",
                "grapes", "wildberries"));
        return ids;
    }
}
