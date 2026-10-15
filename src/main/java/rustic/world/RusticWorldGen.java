package rustic.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import rustic.Rustic;

/**
 * The identifiers Phase 8 worldgen shares between the code that grows a sapling and the data that generates
 * a tree in the world.
 *
 * <p>1.12.2 grew every Rustic tree in code ({@code WorldGenAllTrees}, {@code WorldGenAppleTree}); 1.21 wants a
 * tree to be a {@code minecraft:tree} {@link ConfiguredFeature} declared as data, and a {@code SaplingBlock}'s
 * {@link net.minecraft.world.level.block.grower.TreeGrower} to name that feature by its {@link ResourceKey}.
 * The keys below are that shared name: {@link rustic.content.blocks.RusticBlocks} and
 * {@link rustic.content.blocks.RusticFarmBlocks} hand them to their saplings' growers, and
 * {@code rustic.data.RusticWorldGenProvider} emits a configured feature at each of them. They must agree, or a
 * mature sapling resolves to no feature and grows to nothing.
 *
 * <p>The code {@code Feature}s themselves are registered by {@code rustic.content.world.RusticWorldGenFeatures}
 * (Phase 8.1); this class carries only the worldgen data keys, which is why it lives in the {@code rustic.world}
 * package rather than the code-feature one.
 */
public final class RusticWorldGen {

    /** The olive tree a {@code sapling_olive} matures into, and the biome features that place it. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> OLIVE_TREE = configuredKey("olive_tree");

    /** The ironwood tree a {@code sapling_ironwood} matures into, and the biome features that place it. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> IRONWOOD_TREE = configuredKey("ironwood_tree");

    /**
     * The apple tree a {@code sapling_apple} matures into. Vanilla {@code OAK_LOG} trunk plus
     * {@code leaves_apple} foliage; unlike olive and ironwood it is sapling-grown only, so no biome feature
     * places it (the legacy {@code WorldGenAllTrees} generated olive and ironwood, never apple).
     */
    public static final ResourceKey<ConfiguredFeature<?, ?>> APPLE_TREE = configuredKey("apple_tree");

    /** A {@code rustic}-namespaced configured-feature key at {@code path}. */
    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String path) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, path));
    }

    private RusticWorldGen() {
    }
}
