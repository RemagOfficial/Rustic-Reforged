package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Phase 8 read-back over the world-generation JSON {@code rustic.data.RusticWorldGenProvider} writes into
 * {@code src/generated/resources/data/rustic/worldgen/} and {@code data/rustic/neoforge/biome_modifier/}.
 *
 * <p>Like {@code RusticRecipeDatagenTest} this never boots Minecraft or re-runs datagen - the generated dir is a main
 * resource root, so each assertion reads the shipped JSON off the classpath. What it locks down is the hybrid's data
 * half against the legacy {@code WorldGeneratorRustic} table: every generator is a bare feature (the <em>what</em>)
 * whose block set / tree shape must match 1.12.2, a {@code rustic:config_gated} wrapper that must name the right
 * {@link rustic.content.world.RusticGenKnob} and inner feature, a placed feature with an empty placement list (the
 * gate owns scatter), and a {@code neoforge:add_features} biome modifier on the right dimension tag and decoration
 * step. It also checks the two Phase-8 blocks landed in their class tags so the trees/hive are actually plantable and
 * harvestable. A codec slip in the provider, a wrong herb set, or a mis-routed biome modifier fails here by file name.
 */
public class RusticWorldGenDatagenTest {

    private static final String CF = "/data/rustic/worldgen/configured_feature/";
    private static final String PF = "/data/rustic/worldgen/placed_feature/";
    private static final String BM = "/data/rustic/neoforge/biome_modifier/";

    // ------------------------------------------------------------------ slate ore

    @Test
    public void slateOreReplacesStoneInTheOverworldAndNetherrackInTheNether() {
        JsonObject overworld = configured("slate_ore_overworld");
        assertEquals("minecraft:ore", overworld.get("type").getAsString());
        JsonObject owTarget = overworld.getAsJsonObject("config").getAsJsonArray("targets").get(0).getAsJsonObject();
        assertEquals("rustic:slate", owTarget.getAsJsonObject("state").get("Name").getAsString());
        assertEquals("minecraft:tag_match", owTarget.getAsJsonObject("target").get("predicate_type").getAsString());
        assertEquals("minecraft:stone_ore_replaceables", owTarget.getAsJsonObject("target").get("tag").getAsString());
        // vein size mirrors the shipped slate_vein_size default (20), baked statically into the JSON
        assertEquals(20, overworld.getAsJsonObject("config").get("size").getAsInt());

        JsonObject nether = configured("slate_ore_nether");
        JsonObject nTarget = nether.getAsJsonObject("config").getAsJsonArray("targets").get(0).getAsJsonObject();
        assertEquals("rustic:slate", nTarget.getAsJsonObject("state").get("Name").getAsString());
        assertEquals("minecraft:block_match", nTarget.getAsJsonObject("target").get("predicate_type").getAsString());
        assertEquals("minecraft:netherrack", nTarget.getAsJsonObject("target").get("block").getAsString());
    }

    @Test
    public void slateGatesRouteThroughTheirDimensionKnobs() {
        assertGated("slate_ore_overworld", "rustic:slate_ore_overworld", "SLATE_OVERWORLD");
        assertGated("slate_ore_nether", "rustic:slate_ore_nether", "SLATE_NETHER");

        // each gated feature is exposed with an empty placement (the gate scatters live from config)
        assertPlacedEmpty("slate_ore_overworld", "rustic:slate_ore_overworld_gated");
        assertPlacedEmpty("slate_ore_nether", "rustic:slate_ore_nether_gated");

        assertBiomeModifier("slate_ore_overworld", "#c:is_overworld", "rustic:slate_ore_overworld_placed", "underground_ores");
        assertBiomeModifier("slate_ore_nether", "#c:is_nether", "rustic:slate_ore_nether_placed", "underground_ores");
    }

    // ------------------------------------------------------------------ trees

    @Test
    public void oliveAndIronwoodTreesRebuildTheLegacyOakStyleShapes() {
        assertTree("olive_tree", "rustic:log_olive", "rustic:leaves_olive", 4, 3);
        assertTree("ironwood_tree", "rustic:log_ironwood", "rustic:leaves_ironwood", 7, 6);

        assertGated("olive_tree", "rustic:olive_tree", "OLIVE");
        assertGated("ironwood_tree", "rustic:ironwood_tree", "IRONWOOD");
        assertPlacedEmpty("olive_tree", "rustic:olive_tree_gated");
        assertPlacedEmpty("ironwood_tree", "rustic:ironwood_tree_gated");
        assertBiomeModifier("olive_tree", "#c:is_overworld", "rustic:olive_tree_placed", "vegetal_decoration");
        assertBiomeModifier("ironwood_tree", "#c:is_overworld", "rustic:ironwood_tree_placed", "vegetal_decoration");
    }

    /**
     * The apple tree is sapling-grown only (legacy {@code WorldGenAllTrees} never placed it biomedly): it gets a bare
     * configured feature - vanilla oak trunk + the fruit-bearing apple leaves at age 0 - but no gate, no placed
     * feature and no biome modifier, exactly the plan's faithful reproduction.
     */
    @Test
    public void appleTreeExistsButIsNeverBiomePlaced() {
        JsonObject apple = configured("apple_tree");
        assertEquals("minecraft:tree", apple.get("type").getAsString());
        JsonObject config = apple.getAsJsonObject("config");
        assertEquals("minecraft:oak_log", obj(obj(config, "trunk_provider"), "state").get("Name").getAsString());
        JsonObject foliage = obj(obj(config, "foliage_provider"), "state");
        assertEquals("rustic:leaves_apple", foliage.get("Name").getAsString());
        assertEquals("0", foliage.getAsJsonObject("Properties").get("age").getAsString());
        JsonObject trunk = config.getAsJsonObject("trunk_placer");
        assertEquals("minecraft:straight_trunk_placer", trunk.get("type").getAsString());
        assertEquals(5, trunk.get("base_height").getAsInt());
        assertEquals(2, trunk.get("height_rand_a").getAsInt());

        assertFalse(exists(CF + "apple_tree_gated.json"), "apple must not be config-gated");
        assertFalse(exists(PF + "apple_tree_placed.json"), "apple must not be biome-placed");
        assertFalse(exists(BM + "apple_tree.json"), "apple must have no biome modifier");
    }

    // ------------------------------------------------------------------ surface herbs (the eight legacy categories)

    @Test
    public void surfaceHerbCategoriesMatchGetRandomHerbForBiome() {
        assertHerbSet("herbs_jungle", "rustic:blood_orchid", "rustic:horsetail", "rustic:marsh_mallow", "rustic:mooncap_mushroom");
        assertHerbSet("herbs_sandy", "rustic:aloe_vera");
        assertHerbSet("herbs_savanna", "rustic:aloe_vera");
        assertHerbSet("herbs_mesa", "rustic:aloe_vera");
        assertHerbSet("herbs_mountain", "rustic:wind_thistle", "rustic:cloudsbluff");
        assertHerbSet("herbs_swamp", "rustic:chamomile", "rustic:horsetail", "rustic:marsh_mallow");
        assertHerbSet("herbs_forest", "rustic:chamomile", "rustic:cohosh", "rustic:ginseng", "rustic:horsetail", "rustic:vanta_lily");
        assertHerbSet("herbs_plains", "rustic:chamomile", "rustic:ginseng", "rustic:horsetail", "rustic:wind_thistle", "rustic:vanta_lily");
    }

    @Test
    public void eachSurfaceHerbCategorySharesTheGateAndTargetsItsOwnBiomeTag() {
        // All eight wrap the same SURFACE_HERB knob; the category identity comes from the biome modifier's tag,
        // which is what replaces the legacy if/else-if chain (the eight tags are disjoint in vanilla biomes).
        assertGated("herbs_jungle", "rustic:herbs_jungle", "SURFACE_HERB");
        assertGated("herbs_sandy", "rustic:herbs_sandy", "SURFACE_HERB");
        assertGated("herbs_savanna", "rustic:herbs_savanna", "SURFACE_HERB");
        assertGated("herbs_mesa", "rustic:herbs_mesa", "SURFACE_HERB");
        assertGated("herbs_mountain", "rustic:herbs_mountain", "SURFACE_HERB");
        assertGated("herbs_swamp", "rustic:herbs_swamp", "SURFACE_HERB");
        assertGated("herbs_forest", "rustic:herbs_forest", "SURFACE_HERB");
        assertGated("herbs_plains", "rustic:herbs_plains", "SURFACE_HERB");

        assertBiomeModifier("herbs_jungle", "#c:is_jungle", "rustic:herbs_jungle_placed", "vegetal_decoration");
        assertBiomeModifier("herbs_sandy", "#c:is_sandy", "rustic:herbs_sandy_placed", "vegetal_decoration");
        assertBiomeModifier("herbs_savanna", "#c:is_savanna", "rustic:herbs_savanna_placed", "vegetal_decoration");
        // the legacy MESA maps to the modern badlands tag
        assertBiomeModifier("herbs_mesa", "#c:is_badlands", "rustic:herbs_mesa_placed", "vegetal_decoration");
        assertBiomeModifier("herbs_mountain", "#c:is_mountain", "rustic:herbs_mountain_placed", "vegetal_decoration");
        assertBiomeModifier("herbs_swamp", "#c:is_swamp", "rustic:herbs_swamp_placed", "vegetal_decoration");
        assertBiomeModifier("herbs_forest", "#c:is_forest", "rustic:herbs_forest_placed", "vegetal_decoration");
        assertBiomeModifier("herbs_plains", "#c:is_plains", "rustic:herbs_plains_placed", "vegetal_decoration");
    }

    // ------------------------------------------------------------------ cave + nether herbs, wildberries

    @Test
    public void caveHerbsAreCoreRootAndMooncapDroppedUnderground() {
        assertHerbSet("herbs_cave", "rustic:core_root", "rustic:mooncap_mushroom");
        assertGated("herbs_cave", "rustic:herbs_cave", "CAVE_HERB");
        assertPlacedEmpty("herbs_cave", "rustic:herbs_cave_gated");
        // the legacy WorldGenCaveHerbs asked no biome, it only scattered underground - so every overworld biome,
        // and the underground_decoration step so the drop-from-ceiling runs below the surface pass
        assertBiomeModifier("herbs_cave", "#c:is_overworld", "rustic:herbs_cave_placed", "underground_decoration");
    }

    @Test
    public void netherHerbsAreAlwaysDeathstalk() {
        assertHerbSet("herbs_nether", "rustic:deathstalk_mushroom");
        assertGated("herbs_nether", "rustic:herbs_nether", "NETHER_HERB");
        assertBiomeModifier("herbs_nether", "#c:is_nether", "rustic:herbs_nether_placed", "vegetal_decoration");
    }

    /**
     * The wildberry bush carries all six {@code AGE_5} states at equal weight - the JSON form of the legacy
     * {@code withAge(rand.nextInt(6))} - and routes to every overworld biome; the legacy blacklist
     * (cold/snowy/sandy/savanna/mesa/mushroom/nether/end/dead/wasteland) lives in the WILDBERRY knob's none-of list,
     * evaluated per chunk, not in the biome tag.
     */
    @Test
    public void wildberriesRollEveryBushAge() {
        JsonObject berries = configured("wildberries");
        assertEquals("minecraft:simple_block", berries.get("type").getAsString());
        JsonObject provider = berries.getAsJsonObject("config").getAsJsonObject("to_place");
        assertEquals("minecraft:weighted_state_provider", provider.get("type").getAsString());
        List<String> states = herbStates(provider);
        assertEquals(List.of(
                "rustic:wildberry_bush@0", "rustic:wildberry_bush@1", "rustic:wildberry_bush@2",
                "rustic:wildberry_bush@3", "rustic:wildberry_bush@4", "rustic:wildberry_bush@5"), states);

        assertGated("wildberries", "rustic:wildberries", "WILDBERRY");
        assertBiomeModifier("wildberries", "#c:is_overworld", "rustic:wildberries_placed", "vegetal_decoration");
    }

    // ------------------------------------------------------------------ beehive

    /**
     * The beehive is the one generator with no data block-set to declare: {@code rustic:beehive} places the hive
     * itself after its leaves walk-down, so its configured feature names the code feature with an empty config, its
     * placed feature points straight at that bare feature (no {@code _gated} wrapper), and the whole
     * {@code RusticGenKnob.BEEHIVE} gate lives inside {@code place()}.
     */
    @Test
    public void beehiveIsAnUngatedCodeFeature() {
        JsonObject hive = configured("beehive");
        assertEquals("rustic:beehive", hive.get("type").getAsString());
        assertTrue(hive.getAsJsonObject("config").entrySet().isEmpty(), "rustic:beehive takes an empty config");

        JsonObject placed = placed("beehive");
        assertEquals("rustic:beehive", placed.get("feature").getAsString(), "the hive is not wrapped by a gate");
        assertEquals(0, placed.getAsJsonArray("placement").size());

        assertBiomeModifier("beehive", "#c:is_overworld", "rustic:beehive_placed", "vegetal_decoration");
        assertFalse(exists(CF + "beehive_gated.json"), "there is no gated beehive variant");
    }

    // ------------------------------------------------------------------ the two Phase-8 blocks in their class tags

    @Test
    public void appleLeavesAndSaplingJoinTheVanillaClassTags() {
        assertTrue(tagContains("leaves", "rustic:leaves_apple"), "apple leaves must be in #minecraft:leaves");
        assertTrue(tagContains("saplings", "rustic:sapling_apple"), "apple sapling must be in #minecraft:saplings");
    }

    /** The beehive and the apple leaves/sapling are real registered, lootable, modelled blocks, not just JSON hooks. */
    @Test
    public void theNewBlocksHaveLootAndBlockstate() {
        assertTrue(exists("/data/rustic/loot_table/blocks/beehive.json"), "beehive needs a loot table");
        assertTrue(exists("/assets/rustic/blockstates/beehive.json"), "beehive needs a blockstate");
        assertTrue(exists("/assets/rustic/blockstates/leaves_apple.json"), "apple leaves need a blockstate");
        assertTrue(exists("/assets/rustic/blockstates/sapling_apple.json"), "apple sapling needs a blockstate");
    }

    // ------------------------------------------------------------------ helpers

    private static void assertGated(String name, String expectedInner, String expectedKnob) {
        JsonObject gated = configured(name + "_gated");
        assertEquals("rustic:config_gated", gated.get("type").getAsString(), name + "_gated type");
        JsonObject config = gated.getAsJsonObject("config");
        assertEquals(expectedInner, config.get("inner").getAsString(), name + "_gated inner");
        assertEquals(expectedKnob, config.get("knob").getAsString(), name + "_gated knob");
    }

    private static void assertPlacedEmpty(String name, String expectedFeature) {
        JsonObject placed = placed(name);
        assertEquals(expectedFeature, placed.get("feature").getAsString(), name + "_placed feature");
        assertEquals(0, placed.getAsJsonArray("placement").size(), name + "_placed must not scatter (the gate does)");
    }

    private static void assertBiomeModifier(String name, String biomes, String feature, String step) {
        JsonObject modifier = biomeModifier(name);
        assertEquals("neoforge:add_features", modifier.get("type").getAsString(), name + " modifier type");
        assertEquals(biomes, modifier.get("biomes").getAsString(), name + " biome tag");
        assertEquals(feature, modifier.get("features").getAsString(), name + " placed reference");
        assertEquals(step, modifier.get("step").getAsString(), name + " decoration step");
    }

    /** An oak-style tree: the given log/leaves under a {@code straight} trunk of {@code base + rand(randA)}. */
    private static void assertTree(String name, String log, String leaves, int baseHeight, int heightRandA) {
        JsonObject tree = configured(name);
        assertEquals("minecraft:tree", tree.get("type").getAsString(), name + " type");
        JsonObject config = tree.getAsJsonObject("config");
        assertEquals(log, obj(obj(config, "trunk_provider"), "state").get("Name").getAsString(), name + " log");
        assertEquals(leaves, obj(obj(config, "foliage_provider"), "state").get("Name").getAsString(), name + " leaves");
        JsonObject trunk = config.getAsJsonObject("trunk_placer");
        assertEquals("minecraft:straight_trunk_placer", trunk.get("type").getAsString(), name + " trunk placer");
        assertEquals(baseHeight, trunk.get("base_height").getAsInt(), name + " base height");
        assertEquals(heightRandA, trunk.get("height_rand_a").getAsInt(), name + " height rand");
        assertEquals("minecraft:blob_foliage_placer",
                config.getAsJsonObject("foliage_placer").get("type").getAsString(), name + " foliage placer");
    }

    /** A bare surface/cave/nether herb feature: simple_block over an equal-weight list of age-3 herb states. */
    private static void assertHerbSet(String name, String... herbs) {
        JsonObject feature = configured(name);
        assertEquals("minecraft:simple_block", feature.get("type").getAsString(), name + " type");
        JsonObject provider = feature.getAsJsonObject("config").getAsJsonObject("to_place");
        assertEquals("minecraft:weighted_state_provider", provider.get("type").getAsString(), name + " provider");
        List<String> expected = new ArrayList<>();
        for (String herb : herbs) {
            expected.add(herb + "@3"); // 1.12.2 planted every herb fully ripe (withProperty(AGE, 3))
        }
        assertEquals(expected, herbStates(provider), name + " herb set");
    }

    /** The {@code Name@age} of every entry in a weighted state provider, in the JSON's declared order. */
    private static List<String> herbStates(JsonObject provider) {
        List<String> states = new ArrayList<>();
        for (JsonElement entry : provider.getAsJsonArray("entries")) {
            JsonObject data = entry.getAsJsonObject().getAsJsonObject("data");
            String age = data.getAsJsonObject("Properties").get("age").getAsString();
            states.add(data.get("Name").getAsString() + "@" + age);
        }
        return states;
    }

    private static boolean tagContains(String tag, String id) {
        JsonObject json = json("/data/minecraft/tags/block/" + tag + ".json");
        for (JsonElement value : json.getAsJsonArray("values")) {
            if (id.equals(value.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject obj(JsonObject parent, String member) {
        return parent.getAsJsonObject(member);
    }

    private static JsonObject configured(String name) {
        return json(CF + name + ".json");
    }

    /** Placed features are written as {@code <stem>_placed.json} - the same stem as their configured features. */
    private static JsonObject placed(String name) {
        return json(PF + name + "_placed.json");
    }

    private static JsonObject biomeModifier(String name) {
        return json(BM + name + ".json");
    }

    /** Whether a generated data/asset file is present on the classpath, without asserting it parses. */
    private static boolean exists(String classpathResource) {
        return RusticWorldGenDatagenTest.class.getResource(classpathResource) != null;
    }

    private static JsonObject json(String classpathResource) {
        try (InputStream in = RusticWorldGenDatagenTest.class.getResourceAsStream(classpathResource)) {
            assertNotNull(in, "missing generated worldgen file on the classpath: " + classpathResource
                    + " - re-run runData rather than writing the file by hand");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new RuntimeException("could not read " + classpathResource, e);
        }
    }
}
