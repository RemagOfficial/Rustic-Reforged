package rustic.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import rustic.Rustic;

public final class RusticWorldGen {

    public static final ResourceKey<ConfiguredFeature<?, ?>> OLIVE_TREE = configuredKey("olive_tree");

    public static final ResourceKey<ConfiguredFeature<?, ?>> IRONWOOD_TREE = configuredKey("ironwood_tree");

    public static final ResourceKey<ConfiguredFeature<?, ?>> APPLE_TREE = configuredKey("apple_tree");

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String path) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(Rustic.MODID, path));
    }

    private RusticWorldGen() {
    }
}
