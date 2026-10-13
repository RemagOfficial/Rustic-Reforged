package rustic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import rustic.common.Config;
import rustic.core.CommonProxy;
import rustic.testsupport.RusticContent;

/**
 * Phase 0 baseline for {@link Config}: every property name, category, default, range and the exact
 * way the Forge {@code Configuration} file is parsed. This is the spec the NeoForge
 * {@code ModConfigSpec} has to reproduce, including the places where the legacy code is buggy -
 * those are pinned here so the port makes an informed choice instead of silently changing behaviour.
 */
public class ConfigBaselineTest {

	private static final String GENERAL = "all.general";
	private static final String BEES = "all.bees";
	private static final String WORLD = "all.world";
	private static final String COMPAT = "all.compat";

	private File configFile;
	private Configuration harnessConfig;
	private Configuration testConfig;

	@BeforeClass
	public static void bootstrapRustic() {
		RusticContent.init();
	}

	@Before
	public void useAScratchConfigFile() throws Exception {
		harnessConfig = CommonProxy.config;
		configFile = new File(Files.createTempDirectory("rustic-config").toFile(), "rustic.cfg");
		testConfig = new Configuration(configFile);
		CommonProxy.config = testConfig;
		// these two are static and are appended to on every read, so they need a clean slate
		Config.OVERWORLD_GENERATION_WHITELIST.clear();
		Config.NETHER_GENERATION_WHITELIST.clear();
	}

	@After
	public void restoreTheHarnessConfig() {
		CommonProxy.config = harnessConfig;
		Config.OVERWORLD_GENERATION_WHITELIST.clear();
		Config.NETHER_GENERATION_WHITELIST.clear();
		Config.readConfig();
		// if this throws, the next test class would silently run against tampered config
		assertEquals(12000, Config.MAX_BREW_TIME);
		assertEquals(-1, Config.MIN_BREW_QUALITY_CHANGE);
		assertEquals(4, Config.MAX_BREW_QUALITY_CHANGE);
		assertEquals(1, Config.OVERWORLD_GENERATION_WHITELIST.size());
		assertEquals(1, Config.NETHER_GENERATION_WHITELIST.size());
	}

	// ---------------------------------------------------------------- defaults

	@Test
	public void aFreshFileGetsTheShippedDefaults() {
		Config.readConfig();

		// world gen
		assertEquals(0.03F, Config.BEEHIVE_GEN_CHANCE, 0.0001F);
		assertEquals(3, Config.MAX_BEEHIVE_ATTEMPTS);
		assertFalse(Config.NETHER_SLATE);
		assertEquals(5, Config.SLATE_VEINS_PER_CHUNK);
		assertEquals(20, Config.SLATE_VEIN_SIZE);
		assertEquals(0.03F, Config.OLIVE_GEN_CHANCE, 0.0001F);
		assertEquals(5, Config.MAX_OLIVE_GEN_ATTEMPTS);
		assertEquals(0.015F, Config.IRONWOOD_GEN_CHANCE, 0.0001F);
		assertEquals(4, Config.MAX_IRONWOOD_GEN_ATTEMPTS);
		assertEquals(0.125F, Config.HERB_GEN_CHANCE, 0.0001F);
		assertEquals(8, Config.MAX_HERB_ATTEMPTS);
		assertEquals(0.05F, Config.WILDBERRY_GEN_CHANCE, 0.0001F);
		assertEquals(4, Config.MAX_WILDBERRY_ATTEMPTS);
		assertEquals(Arrays.asList(0), Config.OVERWORLD_GENERATION_WHITELIST);
		assertEquals(Arrays.asList(-1), Config.NETHER_GENERATION_WHITELIST);

		// bees
		assertEquals(1F, Config.BEE_GROWTH_MULTIPLIER, 0.0001F);
		assertEquals(1F, Config.BEE_REPRODUCTION_MULTIPLIER, 0.0001F);
		assertEquals(1F, Config.BEE_HONEYCOMB_MULTIPLIER, 0.0001F);

		// toggles
		assertTrue(Config.FLESH_SMELTING);
		assertTrue(Config.EXTRA_ARMOR_HUD);
		assertTrue(Config.TOUGHNESS_HUD);
		assertTrue(Config.OFFSET_WILDBERRY_BUSHES);
		assertTrue(Config.ENABLE_SLATE);
		assertTrue(Config.ENABLE_PILLARS);
		assertTrue(Config.ENABLE_CLAY_WALLS);
		assertTrue(Config.ENABLE_PAINTED_WOOD);
		assertTrue(Config.ENABLE_TABLES);
		assertTrue(Config.ENABLE_CHAIRS);
		assertTrue(Config.ENABLE_LATTICE);
		assertTrue(Config.ENABLE_OLIVE_OILING);
		assertTrue(Config.ENABLE_SEED_DROPS);
		assertTrue(Config.ENABLE_BOTTLE_EMPTYING);
		assertFalse(Config.OLIVE_OIL_USE_WHITELIST);
		assertFalse(Config.GRAPE_DROP_NEEDS_TOOL);
		assertEquals(7, Config.SEED_DROP_RATE);

		// brewing
		assertEquals(-1, Config.MIN_BREW_QUALITY_CHANGE);
		assertEquals(4, Config.MAX_BREW_QUALITY_CHANGE);
		assertEquals(12000, Config.MAX_BREW_TIME);

		// compat
		assertTrue(Config.ENABLE_FORESTRY_COMPAT);
		assertTrue(Config.ENABLE_SILVER_DECOR);

		// lists
		assertEquals(Arrays.asList("minecraft:slime", "minecraft:shulker"), Config.IRONSKIN_RENDER_ENTITY_BLACKLIST);
		assertEquals(Arrays.asList("minecraft:iron_hoe", "minecraft:diamond_hoe"), Config.GRAPE_TOOL_WHITELIST);
		assertEquals(Arrays.asList("#stickWood", "minecraft:bone", "tconstruct:broadsword", "tconstruct:longsword",
				"tconstruct:rapier", "tconstruct:frypan", "tconstruct:battlesign", "tconstruct:cleaver"),
				Config.VANTA_OIL_WHITELIST);
		assertTrue(Config.OLIVE_OIL_BLACKLIST.isEmpty());
	}

	/**
	 * "all.general" and friends contain a dot, which Forge treats as a category hierarchy separator: an
	 * empty parent category called "all" is synthesized. Five category names therefore exist for four
	 * real categories, and category lookup is case-insensitive by default. 1.21 TOML nests tables the
	 * same way, but the empty parent is still worth knowing about when porting.
	 */
	@Test
	public void thereAreFourCategoriesNestedUnderASynthesizedAllParent() {
		Config.readConfig();

		for (String category : Arrays.asList(GENERAL, BEES, WORLD, COMPAT)) {
			assertTrue(category + " should exist", testConfig.hasCategory(category));
		}
		assertEquals(new java.util.TreeSet<String>(Arrays.asList("all", GENERAL, BEES, WORLD, COMPAT)),
				new java.util.TreeSet<String>(testConfig.getCategoryNames()));

		assertEquals("General Options", testConfig.getCategory(GENERAL).getComment());
		assertEquals("Bee Related Options", testConfig.getCategory(BEES).getComment());
		assertEquals("World Generation Options", testConfig.getCategory(WORLD).getComment());
		assertEquals("Mod Compatability Related Options", testConfig.getCategory(COMPAT).getComment());
	}

	/**
	 * 1.12 has no proper lang keys: Forge defaults the language key to the raw property name (spaces
	 * and all) and appends the range/default note to the comment. 1.21 needs real translation keys, so
	 * this pins which string each property was identified by.
	 *
	 * Note the properties are read straight off the category map: Configuration.get(...) would overwrite
	 * the comment with null again, which is itself a legacy-API trap worth remembering.
	 */
	@Test
	public void propertyNamesAreTheLangKeysAndCommentsCarryTheRange() {
		Config.readConfig();

		Property brewTime = testConfig.getCategory(GENERAL).get("Brewing Time");
		assertEquals("Brewing Time", brewTime.getLanguageKey());
		assertEquals("how long it should take for a brewing barrel to finish a brew, in ticks "
				+ "[range: 1200 ~ 120000, default: 12000]", brewTime.getComment());

		Property veinSize = testConfig.getCategory(WORLD).get("Slate Vein Size");
		assertTrue("unexpected comment: " + veinSize.getComment(),
				veinSize.getComment().endsWith("[range: 0 ~ 25, default: 20]"));

		Property booleansHaveNoRange = testConfig.getCategory(GENERAL).get("Enable Slate");
		assertEquals("Enable Slate", booleansHaveNoRange.getLanguageKey());
		assertFalse("boolean comments must not advertise a range",
				booleansHaveNoRange.getComment().contains("[range:"));
	}

	// ---------------------------------------------------------------- validation

	@Test
	public void outOfRangeNumbersAreClampedToTheDeclaredBounds() throws Exception {
		seed(GENERAL, "Brewing Time", 500000);
		seed(GENERAL, "Seed Drop Rate", 0);
		seed(GENERAL, "Minimum Change To Brew Quality", -999);
		seed(GENERAL, "Maximum Change To Brew Quality", 999);
		seed(BEES, "Max Beehive Generation Attempts", 129);
		seed(WORLD, "Slate Vein Size", 26);
		seed(BEES, "Bee Crop Boost Multiplier", 11F);
		Config.readConfig();

		assertEquals(120000, Config.MAX_BREW_TIME);
		assertEquals(1, Config.SEED_DROP_RATE);
		assertEquals(-50, Config.MIN_BREW_QUALITY_CHANGE);
		assertEquals(50, Config.MAX_BREW_QUALITY_CHANGE);
		assertEquals(128, Config.MAX_BEEHIVE_ATTEMPTS);
		assertEquals(25, Config.SLATE_VEIN_SIZE);
		assertEquals(10F, Config.BEE_GROWTH_MULTIPLIER, 0.0001F);
	}

	/**
	 * An unparseable value does not throw: the raw string is kept in the file and the default is used
	 * for the field. A 1.21 ModConfigSpec would reject/repair the entry, so the port has to decide
	 * whether keeping the lenient behaviour matters for existing users' files.
	 */
	@Test
	public void garbageNumbersFallBackToTheDefaultWithoutThrowing() throws Exception {
		seed(GENERAL, "Brewing Time", "banana");
		Config.readConfig();

		assertEquals(12000, Config.MAX_BREW_TIME);
		assertEquals(Property.Type.STRING, testConfig.getCategory(GENERAL).get("Brewing Time").getType());
	}

	/**
	 * LATENT BUG, pinned deliberately: every {@code readConfig()} adds the dimension whitelist entries
	 * again instead of replacing them, so a config reload duplicates them. Anything that just checks
	 * {@code contains(dimensionId)} is unaffected, but code that uses the list size is not.
	 */
	@Test
	public void dimensionWhitelistsGrowOnEveryReload() {
		Config.readConfig();
		assertEquals(Arrays.asList(0), Config.OVERWORLD_GENERATION_WHITELIST);

		Config.readConfig();
		assertEquals(Arrays.asList(0, 0), Config.OVERWORLD_GENERATION_WHITELIST);

		Config.readConfig();
		assertEquals(3, Config.OVERWORLD_GENERATION_WHITELIST.size());
		assertEquals(Arrays.asList(-1, -1, -1), Config.NETHER_GENERATION_WHITELIST);
	}

	/**
	 * LATENT BUG, pinned deliberately: {@code Integer.parseInt} runs inside {@code readConfig}'s
	 * try/catch whose handler is completely empty, so one non-numeric dimension id silently kills the
	 * rest of the parsing - the nether whitelist never gets loaded at all, and no error is reported.
	 */
	@Test
	public void oneBadDimensionIdSilentlyAbortsTheDimensionParsing() throws Exception {
		seed(WORLD, "Overworld Generation Dimension Whitelist", new String[] { "0", "banana" });
		Config.readConfig();

		assertEquals(Arrays.asList(0), Config.OVERWORLD_GENERATION_WHITELIST);
		assertTrue("the nether whitelist was never parsed", Config.NETHER_GENERATION_WHITELIST.isEmpty());
		// everything assigned before the throw is still intact
		assertEquals(12000, Config.MAX_BREW_TIME);
		assertTrue(Config.ENABLE_SILVER_DECOR);
	}

	/**
	 * The parsed lists are {@code Arrays.asList} results, so they cannot be added to. Several compat
	 * paths would throw UnsupportedOperationException if they tried to sort or extend a whitelist.
	 */
	@Test
	public void theStringListFieldsAreImmutable() {
		Config.readConfig();

		assertImmutable(Config.VANTA_OIL_WHITELIST, "VANTA_OIL_WHITELIST");
		assertImmutable(Config.GRAPE_TOOL_WHITELIST, "GRAPE_TOOL_WHITELIST");
		assertImmutable(Config.IRONSKIN_RENDER_ENTITY_BLACKLIST, "IRONSKIN_RENDER_ENTITY_BLACKLIST");
		assertImmutable(Config.OLIVE_OIL_BLACKLIST, "OLIVE_OIL_BLACKLIST");
	}

	// ---------------------------------------------------------------- file round trips

	@Test
	public void userEditedValuesSurviveARealFileRoundTrip() throws Exception {
		seed(GENERAL, "Enable Slate", false);
		seed(GENERAL, "Brewing Time", 6000);
		seed(GENERAL, "Vanta Oil Whitelist", new String[] { "minecraft:stick" });
		seed(GENERAL, "Iron Skin Renderer Blacklist", new String[] { "minecraft:cow", "minecraft:sheep" });
		seed(WORLD, "Overworld Generation Dimension Whitelist", new String[] { "0", "2", "7" });
		seed(COMPAT, "Enable Forestry Compat", false);

		// a brand new Configuration object, reading only what was written to disk
		CommonProxy.config = new Configuration(configFile);
		Config.readConfig();

		assertFalse(Config.ENABLE_SLATE);
		assertEquals(6000, Config.MAX_BREW_TIME);
		assertEquals(Arrays.asList("minecraft:stick"), Config.VANTA_OIL_WHITELIST);
		assertEquals(Arrays.asList("minecraft:cow", "minecraft:sheep"), Config.IRONSKIN_RENDER_ENTITY_BLACKLIST);
		assertEquals(Arrays.asList(0, 2, 7), Config.OVERWORLD_GENERATION_WHITELIST);
		assertFalse(Config.ENABLE_FORESTRY_COMPAT);
		assertTrue("the untouched properties still come from the defaults", Config.ENABLE_TABLES);

		assertTrue(configFile.exists());
		String written = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
		assertTrue(written.contains("Enable Slate"));
		assertTrue(written.contains("minecraft:stick"));
	}

	// ---------------------------------------------------------------- helpers

	private static void assertImmutable(java.util.List<String> list, String label) {
		try {
			list.add("rustic:whatever");
			fail(label + " is expected to be an immutable Arrays.asList view");
		} catch (UnsupportedOperationException expected) {
			// the 1.12 behaviour we are pinning
		}
	}

	private void seed(String category, String key, int value) {
		Configuration seed = new Configuration(configFile);
		seed.load();
		seed.get(category, key, 1).set(value);
		seed.save();
	}

	private void seed(String category, String key, float value) {
		Configuration seed = new Configuration(configFile);
		seed.load();
		seed.get(category, key, 1F).set(value);
		seed.save();
	}

	private void seed(String category, String key, boolean value) {
		Configuration seed = new Configuration(configFile);
		seed.load();
		seed.get(category, key, true).set(value);
		seed.save();
	}

	private void seed(String category, String key, String value) {
		Configuration seed = new Configuration(configFile);
		seed.load();
		seed.get(category, key, "0").set(value);
		seed.save();
	}

	private void seed(String category, String key, String[] value) {
		Configuration seed = new Configuration(configFile);
		seed.load();
		seed.getStringList(key, category, value, "seeded by the baseline test");
		seed.save();
	}

}
