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

public class RusticWorldGenProvider extends DatapackBuiltinEntriesProvider {

    private static final int SLATE_VEIN_SIZE = 20;

    private static final ResourceKey<ConfiguredFeature<?, ?>> SLATE_ORE_OVERWORLD =
            configuredKey("slate_ore_overworld");
    private static final ResourceKey<ConfiguredFeature<?, ?>> SLATE_ORE_NETHER =
            configuredKey("slate_ore_nether");

    private static final ResourceKey<ConfiguredFeature<?, ?>> SLATE_ORE_OVERWORLD_GATED =
            configuredKey("slate_ore_overworld_gated");
    private static final ResourceKey<ConfiguredFeature<?, ?>> SLATE_ORE_NETHER_GATED =
            configuredKey("slate_ore_nether_gated");
    private static final ResourceKey<ConfiguredFeature<?, ?>> OLIVE_TREE_GATED =
            configuredKey("olive_tree_gated");
    private static final ResourceKey<ConfiguredFeature<?, ?>> IRONWOOD_TREE_GATED =
            configuredKey("ironwood_tree_gated");

    private static final ResourceKey<PlacedFeature> SLATE_ORE_OVERWORLD_PLACED =
            placedKey("slate_ore_overworld");
    private static final ResourceKey<PlacedFeature> SLATE_ORE_NETHER_PLACED =
            placedKey("slate_ore_nether");
    private static final ResourceKey<PlacedFeature> OLIVE_TREE_PLACED =
            placedKey("olive_tree");
    private static final ResourceKey<PlacedFeature> IRONWOOD_TREE_PLACED =
            placedKey("ironwood_tree");

    private static final ResourceKey<BiomeModifier> SLATE_ORE_OVERWORLD_BM =
            biomeModifierKey("slate_ore_overworld");
    private static final ResourceKey<BiomeModifier> SLATE_ORE_NETHER_BM =
            biomeModifierKey("slate_ore_nether");
    private static final ResourceKey<BiomeModifier> OLIVE_TREE_BM =
            biomeModifierKey("olive_tree");
    private static final ResourceKey<BiomeModifier> IRONWOOD_TREE_BM =
            biomeModifierKey("ironwood_tree");

    static final String[] SURFACE_HERB_PATHS = {
            "herbs_jungle", "herbs_sandy", "herbs_savanna", "herbs_mesa", "herbs_mountain",
            "herbs_swamp", "herbs_forest", "herbs_plains"};

    static final ResourceKey<ConfiguredFeature<?, ?>>[] SURFACE_HERB_BARE = List.of(
            configuredKey("herbs_jungle"), configuredKey("herbs_sandy"), configuredKey("herbs_savanna"),
            configuredKey("herbs_mesa"), configuredKey("herbs_mountain"), configuredKey("herbs_swamp"),
            configuredKey("herbs_forest"), configuredKey("herbs_plains")).toArray(ResourceKey[]::new);
    static final ResourceKey<ConfiguredFeature<?, ?>>[] SURFACE_HERB_GATED = gatedKeys(SURFACE_HERB_PATHS);
    static final ResourceKey<PlacedFeature>[] SURFACE_HERB_PLACED = placedKeys(SURFACE_HERB_PATHS);
    static final ResourceKey<BiomeModifier>[] SURFACE_HERB_BM = biomeModifierKeys(SURFACE_HERB_PATHS);

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

    private static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {

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

        Holder.Reference<ConfiguredFeature<?, ?>> olive = context.register(RusticWorldGen.OLIVE_TREE,
                new ConfiguredFeature<>(Feature.TREE, oliveTree()));
        context.register(OLIVE_TREE_GATED, gated(olive, RusticGenKnob.OLIVE));

        Holder.Reference<ConfiguredFeature<?, ?>> ironwood = context.register(RusticWorldGen.IRONWOOD_TREE,
                new ConfiguredFeature<>(Feature.TREE, ironwoodTree()));
        context.register(IRONWOOD_TREE_GATED, gated(ironwood, RusticGenKnob.IRONWOOD));

        context.register(RusticWorldGen.APPLE_TREE,
                new ConfiguredFeature<>(Feature.TREE, appleTree()));

        for (int i = 0; i < SURFACE_HERB_BARE.length; i++) {
            Holder.Reference<ConfiguredFeature<?, ?>> bare = context.register(SURFACE_HERB_BARE[i],
                    new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                            new SimpleBlockConfiguration(SURFACE_HERB_SETS[i])));
            context.register(SURFACE_HERB_GATED[i], gated(bare, RusticGenKnob.SURFACE_HERB));
        }

        Holder.Reference<ConfiguredFeature<?, ?>> cave = context.register(CAVE_HERBS,
                new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(caveHerbs())));
        context.register(CAVE_HERBS_GATED, gated(cave, RusticGenKnob.CAVE_HERB));

        Holder.Reference<ConfiguredFeature<?, ?>> nether = context.register(NETHER_HERBS,
                new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(netherHerbs())));
        context.register(NETHER_HERBS_GATED, gated(nether, RusticGenKnob.NETHER_HERB));

        Holder.Reference<ConfiguredFeature<?, ?>> berries = context.register(WILDBERRIES,
                new ConfiguredFeature<>(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(wildberryBush())));
        context.register(WILDBERRIES_GATED, gated(berries, RusticGenKnob.WILDBERRY));

        context.register(BEEHIVE, new ConfiguredFeature<>(RusticWorldGenFeatures.BEEHIVE.get(),
                NoneFeatureConfiguration.INSTANCE));
    }

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

        context.register(BEEHIVE_PLACED,
                new PlacedFeature(configured.getOrThrow(BEEHIVE), List.of()));
    }

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

        for (int i = 0; i < SURFACE_HERB_PLACED.length; i++) {
            context.register(SURFACE_HERB_BM[i], addFeatures(
                    biomes.getOrThrow(SURFACE_HERB_BIOME_TAGS[i]),
                    placed.getOrThrow(SURFACE_HERB_PLACED[i]),
                    Decoration.VEGETAL_DECORATION));
        }

        context.register(CAVE_HERBS_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                placed.getOrThrow(CAVE_HERBS_PLACED),
                Decoration.UNDERGROUND_DECORATION));
        context.register(NETHER_HERBS_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_NETHER),
                placed.getOrThrow(NETHER_HERBS_PLACED),
                Decoration.VEGETAL_DECORATION));

        context.register(WILDBERRIES_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                placed.getOrThrow(WILDBERRIES_PLACED),
                Decoration.VEGETAL_DECORATION));
        context.register(BEEHIVE_BM, addFeatures(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                placed.getOrThrow(BEEHIVE_PLACED),
                Decoration.VEGETAL_DECORATION));
    }

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

    private static TreeConfiguration appleTree() {
        return oakStyleTree(Blocks.OAK_LOG, RusticFarmBlocks.APPLE_LEAVES.get(), 5, 2);
    }

    private static TreeConfiguration oakStyleTree(Block log, Block leaves, int base, int randA) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(log),
                new StraightTrunkPlacer(base, randA, 0),
                BlockStateProvider.simple(leaves),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 2),
                new TwoLayersFeatureSize(1, 0, 1))
                .build();
    }

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

    @SuppressWarnings("unchecked")
    private static BlockStateProvider weightedHerbs(DeferredBlock<HerbBlock>... herbs) {
        SimpleWeightedRandomList.Builder<BlockState> list = SimpleWeightedRandomList.builder();
        for (DeferredBlock<HerbBlock> herb : herbs) {

            list.add(herb.get().getStateForAge(herb.get().getMaxAge()));
        }
        return new WeightedStateProvider(list);
    }

    private static BlockStateProvider caveHerbs() {
        return weightedHerbs(RusticFarmBlocks.CORE_ROOT, RusticFarmBlocks.MOONCAP_MUSHROOM);
    }

    private static BlockStateProvider netherHerbs() {
        return weightedHerbs(RusticFarmBlocks.DEATHSTALK_MUSHROOM);
    }

    private static BlockStateProvider wildberryBush() {
        SimpleWeightedRandomList.Builder<BlockState> list = SimpleWeightedRandomList.builder();
        BerryBushBlock bush = RusticFarmBlocks.WILDBERRY_BUSH.get();
        for (int age = 0; age <= bush.getMaxAge(); age++) {
            list.add(bush.getStateForAge(age));
        }
        return new WeightedStateProvider(list);
    }

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
