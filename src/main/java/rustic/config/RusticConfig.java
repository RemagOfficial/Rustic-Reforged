package rustic.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Port of the 1.12.2 {@code rustic.common.Config} (Forge {@code Configuration} + {@code Property})
 * onto NeoForge's {@link ModConfigSpec}.
 *
 * <p>Every entry keeps the legacy Java field name so ported call sites still read like the
 * 1.12.2 reference, while the TOML key itself is lower_snake_case, which is the idiomatic form for
 * a TOML config (the old file used human-readable keys with spaces). Defaults, bounds and comment
 * text are copied verbatim from the 1.12.2 source, which is what
 * {@code porting/../PORT_BASELINE.md} documents as the golden config baseline.
 *
 * <p>List entries also carry an element supplier: the deprecated {@code defineList} overload without
 * one cannot grow a list from NeoForge's in-game config screen, and every Rustic list is meant to be
 * user-editable. The supplied sample is the value the "+" button inserts.
 *
 * <p>Registered as {@link net.neoforged.fml.config.ModConfig.Type#COMMON} in {@code Rustic}, so the
 * file lands at {@code config/rustic-common.toml}.
 */
public final class RusticConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ------------------------------------------------------------------
    // all.general -> [general]
    // ------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue FLESH_SMELTING;
    public static final ModConfigSpec.BooleanValue EXTRA_ARMOR_HUD;
    public static final ModConfigSpec.BooleanValue TOUGHNESS_HUD;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IRONSKIN_RENDER_ENTITY_BLACKLIST;
    public static final ModConfigSpec.BooleanValue OFFSET_WILDBERRY_BUSHES;
    public static final ModConfigSpec.BooleanValue ENABLE_SLATE;
    public static final ModConfigSpec.BooleanValue ENABLE_PILLARS;
    public static final ModConfigSpec.BooleanValue ENABLE_CLAY_WALLS;
    public static final ModConfigSpec.BooleanValue ENABLE_PAINTED_WOOD;
    public static final ModConfigSpec.BooleanValue ENABLE_TABLES;
    public static final ModConfigSpec.BooleanValue ENABLE_CHAIRS;
    public static final ModConfigSpec.BooleanValue ENABLE_LATTICE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> OLIVE_OIL_BLACKLIST;
    public static final ModConfigSpec.BooleanValue OLIVE_OIL_USE_WHITELIST;
    public static final ModConfigSpec.BooleanValue ENABLE_OLIVE_OILING;
    public static final ModConfigSpec.BooleanValue GRAPE_DROP_NEEDS_TOOL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GRAPE_TOOL_WHITELIST;
    public static final ModConfigSpec.BooleanValue ENABLE_SEED_DROPS;
    public static final ModConfigSpec.IntValue SEED_DROP_RATE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VANTA_OIL_WHITELIST;
    public static final ModConfigSpec.IntValue MIN_BREW_QUALITY_CHANGE;
    public static final ModConfigSpec.IntValue MAX_BREW_QUALITY_CHANGE;
    public static final ModConfigSpec.IntValue MAX_BREW_TIME;
    public static final ModConfigSpec.BooleanValue ENABLE_BOTTLE_EMPTYING;

    // ------------------------------------------------------------------
    // all.world -> [world]
    // ------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue NETHER_SLATE;
    public static final ModConfigSpec.IntValue SLATE_VEINS_PER_CHUNK;
    public static final ModConfigSpec.IntValue SLATE_VEIN_SIZE;
    public static final ModConfigSpec.DoubleValue OLIVE_GEN_CHANCE;
    public static final ModConfigSpec.IntValue MAX_OLIVE_GEN_ATTEMPTS;
    public static final ModConfigSpec.DoubleValue IRONWOOD_GEN_CHANCE;
    public static final ModConfigSpec.IntValue MAX_IRONWOOD_GEN_ATTEMPTS;
    public static final ModConfigSpec.BooleanValue NATURAL_TREE_PLACEMENT;
    public static final ModConfigSpec.DoubleValue HERB_GEN_CHANCE;
    public static final ModConfigSpec.IntValue MAX_HERB_ATTEMPTS;
    public static final ModConfigSpec.DoubleValue WILDBERRY_GEN_CHANCE;
    public static final ModConfigSpec.IntValue MAX_WILDBERRY_ATTEMPTS;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> OVERWORLD_GENERATION_WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> NETHER_GENERATION_WHITELIST;

    // ------------------------------------------------------------------
    // all.bees -> [bees]
    // ------------------------------------------------------------------
    public static final ModConfigSpec.DoubleValue BEEHIVE_GEN_CHANCE;
    public static final ModConfigSpec.IntValue MAX_BEEHIVE_ATTEMPTS;
    public static final ModConfigSpec.DoubleValue BEE_GROWTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BEE_REPRODUCTION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BEE_HONEYCOMB_MULTIPLIER;

    // ------------------------------------------------------------------
    // all.compat -> [compat]
    // ------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue ENABLE_SILVER_DECOR;
    // ENABLE_FORESTRY_COMPAT was dropped in S10.4: the Forestry CE compat ships as plain datapack data
    // (forestry:squeezer / forestry:fermenter recipes + a forestry:biogas_fuels fluid data map). Data of an
    // absent mod is inert - unknown recipe types are skipped with a log warning, unregistered data map types
    // are never read - so there is nothing a toggle could protect and no Java glue to guard.

    static {
        BUILDER.push("General Options");

        FLESH_SMELTING = BUILDER
                .comment("enable smelting rotten flesh into tallow")
                .define("flesh_smelting", true);

        EXTRA_ARMOR_HUD = BUILDER
                .comment("if enabled, allows the armor meter to go beyond one row",
                        "only one extra row will ever be rendered, because the armor stat is naturally capped at 30")
                .define("extra_armor_hud", true);

        TOUGHNESS_HUD = BUILDER
                .comment("if enabled, adds a hud elemnt over the hunger meter to show armor toughness, if applicable")
                .define("armor_toughness_hud", true);

        IRONSKIN_RENDER_ENTITY_BLACKLIST = BUILDER
                .comment("add entity ids to this list to disable the iron skin visual effect for the specified entities")
                .defineList("ironskin_render_entity_blacklist",
                        List.of("minecraft:slime", "minecraft:shulker"),
                        () -> "minecraft:zombie",
                        RusticConfig::isNonBlankString);

        OFFSET_WILDBERRY_BUSHES = BUILDER
                .comment("enable/disable the random offset added to wildberry bush models")
                .define("wildberry_bush_offset", true);

        ENABLE_SLATE = BUILDER
                .comment("enable/disable all slate blocks and world gen")
                .define("enable_slate", true);

        ENABLE_PILLARS = BUILDER
                .comment("enable/disable all stone pillar blocks")
                .define("enable_stone_pillars", true);

        ENABLE_CLAY_WALLS = BUILDER
                .comment("enable/disable all clay wall blocks")
                .define("enable_clay_walls", true);

        ENABLE_PAINTED_WOOD = BUILDER
                .comment("enable/disable all painted wood blocks")
                .define("enable_painted_wood", true);

        ENABLE_TABLES = BUILDER
                .comment("enable/disable all table blocks")
                .define("enable_tables", true);

        ENABLE_CHAIRS = BUILDER
                .comment("enable/disable all chair blocks")
                .define("enable_chairs", true);

        ENABLE_LATTICE = BUILDER
                .comment("enable/disable lattice blocks")
                .define("enable_lattice", true);

        // Ships empty on purpose (the 1.12.2 default was an empty list), so this one uses the
        // allow-empty overload: defineList() documents "this list cannot be empty".
        OLIVE_OIL_BLACKLIST = BUILDER
                .comment("add an item's registry name to this list to prevent it from being craftable with olive oil")
                .defineListAllowEmpty("olive_oil_food_blacklist", List.<String>of(), () -> "minecraft:apple",
                        RusticConfig::isNonBlankString);

        OLIVE_OIL_USE_WHITELIST = BUILDER
                .comment("treat \"olive_oil_food_blacklist\" as a whitelist instead")
                .define("treat_olive_oil_blacklist_as_whitelist", false);

        ENABLE_OLIVE_OILING = BUILDER
                .comment("enable/disable the ability to add olive oil to food")
                .define("enable_olive_oiling", true);

        GRAPE_DROP_NEEDS_TOOL = BUILDER
                .comment("with this value set to true, vines will only drop grape seeds when broken with tools from the whitelist")
                .define("grapeseed_drops_require_tool", false);

        GRAPE_TOOL_WHITELIST = BUILDER
                .comment("add an item's registry name to this list to allow vines to drop grape seeds when broken with it")
                .defineList("grapeseed_tool_whitelist",
                        List.of("minecraft:iron_hoe", "minecraft:diamond_hoe"),
                        () -> "minecraft:wooden_hoe",
                        RusticConfig::isNonBlankString);

        ENABLE_SEED_DROPS = BUILDER
                .comment("set this to false to prevent any of Rustic's seeds from dropping from grass or vines")
                .define("enable_seed_drops", true);

        SEED_DROP_RATE = BUILDER
                .comment("decrease this number to make seeds more difficult to find (10 is wheat seed rarity)")
                .defineInRange("seed_drop_rate", 7, 1, 100);

        VANTA_OIL_WHITELIST = BUILDER
                .comment("add an item's registry name to this list to make it craftable with vanta oil and elixirs",
                        "entries starting with # are item tags, which is how ore dictionary names become 1.21 tags")
                .defineList("vanta_oil_whitelist",
                        List.of("#stickWood", "minecraft:bone", "tconstruct:broadsword", "tconstruct:longsword",
                                "tconstruct:rapier", "tconstruct:frypan", "tconstruct:battlesign", "tconstruct:cleaver"),
                        () -> "minecraft:stick",
                        RusticConfig::isNonBlankString);

        MIN_BREW_QUALITY_CHANGE = BUILDER
                .comment("the minimum amount of increase that booze culture will provide to the new brew, in percent")
                .defineInRange("minimum_change_to_brew_quality", -1, -50, 50);

        MAX_BREW_QUALITY_CHANGE = BUILDER
                .comment("the maximum amount of increase that booze culture will provide to the new brew, in percent")
                .defineInRange("maximum_change_to_brew_quality", 4, -50, 50);

        MAX_BREW_TIME = BUILDER
                .comment("how long it should take for a brewing barrel to finish a brew, in ticks")
                .defineInRange("brewing_time", 12000, 1200, 120000);

        ENABLE_BOTTLE_EMPTYING = BUILDER
                .comment("set this to false if you experience any issues with Rustic's glass bottle emptying recipe")
                .define("enable_bottle_emptying", true);

        BUILDER.pop();
        BUILDER.push("World Generation Options");

        NETHER_SLATE = BUILDER
                .comment("if set to true, slate will generate in the nether instead of the overworld")
                .define("nether_slate", false);

        SLATE_VEINS_PER_CHUNK = BUILDER
                .comment("number of times the generator will try to place a slate vein per chunk")
                .defineInRange("slate_veins_per_chunk", 5, 0, 30);

        SLATE_VEIN_SIZE = BUILDER
                .comment("number of blocks per slate vein")
                .defineInRange("slate_vein_size", 20, 0, 25);

        OLIVE_GEN_CHANCE = BUILDER
                .comment("chance for olive trees to try to generate in a chunk")
                .defineInRange("olive_tree_generation_chance", 0.03D, 0.0D, 1.0D);

        MAX_OLIVE_GEN_ATTEMPTS = BUILDER
                .comment("maximum number of times the generator will attempt to place an olive tree in a chunk")
                .defineInRange("max_olive_tree_generation_attempts", 5, 0, 128);

        IRONWOOD_GEN_CHANCE = BUILDER
                .comment("chance for ironwood trees to try to generate in a chunk")
                .defineInRange("ironwood_generation_chance", 0.015D, 0.0D, 1.0D);

        MAX_IRONWOOD_GEN_ATTEMPTS = BUILDER
                .comment("maximum number of times the generator will attempt to place an ironwood tree in a chunk")
                .defineInRange("max_ironwood_generation_attempts", 4, 0, 128);

        // 1.21-only addition; there is no 1.12.2 counterpart, hence it lives outside the golden baseline pair.
        // Default false preserves the exact legacy behaviour so the port stays faithful until a user opts in.
        NATURAL_TREE_PLACEMENT = BUILDER
                .comment("when true, olive and ironwood trees roll their generation chance once per scatter attempt",
                        "spreading a low density of trees evenly across the biome for a more natural forest;",
                        "when false the 1.12.2 behaviour is reproduced: a single roll per chunk decides everything,",
                        "so a full burst of trees lands in the few chunks that win and the rest stay bare (clumpy).")
                .define("natural_tree_placement", false);

        HERB_GEN_CHANCE = BUILDER
                .comment("chance for an herb to try to generate in a chunk")
                .defineInRange("herb_generation_chance", 0.125D, 0.0D, 1.0D);

        MAX_HERB_ATTEMPTS = BUILDER
                .comment("maximum number of times the generator will attempt to place an herb in a chunk")
                .defineInRange("max_herb_generation_attempts", 8, 0, 128);

        WILDBERRY_GEN_CHANCE = BUILDER
                .comment("chance for wildberry bushes to try to generate in a chunk")
                .defineInRange("wildberry_generation_chance", 0.05D, 0.0D, 1.0D);

        MAX_WILDBERRY_ATTEMPTS = BUILDER
                .comment("maximum number of times the generator will attempt to place a wildberry bush in a chunk")
                .defineInRange("max_wildberry_generation_attempts", 4, 0, 128);

        // 1.12.2 stored these as strings and Integer.parseInt'd them inside a catch-all block, so one
        // bad entry silently aborted the whole whitelist. TOML keeps them as real integers instead.
        OVERWORLD_GENERATION_WHITELIST = BUILDER
                .comment("add numerical dimension ids to this list to allow Rustic's overworld world gen to occur in those dimensions",
                        "dimensions that are not listed here will not receive Rustic's overworld world generation")
                .defineList("overworld_generation_dimension_whitelist", List.of(0), () -> 0,
                        RusticConfig::isInteger);

        NETHER_GENERATION_WHITELIST = BUILDER
                .comment("add numerical dimension ids to this list to allow Rustic's nether world gen to occur in those dimensions",
                        "dimensions that are not listed here will not receive Rustic's nether world generation")
                .defineList("nether_generation_dimension_whitelist", List.of(-1), () -> -1,
                        RusticConfig::isInteger);

        BUILDER.pop();
        BUILDER.push("Bee Related Options");

        BEEHIVE_GEN_CHANCE = BUILDER
                .comment("chance for beehives to try to generate in a chunk")
                .defineInRange("beehive_generation_chance", 0.03D, 0.0D, 1.0D);

        MAX_BEEHIVE_ATTEMPTS = BUILDER
                .comment("maximum number of times the generator will attempt to place a beehive in a chunk")
                .defineInRange("max_beehive_generation_attempts", 3, 0, 128);

        BEE_GROWTH_MULTIPLIER = BUILDER
                .comment("higher values increase the frequency with which apiaries forcibly age a crop")
                .defineInRange("bee_crop_boost_multiplier", 1.0D, 0.0D, 10.0D);

        BEE_REPRODUCTION_MULTIPLIER = BUILDER
                .comment("the time it takes for an apiary to produce a new bee is multiplied by this value",
                        "LARGER numbers make bees reproduce LESS often")
                .defineInRange("bee_reproduction_multiplier", 1.0D, 0.0D, 10.0D);

        BEE_HONEYCOMB_MULTIPLIER = BUILDER
                .comment("the time it takes for an apiary to produce a honeycomb is multiplied by this value",
                        "LARGER numbers make bees produce honeycomb LESS often")
                .defineInRange("bee_honeycomb_multiplier", 1.0D, 0.0D, 10.0D);

        BUILDER.pop();
        BUILDER.push("Mod Compatability Related Options");

        ENABLE_SILVER_DECOR = BUILDER
                .comment("set this to false to disable silver chain, chandelier, candle, and lantern blocks")
                .define("enable_silver_decoration_blocks", true);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean isNonBlankString(Object value) {
        return value instanceof String string && !string.isBlank();
    }

    private static boolean isInteger(Object value) {
        return value instanceof Integer;
    }

    private RusticConfig() {
    }
}
