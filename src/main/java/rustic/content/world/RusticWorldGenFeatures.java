package rustic.content.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

public final class RusticWorldGenFeatures {

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Rustic.MODID);

    public static final DeferredHolder<Feature<?>, RusticConfigGatedFeature> CONFIG_GATED =
            FEATURES.register("config_gated", () -> new RusticConfigGatedFeature(RusticConfigGatedFeature.Configuration.CODEC));

    public static final DeferredHolder<Feature<?>, RusticBeehiveFeature> BEEHIVE =
            FEATURES.register("beehive", () -> new RusticBeehiveFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }

    private RusticWorldGenFeatures() {
    }
}
