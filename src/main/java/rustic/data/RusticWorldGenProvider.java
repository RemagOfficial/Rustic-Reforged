package rustic.data;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import rustic.Rustic;
import rustic.content.blocks.BerryBushBlock;
import rustic.content.blocks.HerbBlock;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticBlocks.WoodFamily;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.world.RusticConfigGatedFeature;
import rustic.content.world.RusticGenKnob;
import rustic.content.world.RusticWorldGenFeatures;
import rustic.world.RusticWorldGen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Phase 8 world generation expressed as data. This is the JSON half of the hybrid the plan chose: every Rustic
 * generator becomes a small stack of dynamic-registry entries - a bare {@code minecraft:ore}/{@code minecraft:tree}
 * configured feature describing <em>what</em> to place, a {@code rustic:config_gated} configured feature wrapping it
 * with a live {@link RusticGenKnob} that reproduces 1.12.2's <em>whether / how-often / which-dimension / which-biome</em>
 * decisions, a placed feature the generator can hand an origin to, and a {@code neoforge:add_features} biome modifier
 * that wires it into the right biomes.
 *
 * <p>The three layers exist because the legacy {@code WorldGeneratorRustic} made all four decisions in code while
 * 1.21 wants the placement declared as data. Keeping the block set / tree shape in the bare feature and the config
 * routing in the gate lets a datapack retarget a tree or herb set without touching code, while the shipped toggles
 * ({@code ENABLE_SLATE}, the generation chances and attempt caps, the dimension whitelists) keep working live - the
 * gate re-reads {@link rustic.config.RusticConfig} on every {@code place()}.
 *
 * <p>Slice S8.2 emits the slate ore and the olive/ironwood trees (plus the sapling-only apple tree the legacy
 * {@code WorldGenAllTrees} never placed biomedly); S8.3 adds the eight surface-herb categories, the cave and nether
 * herb sets, wildberries and the beehive. The herb and berry bodies are {@code minecraft:simple_block} rather than
 * {@code minecraft:random_patch} because the gate already owns the scatter loop - see
 * {@link #bootstrapConfiguredFeatures} - and {@code simple_block}'s {@code canSurvive} test is the legacy
 * {@code canSustainPlant} check the old generators ran before setting the block.
 */
public class RusticWorldGenProvider extends DatapackBuiltinEntriesProvider {

    /**
     * Slate vein size baked into the ore JSON. This mirrors the shipped {@code slate_vein_size} config default (20):
     * the value is a static {@code minecraft:ore} property that the datapack owns, not a live knob, and it cannot be
     * read through {@code RusticConfig} here because datagen runs before the config file is loaded.
     */
    private static final int SLATE_VEIN_SIZE = 20;

    // ---- Slate ore: bare minecraft:ore configured features ----
    private static final ResourceKey<ConfiguredFeature<?, ?>> SLATE_ORE_OVERWORLD =
            configuredKey("slate_ore_overworld");
    private static final ResourceKey<ConfiguredFeature<?, ?>> SLATE_ORE_NETHER =
            configuredKey("slate_ore_nether");

    // ---- Gated wrappers (rustic:config_gated) for every biome-placed generator ----
    private static final ResourceKey<ConfiguredFeature<?, ?>> SLATE_ORE_OVERWORLD_GATED =
            configuredKey("slate_ore_overworld_gated");
    private static final ResourceKey<ConfiguredFeature<?, ?>> SLATE_ORE_NETHER_GATED =
            configuredKey("slate_ore_nether_gated");
    private static final ResourceKey<ConfiguredFeature<?, ?>> OLIVE_TREE_GATED =
            configuredKey("olive_tree_gated");
    private static final ResourceKey<ConfiguredFeature<?, ?>> IRONWOOD_TREE_GATED =
            configuredKey("ironwood_tree_gated");

    // ---- Placed features (the entry point each biome modifier references). placedKey appends _placed, so these
    // paths stay the same stem as their configured features - the shipped S8.2 json names are unchanged.
    private static final ResourceKey<PlacedFeature> SLATE_ORE_OVERWORLD_PLACED =
            placedKey("slate_ore_overworld");
    private static final ResourceKey<PlacedFeature> SLATE_ORE_NETHER_PLACED =
            placedKey("slate_ore_nether");
    private static final ResourceKey<PlacedFeature> OLIVE_TREE_PLACED =
            placedKey("olive_tree");
    private static final ResourceKey<PlacedFeature> IRONWOOD_TREE_PLACED =
            placedKey("ironwood_tree");

    // ---- Biome modifiers (neoforge:add_features) ----
    private static final ResourceKey<BiomeModifier> SLATE_ORE_OVERWORLD_BM =
            biomeModifierKey("slate_ore_overworld");
    private static final ResourceKey<BiomeModifier> SLATE_ORE_NETHER_BM =
            biomeModifierKey("slate_ore_nether");
    private static final ResourceKey<BiomeModifier> OLIVE_TREE_BM =
            biomeModifierKey("olive_tree");
    private static final ResourceKey<BiomeModifier> IRONWOOD_TREE_BM =
            biomeModifierKey("ironwood_tree");

    // ---- S8.3 herbs / wildberries / beehive: one generator stack per legacy biome category ----
    //
    // The legacy WorldGeneratorRustic picked the herb set at run time through
    // Herbs.getRandomHerbForBiome, an if/else-if chain that returned the first category the chunk biome matched.
    // Here the same chain becomes data: one bare minecraft:simple_block feature per category whose
    // minecraft:weighted_state_provider lists exactly that category's herbs, and a biome modifier per category that
    // targets the matching NeoForge biome tag. The shared SURFACE_HERB gate carries the rules the legacy generator
    // checked itself (!snowy, the chance roll, the MAX_HERB_ATTEMPTS scatter loop), so its any-of list is empty -
    // the category identity comes from the modifier's biome tag, which is why no two category tags may overlap.
    static final String[] SURFACE_HERB_PATHS = {
            "herbs_jungle", "herbs_sandy", "herbs_savanna", "herbs_mesa", "herbs_mountain",
            "herbs_swamp", "herbs_forest", "herbs_plains"};
    /**
     * The eight bare surface-herb features, positionally matched to {@link #SURFACE_HERB_PATHS} and
     * {@link #SURFACE_HERB_BIOME_TAGS}. {@code List.of} infers the element type from the cast target here, so no
     * generic-array creation (and no unchecked cast) is needed.
     */
    static final ResourceKey<ConfiguredFeature<?, ?>>[] SURFACE_HERB_BARE = List.of(
            configuredKey("herbs_jungle"), configuredKey("herbs_sandy"), configuredKey("herbs_savanna"),
            configuredKey("herbs_mesa"), configuredKey("herbs_mountain"), configuredKey("herbs_swamp"),
            configuredKey("herbs_forest"), configuredKey("herbs_plains")).toArray(ResourceKey[]::new);
    static final ResourceKey<ConfiguredFeature<?, ?>>[] SURFACE_HERB_GATED = gatedKeys(SURFACE_HERB_PATHS);
    static final ResourceKey<PlacedFeature>[] SURFACE_HERB_PLACED = placedKeys(SURFACE_HERB_PATHS);
    static final ResourceKey<BiomeModifier>[] SURFACE_HERB_BM = biomeModifierKeys(SURFACE_HERB_PATHS);

    /** The biome tag each surface-herb category is added to, positionally matched to {@link #SURFACE_HERB_BARE}. */
    private static final TagKey<Biome>[] SURFACE_HERB_BIOME_TAGS = tags(
            Tags.Biomes.IS_JUNGLE, Tags.Biomes.IS_SANDY, Tags.Biomes.IS_SAVANNA, Tags.Biomes.IS_BADLANDS,
            Tags.Biomes.IS_MOUNTAIN, Tags.Biomes.IS_SWAMP, Tags.Biomes.IS_FOREST, Tags.Biomes.IS_PLAINS);

    private static final ResourceKey<ConfiguredFeature<?, ?>> CAVE_HERBS = configuredKey("herbs_cave");
    private static final ResourceKey<ConfiguredFeature<?, ?>> CAVE_HERBS_GATED = gatedKey("herbs_cave");
    private static final ResourceKey<PlacedFeature> CAVE_HERBS_PLACED = placedKey("herbs_cave");
    private static final ResourceKey<BiomeModifier> CAVE_HERBS_BM = biomeModifierKey("herbs_cave");

    private static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_HERBS = configuredKey("herbs_nether");
    private static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_HERBS_GATED = gatedKey("herbs_nether");
    private static final ResourceKey<PlacedFeature> NETHER_HERBS_PLACED = placedKey("herbs_nether");
    private static final ResourceKey<BiomeModifier> NETHER_HERBS_BM = biomeModifierKey("herbs_nether");

    private static final ResourceKey<ConfiguredFeature<?, ?>> WILDBERRIES = configuredKey("wildberries");
    private static final ResourceKey<ConfiguredFeature<?, ?>> WILDBERRIES_GATED = gatedKey("wildberries");
    private static final ResourceKey<PlacedFeature> WILDBERRIES_PLACED = placedKey("wildberries");
    private static final ResourceKey<BiomeModifier> WILDBERRIES_BM = biomeModifierKey("wildberries");

    /**
     * The beehive is the one generator with no data half to declare: it places exactly one block after a structural
     * walk-down through leaves, so {@link RusticBeehiveFeature} carries the block, the {@code rustic:beehive}
     * scatter and the whole {@link RusticGenKnob#BEEHIVE} gate itself. Its configured feature is therefore bare -
     * no {@code rustic:config_gated} wrapper in front of it.
     */
    private static final ResourceKey<ConfiguredFeature<?, ?>> BEEHIVE = configuredKey("beehive");
    private static final ResourceKey<PlacedFeature> BEEHIVE_PLACED = placedKey("beehive");
    private static final ResourceKey<BiomeModifier> BEEHIVE_BM = biomeModifierKey("beehive");

    public RusticWorldGenProvider(PackOutput output, CompletableFuture<net.minecraft.core.HolderLookup.Provider> registries) {
        super(output, registries, new RegistrySetBuilder()
                        .add(Registries.CONFIGURED_FEATURE, RusticWorldGenProvider::bootstrapConfiguredFeatures)
                        .add(Registries.PLACED_FEATURE, RusticWorldGenProvider::bootstrapPlacedFeatures)
                        .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, RusticWorldGenProvider::bootstrapBiomeModifiers),
                Set.of(Rustic.MODID));
    }

    /**
     * The bare data features (what to place) and the {@code rustic:config_gated} wrappers that gate them. The bare
     * tree keys are {@link RusticWorldGen#OLIVE_TREE}/{@link RusticWorldGen#IRONWOOD_TREE}/
     * {@link RusticWorldGen#APPLE_TREE} so a mature sapling resolves the very same configured feature the world
     * generator wraps - the plan's one shared identifier between the code grower and the data tree.
     */
    private static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Slate: minecraft:ore placing the SLATE block. Overworld replaces the stone family (the legacy
        // WorldGenMinable default), the nether replaces netherrack (the legacy NetherPredicate exactly).
        Holder.Reference<ConfiguredFeature<?, ?>> slateOverworld = context.register(SLATE_ORE_OVERWORLD,
                new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(
                        new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
                        RusticBlocks.SLATE.get().defaultBlockState(), SLATE_VEIN_SIZE)));
        context.register(SLATE_ORE_OVERWORLD_GATED, gated(slateOverworld, RusticGenKnob.SLATE_OVERWORLD));

        Holder.Reference<ConfiguredFeature<?, ?>> slateNether = context.register(SLATE_ORE_NETHER,
                new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(
                        new BlockMatchTest(Blocks.NETHERRACK),
                        RusticBlocks.SLATE.get().defaultBlockState(), SLATE_VEIN_SIZE)));
        context.register(SLATE_ORE_NETHER_GATED, gated(slateNether, RusticGenKnob.SLATE_NETHER));

        // Olive and ironwood: the legacy WorldGenAllTrees shapes as minecraft:tree features. StraightTrunkPlacer
        // reproduces the rand(a)+base height exactly (4+rand(3) olive, 7+rand(6) ironwood) and BlobFoliagePlacer
        // reproduces the vanilla-oak corner canopy the legacy l2 = 1 - k2/2 loop drew.
        Holder.Reference<ConfiguredFeature<?, ?>> olive = context.register(RusticWorldGen.OLIVE_TREE,
                new ConfiguredFeature<>(Feature.TREE, oliveTree()));
        context.register(OLIVE_TREE_GATED, gated(olive, RusticGenKnob.OLIVE));

        Holder.Reference<ConfiguredFeature<?, ?>> ironwood = context.register(RusticWorldGen.IRONWOOD_TREE,
                new ConfiguredFeature<>(Feature.TREE, ironwoodTree()));
        context.register(IRONWOOD_TREE_GATED, gated(ironwood, RusticGenKnob.IRONWOOD));

        // The apple tree is sapling-grown only (legacy WorldGenAllTrees never generated it), so it gets a bare
        // configured feature for the grower but no gate and no biome modifier. Its trunk is vanilla OAK_LOG and
        // its foliage the fruit-bearing APPLE_LEAVES, matching BlockLeavesApple's legacy pairing.
        context.register(RusticWorldGen.APPLE_TREE,
                new ConfiguredFeature<>(Feature.TREE, appleTree()));

        // ---- S8.3 surface herbs: the eight categories of Herbs.getRandomHerbForBiome, in that method's order ----
        // Each is a minecraft:simple_block that picks one state from the category's weighted herb list. simple_block
        // is used rather than random_patch because rustic:config_gated already reproduces the legacy scatter loop
        // live (MAX_HERB_ATTEMPTS attempts of x/z = origin + rand(7) - rand(7) at the surface height); wrapping a
        // random_patch would scatter twice and would freeze the attempt count into JSON, losing the config toggle.
        // simple_block's own check is the legacy generateHerb guard: it only sets the block when
        // BlockState#canSurvive passes, i.e. the cell is air above soil that accepts the plant.
        for (int i = 0; i < SURFACE_HERB_BARE.length; i++) {
            Holder.Reference<ConfiguredFeature<?, ?>> bare = context.register(SURFACE_HERB_BARE[i],
                    new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                            new SimpleBlockConfiguration(SURFACE_HERB_SETS[i])));
            context.register(SURFACE_HERB_GATED[i], gated(bare, RusticGenKnob.SURFACE_HERB));
        }

        // Cave herbs (core root, mooncap) and the nether's deathstalk: same simple_block body, the difference is
        // entirely the gate - CAVE_HERB drops from y 10..41 to the first cave floor, NETHER_HERB routes to the
        // nether and drops from y 32..127 with the legacy 24-block walk.
        Holder.Reference<ConfiguredFeature<?, ?>> cave = context.register(CAVE_HERBS,
                new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(caveHerbs())));
        context.register(CAVE_HERBS_GATED, gated(cave, RusticGenKnob.CAVE_HERB));

        Holder.Reference<ConfiguredFeature<?, ?>> nether = context.register(NETHER_HERBS,
                new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(netherHerbs())));
        context.register(NETHER_HERBS_GATED, gated(nether, RusticGenKnob.NETHER_HERB));

        // Wildberries: the legacy WorldGenWildberries placed the bush at rand(6) ages, so the weighted list carries
        // all six ages with equal weight - which is exactly the uniform draw the legacy roll was.
        Holder.Reference<ConfiguredFeature<?, ?>> berries = context.register(WILDBERRIES,
                new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(wildberryBush())));
        context.register(WILDBERRIES_GATED, gated(berries, RusticGenKnob.WILDBERRY));

        // The beehive needs no bare block-set feature; RusticBeehiveFeature places the hive itself after its
        // leaves walk-down, so the configured feature names the code feature directly with an empty configuration.
        context.register(BEEHIVE, new ConfiguredFeature<>(RusticWorldGenFeatures.BEEHIVE.get(),
                NoneFeatureConfiguration.INSTANCE));
    }

    /**
     * Each gated configured feature is exposed to the biome decoration pipeline as a placed feature. The placement
     * list is deliberately empty: unlike vanilla, which scatters and counts via placement modifiers, the whole
     * dimension / chance / attempts / scatter routine lives inside {@link RusticGenKnob} so those values stay live in
     * config. The generator hands the gate one origin per chunk and the gate decides whether and where to place.
     */
    private static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(SLATE_ORE_OVERWORLD_PLACED,
                new PlacedFeature(configured.getOrThrow(SLATE_ORE_OVERWORLD_GATED), List.of()));
        context.register(SLATE_ORE_NETHER_PLACED,
                new PlacedFeature(configured.getOrThrow(SLATE_ORE_NETHER_GATED), List.of()));
        context.register(OLIVE_TREE_PLACED,
                new PlacedFeature(configured.getOrThrow(OLIVE_TREE_GATED), List.of()));
        context.register(IRONWOOD_TREE_PLACED,
                new PlacedFeature(configured.getOrThrow(IRONWOOD_TREE_GATED), List.of()));

        for (int i = 0; i < SURFACE_HERB_PLACED.length; i++) {
            context.register(SURFACE_HERB_PLACED[i], new PlacedFeature(
                    configured.getOrThrow(SURFACE_HERB_GATED[i]), List.of()));
        }
        context.register(CAVE_HERBS_PLACED,
                new PlacedFeature(configured.getOrThrow(CAVE_HERBS_GATED), List.of()));
        context.register(NETHER_HERBS_PLACED,
                new PlacedFeature(configured.getOrThrow(NETHER_HERBS_GATED), List.of()));
        context.register(WILDBERRIES_PLACED,
                new PlacedFeature(configured.getOrThrow(WILDBERRIES_GATED), List.of()));
        // Un-gated: the placed feature points straight at the rustic:beehive configured feature, whose own place()
        // reads RusticGenKnob.BEEHIVE for the dimension / biome / chance / attempts decisions.
        context.register(BEEHIVE_PLACED,
                new PlacedFeature(configured.getOrThrow(BEEHIVE), List.of()));
    }

    /**
     * The biome half of the wiring. Each modifier adds one placed feature to a broad dimension-wide biome tag
     * ({@code #c:is_overworld}/{@code #c:is_nether}); the specific forest/plains/mountain minus snowy (olive) and
     * forest/plains/mountain/swamp/jungle minus dry (ironwood) filtering is the gate's {@link RusticGenKnob} any-of /
     * none-of test, which reads the biome at {@code place()} time. This is what lets one modifier serve a whole
     * category while the shipped biome rules stay in the ported code.
     */
    private static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);

        context.register(SLATE_ORE_OVERWORLD_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                placed.getOrThrow(SLATE_ORE_OVERWORLD_PLACED),
                Decoration.UNDERGROUND_ORES));
        context.register(SLATE_ORE_NETHER_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_NETHER),
                placed.getOrThrow(SLATE_ORE_NETHER_PLACED),
                Decoration.UNDERGROUND_ORES));
        context.register(OLIVE_TREE_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                placed.getOrThrow(OLIVE_TREE_PLACED),
                Decoration.VEGETAL_DECORATION));
        context.register(IRONWOOD_TREE_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                placed.getOrThrow(IRONWOOD_TREE_PLACED),
                Decoration.VEGETAL_DECORATION));

        // One modifier per surface-herb category, each on that category's own biome tag; the wide tag is what
        // replaces the legacy if/else-if chain, since these eight tags are mutually exclusive in vanilla biomes.
        for (int i = 0; i < SURFACE_HERB_PLACED.length; i++) {
            context.register(SURFACE_HERB_BM[i], addFeatures(
                    biomes.getOrThrow(SURFACE_HERB_BIOME_TAGS[i]),
                    placed.getOrThrow(SURFACE_HERB_PLACED[i]),
                    Decoration.VEGETAL_DECORATION));
        }

        // Cave herbs go to every overworld biome - the legacy WorldGenCaveHerbs never asked which biome it was in,
        // it only scattered underground, which is the CAVE scatter's job.
        context.register(CAVE_HERBS_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                placed.getOrThrow(CAVE_HERBS_PLACED),
                Decoration.UNDERGROUND_DECORATION));
        context.register(NETHER_HERBS_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_NETHER),
                placed.getOrThrow(NETHER_HERBS_PLACED),
                Decoration.VEGETAL_DECORATION));
        // Wildberries are added over the whole overworld too: the legacy blacklist (cold/snowy/sandy/savanna/
        // mesa/mushroom/end/nether/dead/wasteland) is the WILDBERRY knob's none-of list, evaluated per chunk.
        context.register(WILDBERRIES_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                placed.getOrThrow(WILDBERRIES_PLACED),
                Decoration.VEGETAL_DECORATION));
        context.register(BEEHIVE_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                placed.getOrThrow(BEEHIVE_PLACED),
                Decoration.VEGETAL_DECORATION));
    }

    // ---- Feature construction helpers ----

    /** Wrap a bare configured feature with the shared {@code rustic:config_gated} gate reading {@code knob}. */
    private static ConfiguredFeature<?, ?> gated(Holder<ConfiguredFeature<?, ?>> inner, RusticGenKnob knob) {
        return new ConfiguredFeature<>(RusticWorldGenFeatures.CONFIG_GATED.get(),
                new RusticConfigGatedFeature.Configuration(inner, knob));
    }

    private static BiomeModifiers.AddFeaturesBiomeModifier addFeatures(
            HolderSet<Biome> biomes, Holder<PlacedFeature> feature, Decoration step) {
        return new BiomeModifiers.AddFeaturesBiomeModifier(biomes, HolderSet.direct(feature), step);
    }

    private static TreeConfiguration oliveTree() {
        WoodFamily olive = RusticBlocks.olive();
        return oakStyleTree(olive.log().get(), olive.leaves().get(), 4, 3);
    }

    private static TreeConfiguration ironwoodTree() {
        WoodFamily ironwood = RusticBlocks.ironwood();
        return oakStyleTree(ironwood.log().get(), ironwood.leaves().get(), 7, 6);
    }

    /**
     * The apple tree: vanilla oak trunk plus the fruit-bearing apple leaves. Height {@code rand(2)+5}, so base 5 and
     * {@code height_rand_a} 2, matching the legacy {@code WorldGenAppleTree}.
     */
    private static TreeConfiguration appleTree() {
        return oakStyleTree(Blocks.OAK_LOG, RusticFarmBlocks.APPLE_LEAVES.get(), 5, 2);
    }

    /**
     * The shared olive/ironwood/apple shape: a {@link StraightTrunkPlacer} of {@code base + rand(randA)} height (the
     * legacy {@code rand(a)+base}) under the given log and leaves, capped with the vanilla-oak {@link BlobFoliagePlacer}
     * canopy that draws the same {@code l2 = 1 - k2/2} corner pattern the legacy tree loops wrote.
     */
    private static TreeConfiguration oakStyleTree(Block log, Block leaves, int base, int randA) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(log),
                new StraightTrunkPlacer(base, randA, 0),
                BlockStateProvider.simple(leaves),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 2),
                new TwoLayersFeatureSize(1, 0, 1))
                .build();
    }

    // ---- Herb / berry state providers ----

    /**
     * The eight surface-herb sets, in {@link Herbs}'s priority order and positionally matched to
     * {@link #SURFACE_HERB_BARE}: jungle {blood orchid, horsetail, marsh mallow, mooncap}, the three aloe vera
     * categories (sandy, savanna, mesa), mountain {wind thistle, cloudsbluff}, swamp {chamomile, horsetail,
     * marsh mallow}, forest {chamomile, cohosh, ginseng, horsetail, vanta lily} and plains {chamomile, ginseng,
     * horsetail, wind thistle, vanta lily}.
     */
    private static final BlockStateProvider[] SURFACE_HERB_SETS = {
            weightedHerbs(RusticFarmBlocks.BLOOD_ORCHID, RusticFarmBlocks.HORSETAIL,
                    RusticFarmBlocks.MARSH_MALLOW, RusticFarmBlocks.MOONCAP_MUSHROOM),
            weightedHerbs(RusticFarmBlocks.ALOE_VERA),
            weightedHerbs(RusticFarmBlocks.ALOE_VERA),
            weightedHerbs(RusticFarmBlocks.ALOE_VERA),
            weightedHerbs(RusticFarmBlocks.WIND_THISTLE, RusticFarmBlocks.CLOUDSBLUFF),
            weightedHerbs(RusticFarmBlocks.CHAMOMILE, RusticFarmBlocks.HORSETAIL, RusticFarmBlocks.MARSH_MALLOW),
            weightedHerbs(RusticFarmBlocks.CHAMOMILE, RusticFarmBlocks.COHOSH, RusticFarmBlocks.GINSENG,
                    RusticFarmBlocks.HORSETAIL, RusticFarmBlocks.VANTA_LILY),
            weightedHerbs(RusticFarmBlocks.CHAMOMILE, RusticFarmBlocks.GINSENG, RusticFarmBlocks.HORSETAIL,
                    RusticFarmBlocks.WIND_THISTLE, RusticFarmBlocks.VANTA_LILY),
    };

    /** {@code Herbs.getRandomHerbForBiome}'s uniform pick, expressed as an equal-weight state list. */
    @SuppressWarnings("unchecked")
    private static BlockStateProvider weightedHerbs(DeferredBlock<HerbBlock>... herbs) {
        SimpleWeightedRandomList.Builder<BlockState> list = SimpleWeightedRandomList.builder();
        for (DeferredBlock<HerbBlock> herb : herbs) {
            // 1.12.2 planted at herb.getDefaultState().withProperty(BlockHerbBase.AGE, 3) - fully ripe, not a seedling
            list.add(herb.get().getStateForAge(herb.get().getMaxAge()));
        }
        return new WeightedStateProvider(list);
    }

    /** {@code Herbs.getRandomCaveHerb}: core root and mooncap. */
    private static BlockStateProvider caveHerbs() {
        return weightedHerbs(RusticFarmBlocks.CORE_ROOT, RusticFarmBlocks.MOONCAP_MUSHROOM);
    }

    /** {@code Herbs.getRandomNetherHerb}: the nether was always a single species, deathstalk. */
    private static BlockStateProvider netherHerbs() {
        return weightedHerbs(RusticFarmBlocks.DEATHSTALK_MUSHROOM);
    }

    /**
     * The wildberry bush at every age. {@code WorldGenWildberries} wrote
     * {@code WILDBERRY_BUSH.withAge(rand.nextInt(6))}, so an equal-weight list of the six {@code AGE_5} states is
     * the same uniform draw rather than the single age-0 state the block's default would give.
     */
    private static BlockStateProvider wildberryBush() {
        SimpleWeightedRandomList.Builder<BlockState> list = SimpleWeightedRandomList.builder();
        BerryBushBlock bush = RusticFarmBlocks.WILDBERRY_BUSH.get();
        for (int age = 0; age <= bush.getMaxAge(); age++) {
            list.add(bush.getStateForAge(age));
        }
        return new WeightedStateProvider(list);
    }

    // ---- Key factories ----

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String path) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, path));
    }

    private static ResourceKey<PlacedFeature> placedKey(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, path + "_placed"));
    }

    private static ResourceKey<BiomeModifier> biomeModifierKey(String path) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, path));
    }

    /**
     * The gated wrapper keys are the bare feature's path plus {@code _gated} rather than an inline literal so the
     * eight surface-herb stacks cannot drift out of step with their bare features; the S8.2 ore and tree keys are
     * written out because a datapack may already reference those names.
     */
    private static ResourceKey<ConfiguredFeature<?, ?>> gatedKey(String path) {
        return configuredKey(path + "_gated");
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<ConfiguredFeature<?, ?>>[] configuredKeys(String... paths) {
        return java.util.Arrays.stream(paths)
                .map(RusticWorldGenProvider::configuredKey)
                .toArray(ResourceKey[]::new);
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<ConfiguredFeature<?, ?>>[] gatedKeys(String... paths) {
        return java.util.Arrays.stream(paths)
                .map(RusticWorldGenProvider::gatedKey)
                .toArray(ResourceKey[]::new);
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<PlacedFeature>[] placedKeys(String... paths) {
        return java.util.Arrays.stream(paths)
                .map(RusticWorldGenProvider::placedKey)
                .toArray(ResourceKey[]::new);
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<BiomeModifier>[] biomeModifierKeys(String... paths) {
        return java.util.Arrays.stream(paths)
                .map(RusticWorldGenProvider::biomeModifierKey)
                .toArray(ResourceKey[]::new);
    }

    @SafeVarargs
    private static TagKey<Biome>[] tags(TagKey<Biome>... tags) {
        return tags;
    }
}
