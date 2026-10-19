package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.Range;
import rustic.config.RusticConfig;

public class RusticConfigBaselineTest {

    private static final String GENERAL = "General Options";
    private static final String WORLD = "World Generation Options";
    private static final String BEES = "Bee Related Options";
    private static final String COMPAT = "Mod Compatability Related Options";

    private static final List<ConfigValue<?>> ALL = List.of(

            RusticConfig.FLESH_SMELTING,
            RusticConfig.EXTRA_ARMOR_HUD,
            RusticConfig.TOUGHNESS_HUD,
            RusticConfig.IRONSKIN_RENDER_ENTITY_BLACKLIST,
            RusticConfig.OFFSET_WILDBERRY_BUSHES,
            RusticConfig.ENABLE_SLATE,
            RusticConfig.ENABLE_PILLARS,
            RusticConfig.ENABLE_CLAY_WALLS,
            RusticConfig.ENABLE_PAINTED_WOOD,
            RusticConfig.ENABLE_TABLES,
            RusticConfig.ENABLE_CHAIRS,
            RusticConfig.ENABLE_LATTICE,
            RusticConfig.OLIVE_OIL_BLACKLIST,
            RusticConfig.OLIVE_OIL_USE_WHITELIST,
            RusticConfig.ENABLE_OLIVE_OILING,
            RusticConfig.GRAPE_DROP_NEEDS_TOOL,
            RusticConfig.GRAPE_TOOL_WHITELIST,
            RusticConfig.ENABLE_SEED_DROPS,
            RusticConfig.SEED_DROP_RATE,
            RusticConfig.VANTA_OIL_WHITELIST,
            RusticConfig.MIN_BREW_QUALITY_CHANGE,
            RusticConfig.MAX_BREW_QUALITY_CHANGE,
            RusticConfig.MAX_BREW_TIME,
            RusticConfig.ENABLE_BOTTLE_EMPTYING,

            RusticConfig.NETHER_SLATE,
            RusticConfig.SLATE_VEINS_PER_CHUNK,
            RusticConfig.SLATE_VEIN_SIZE,
            RusticConfig.OLIVE_GEN_CHANCE,
            RusticConfig.MAX_OLIVE_GEN_ATTEMPTS,
            RusticConfig.IRONWOOD_GEN_CHANCE,
            RusticConfig.MAX_IRONWOOD_GEN_ATTEMPTS,
            RusticConfig.HERB_GEN_CHANCE,
            RusticConfig.MAX_HERB_ATTEMPTS,
            RusticConfig.WILDBERRY_GEN_CHANCE,
            RusticConfig.MAX_WILDBERRY_ATTEMPTS,
            RusticConfig.OVERWORLD_GENERATION_WHITELIST,
            RusticConfig.NETHER_GENERATION_WHITELIST,

            RusticConfig.BEEHIVE_GEN_CHANCE,
            RusticConfig.MAX_BEEHIVE_ATTEMPTS,
            RusticConfig.BEE_GROWTH_MULTIPLIER,
            RusticConfig.BEE_REPRODUCTION_MULTIPLIER,
            RusticConfig.BEE_HONEYCOMB_MULTIPLIER,

            RusticConfig.ENABLE_SILVER_DECOR);

    @Test
    public void theSpecIsExactlyTheFortyThreePortedLegacyProperties() {
        assertEquals(43, ALL.size(), "property count changed without a baseline update");
        assertEquals(43L, ALL.stream().map(ConfigValue::getPath).distinct().count(), "two properties share a path");

        for (ConfigValue<?> value : ALL) {
            List<String> path = value.getPath();
            assertEquals(2, path.size(), path + " must sit directly under a category");
            assertTrue(List.of(GENERAL, WORLD, BEES, COMPAT).contains(path.get(0)),
                    path + " has an unknown category");
            assertTrue(path.get(1).matches("[a-z0-9_]+"), path + " is not a lower_snake_case toml key");
            assertNotNull(value.getDefault(), path + " has no default");
            assertNotNull(value.getSpec().getComment(), path + " has no comment");
            assertFalse(value.getSpec().getComment().isBlank(), path + " has a blank comment");
        }
    }

    @Test
    public void thereAreFourLegacyCategoriesAndNoSynthesizedAllParent() {
        assertEquals(24, countIn(GENERAL), GENERAL);
        assertEquals(13, countIn(WORLD), WORLD);
        assertEquals(5, countIn(BEES), BEES);

        assertEquals(1, countIn(COMPAT), COMPAT);

        assertNull(RusticConfig.SPEC.getLevelComment(List.of(GENERAL)));
        assertNull(RusticConfig.SPEC.getLevelComment(List.of(WORLD)));
        assertNull(RusticConfig.SPEC.getLevelComment(List.of(BEES)));
        assertNull(RusticConfig.SPEC.getLevelComment(List.of(COMPAT)));
        assertFalse(RusticConfig.SPEC.isEmpty(), "an empty spec means nothing was registered");
    }

    @Test
    public void shippedScalarsMatchTheLegacyDefaults() {

        assertEquals(0.03D, RusticConfig.BEEHIVE_GEN_CHANCE.getDefault(), 0.0001D);
        assertEquals(3, RusticConfig.MAX_BEEHIVE_ATTEMPTS.getDefault().intValue());
        assertFalse(RusticConfig.NETHER_SLATE.getDefault());
        assertEquals(5, RusticConfig.SLATE_VEINS_PER_CHUNK.getDefault().intValue());
        assertEquals(20, RusticConfig.SLATE_VEIN_SIZE.getDefault().intValue());
        assertEquals(0.03D, RusticConfig.OLIVE_GEN_CHANCE.getDefault(), 0.0001D);
        assertEquals(5, RusticConfig.MAX_OLIVE_GEN_ATTEMPTS.getDefault().intValue());
        assertEquals(0.015D, RusticConfig.IRONWOOD_GEN_CHANCE.getDefault(), 0.0001D);
        assertEquals(4, RusticConfig.MAX_IRONWOOD_GEN_ATTEMPTS.getDefault().intValue());
        assertEquals(0.125D, RusticConfig.HERB_GEN_CHANCE.getDefault(), 0.0001D);
        assertEquals(8, RusticConfig.MAX_HERB_ATTEMPTS.getDefault().intValue());
        assertEquals(0.05D, RusticConfig.WILDBERRY_GEN_CHANCE.getDefault(), 0.0001D);
        assertEquals(4, RusticConfig.MAX_WILDBERRY_ATTEMPTS.getDefault().intValue());

        assertEquals(1.0D, RusticConfig.BEE_GROWTH_MULTIPLIER.getDefault(), 0.0001D);
        assertEquals(1.0D, RusticConfig.BEE_REPRODUCTION_MULTIPLIER.getDefault(), 0.0001D);
        assertEquals(1.0D, RusticConfig.BEE_HONEYCOMB_MULTIPLIER.getDefault(), 0.0001D);

        assertTrue(RusticConfig.FLESH_SMELTING.getDefault());
        assertTrue(RusticConfig.EXTRA_ARMOR_HUD.getDefault());
        assertTrue(RusticConfig.TOUGHNESS_HUD.getDefault());
        assertTrue(RusticConfig.OFFSET_WILDBERRY_BUSHES.getDefault());
        assertTrue(RusticConfig.ENABLE_SLATE.getDefault());
        assertTrue(RusticConfig.ENABLE_PILLARS.getDefault());
        assertTrue(RusticConfig.ENABLE_CLAY_WALLS.getDefault());
        assertTrue(RusticConfig.ENABLE_PAINTED_WOOD.getDefault());
        assertTrue(RusticConfig.ENABLE_TABLES.getDefault());
        assertTrue(RusticConfig.ENABLE_CHAIRS.getDefault());
        assertTrue(RusticConfig.ENABLE_LATTICE.getDefault());
        assertTrue(RusticConfig.ENABLE_OLIVE_OILING.getDefault());
        assertTrue(RusticConfig.ENABLE_SEED_DROPS.getDefault());
        assertTrue(RusticConfig.ENABLE_BOTTLE_EMPTYING.getDefault());
        assertFalse(RusticConfig.OLIVE_OIL_USE_WHITELIST.getDefault());
        assertFalse(RusticConfig.GRAPE_DROP_NEEDS_TOOL.getDefault());
        assertEquals(7, RusticConfig.SEED_DROP_RATE.getDefault().intValue());

        assertEquals(-1, RusticConfig.MIN_BREW_QUALITY_CHANGE.getDefault().intValue());
        assertEquals(4, RusticConfig.MAX_BREW_QUALITY_CHANGE.getDefault().intValue());
        assertEquals(12000, RusticConfig.MAX_BREW_TIME.getDefault().intValue());

        assertTrue(RusticConfig.ENABLE_SILVER_DECOR.getDefault());
    }

    @Test
    public void shippedListsMatchTheLegacyDefaults() {
        assertEquals(List.of("minecraft:slime", "minecraft:shulker"),
                RusticConfig.IRONSKIN_RENDER_ENTITY_BLACKLIST.getDefault());
        assertEquals(List.of("minecraft:iron_hoe", "minecraft:diamond_hoe"),
                RusticConfig.GRAPE_TOOL_WHITELIST.getDefault());

        assertEquals(List.of("#c:rods/wooden", "minecraft:bone", "tconstruct:broadsword", "tconstruct:longsword",
                "tconstruct:rapier", "tconstruct:frypan", "tconstruct:battlesign", "tconstruct:cleaver"),
                RusticConfig.VANTA_OIL_WHITELIST.getDefault());
        assertTrue(RusticConfig.OLIVE_OIL_BLACKLIST.getDefault().isEmpty());

        assertEquals(List.of(0), RusticConfig.OVERWORLD_GENERATION_WHITELIST.getDefault());
        assertEquals(List.of(-1), RusticConfig.NETHER_GENERATION_WHITELIST.getDefault());
    }

    @Test
    public void theOliveOilBlacklistIsAllowedToStayEmpty() {
        assertTrue(RusticConfig.OLIVE_OIL_BLACKLIST.getSpec().test(List.of()));
        assertTrue(RusticConfig.OLIVE_OIL_BLACKLIST.getSpec().test(List.of("minecraft:apple")));
        assertFalse(RusticConfig.OLIVE_OIL_BLACKLIST.getSpec().test(List.of("  ")),
                "blank entries are rejected");
    }

    @Test
    public void commentsAreTheLegacyTextWithNeoForgesMetadataLinesAppended() {
        String brewTime = RusticConfig.MAX_BREW_TIME.getSpec().getComment();
        assertTrue(brewTime.startsWith("how long it should take for a brewing barrel to finish a brew, in ticks"),
                "unexpected comment: " + brewTime);
        assertTrue(brewTime.contains("Default: 12000"), "unexpected comment: " + brewTime);
        assertTrue(brewTime.contains("Range: 1200 ~ 120000"), "unexpected comment: " + brewTime);
        assertFalse(brewTime.contains("[range:"),
                "the legacy suffix must not be spelled out by hand as well");
        assertFalse(RusticConfig.SLATE_VEIN_SIZE.getSpec().getComment().contains("[range:"));

        assertEquals("enable/disable all slate blocks and world gen", RusticConfig.ENABLE_SLATE.getSpec().getComment());
        assertEquals("add entity ids to this list to disable the iron skin visual effect for the specified entities",
                RusticConfig.IRONSKIN_RENDER_ENTITY_BLACKLIST.getSpec().getComment());

        assertEquals("if enabled, allows the armor meter to go beyond one row\n"
                + "only one extra row will ever be rendered, because the armor stat is naturally capped at 30",
                RusticConfig.EXTRA_ARMOR_HUD.getSpec().getComment());
    }

    @Test
    public void everyRangedPropertyKeepsTheLegacyBounds() {
        assertBounds(RusticConfig.SEED_DROP_RATE, 1, 100);
        assertBounds(RusticConfig.MIN_BREW_QUALITY_CHANGE, -50, 50);
        assertBounds(RusticConfig.MAX_BREW_QUALITY_CHANGE, -50, 50);
        assertBounds(RusticConfig.MAX_BREW_TIME, 1200, 120000);
        assertBounds(RusticConfig.SLATE_VEINS_PER_CHUNK, 0, 30);
        assertBounds(RusticConfig.SLATE_VEIN_SIZE, 0, 25);
        assertBounds(RusticConfig.MAX_OLIVE_GEN_ATTEMPTS, 0, 128);
        assertBounds(RusticConfig.MAX_IRONWOOD_GEN_ATTEMPTS, 0, 128);
        assertBounds(RusticConfig.MAX_HERB_ATTEMPTS, 0, 128);
        assertBounds(RusticConfig.MAX_WILDBERRY_ATTEMPTS, 0, 128);
        assertBounds(RusticConfig.MAX_BEEHIVE_ATTEMPTS, 0, 128);

        assertBounds(RusticConfig.BEEHIVE_GEN_CHANCE, 0.0D, 1.0D);
        assertBounds(RusticConfig.OLIVE_GEN_CHANCE, 0.0D, 1.0D);
        assertBounds(RusticConfig.IRONWOOD_GEN_CHANCE, 0.0D, 1.0D);
        assertBounds(RusticConfig.HERB_GEN_CHANCE, 0.0D, 1.0D);
        assertBounds(RusticConfig.WILDBERRY_GEN_CHANCE, 0.0D, 1.0D);
        assertBounds(RusticConfig.BEE_GROWTH_MULTIPLIER, 0.0D, 10.0D);
        assertBounds(RusticConfig.BEE_REPRODUCTION_MULTIPLIER, 0.0D, 10.0D);
        assertBounds(RusticConfig.BEE_HONEYCOMB_MULTIPLIER, 0.0D, 10.0D);

        assertNull(RusticConfig.ENABLE_SLATE.getSpec().getRange());
        assertNull(RusticConfig.VANTA_OIL_WHITELIST.getSpec().getRange());
    }

    @Test
    public void outOfRangeNumbersClampToTheSameBoundsAsForgeDid() {
        assertEquals(120000, clampedInt(RusticConfig.MAX_BREW_TIME, 500000));
        assertEquals(1200, clampedInt(RusticConfig.MAX_BREW_TIME, 50));
        assertEquals(1, clampedInt(RusticConfig.SEED_DROP_RATE, 0));
        assertEquals(-50, clampedInt(RusticConfig.MIN_BREW_QUALITY_CHANGE, -999));
        assertEquals(50, clampedInt(RusticConfig.MAX_BREW_QUALITY_CHANGE, 999));
        assertEquals(128, clampedInt(RusticConfig.MAX_BEEHIVE_ATTEMPTS, 129));
        assertEquals(25, clampedInt(RusticConfig.SLATE_VEIN_SIZE, 26));
        assertEquals(10.0D, clampedDouble(RusticConfig.BEE_GROWTH_MULTIPLIER, 11.0D), 0.0001D);
        assertEquals(10.0D, clampedDouble(RusticConfig.BEE_HONEYCOMB_MULTIPLIER, 99.0D), 0.0001D);

        assertEquals(12000, clampedInt(RusticConfig.MAX_BREW_TIME, 12000));
        assertTrue(RusticConfig.MAX_BREW_TIME.getSpec().test(6000));
        assertFalse(RusticConfig.MAX_BREW_TIME.getSpec().test(50),
                "this is what makes the server log the clamping line");
    }

    @Test
    public void garbageNumbersFallBackToTheDefaultWithoutThrowing() {
        assertFalse(RusticConfig.MAX_BREW_TIME.getSpec().test("banana"));
        assertEquals(12000, clampedInt(RusticConfig.MAX_BREW_TIME, "banana"));
        assertEquals(20, clampedInt(RusticConfig.SLATE_VEIN_SIZE, "banana"));
    }

    @Test
    public void oneBadDimensionIdNoLongerTakesTheOtherListDownWithIt() {
        assertTrue(RusticConfig.OVERWORLD_GENERATION_WHITELIST.getSpec().test(List.of(0, 2, 7)));
        assertFalse(RusticConfig.OVERWORLD_GENERATION_WHITELIST.getSpec().test(List.of("banana")));

        assertEquals(List.of(0), RusticConfig.OVERWORLD_GENERATION_WHITELIST.getSpec().correct(List.of("banana")));

        assertEquals(List.of(-1), RusticConfig.NETHER_GENERATION_WHITELIST.getDefault());
    }

    @Test
    public void theRunningModContainerLoadsTheSpecAndTheValuesReadThrough() {
        assertTrue(RusticConfig.SPEC.isLoaded(), "the config was never loaded, so the mod constructor did not run");

        assertEquals(12000, RusticConfig.MAX_BREW_TIME.getAsInt());
        assertEquals(7, RusticConfig.SEED_DROP_RATE.getAsInt());
        assertEquals(1.0D, RusticConfig.BEE_HONEYCOMB_MULTIPLIER.getAsDouble(), 0.0001D);
        assertTrue(RusticConfig.ENABLE_SLATE.getAsBoolean());
        assertFalse(RusticConfig.NETHER_SLATE.getAsBoolean());
        assertEquals(List.of("minecraft:slime", "minecraft:shulker"), RusticConfig.IRONSKIN_RENDER_ENTITY_BLACKLIST.get());
        assertEquals(List.of(0), RusticConfig.OVERWORLD_GENERATION_WHITELIST.get());
        assertEquals(List.of(-1), RusticConfig.NETHER_GENERATION_WHITELIST.get());
        assertTrue(RusticConfig.OLIVE_OIL_BLACKLIST.get().isEmpty());
    }

    private static int countIn(String category) {
        return (int) ALL.stream().map(ConfigValue::getPath).filter(path -> path.get(0).equals(category)).count();
    }

    private static int clampedInt(ModConfigSpec.IntValue value, Object input) {
        return (Integer) value.getSpec().correct(input);
    }

    private static double clampedDouble(ModConfigSpec.DoubleValue value, Object input) {
        return (Double) value.getSpec().correct(input);
    }

    private static void assertBounds(ModConfigSpec.IntValue value, int min, int max) {
        Range<Integer> range = value.getSpec().getRange();
        assertEquals(Integer.valueOf(min), range.getMin(), value.getPath() + " min");
        assertEquals(Integer.valueOf(max), range.getMax(), value.getPath() + " max");
    }

    private static void assertBounds(ModConfigSpec.DoubleValue value, double min, double max) {
        Range<Double> range = value.getSpec().getRange();
        assertEquals(Double.valueOf(min), range.getMin(), value.getPath() + " min");
        assertEquals(Double.valueOf(max), range.getMax(), value.getPath() + " max");
    }
}
