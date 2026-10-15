package rustic.content.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.world.RusticWorldGen;

/**
 * The two code features Phase 8 registers so the data-driven world generation can reference them: the reusable
 * {@link RusticConfigGatedFeature} (type {@code rustic:config_gated}) that every ore/tree/herb/berry configured
 * feature wraps, and the {@link RusticBeehiveFeature} (type {@code rustic:beehive}) that hangs a hive under leaves.
 *
 * <p>Registering a {@link Feature} is what gives its {@link Codec} a registry name so a
 * {@code minecraft:configured_feature} JSON can say {@code "type": "rustic:config_gated"} - the
 * {@link RusticWorldGen} keys the sapling growers and biome modifiers share are configured features that point back
 * at the feature types registered here. {@code rustics.content.world} holds the code, {@code rustic.world} the data
 * keys, and this class is the bridge that makes the data keys resolvable at load time.
 */
public final class RusticWorldGenFeatures {

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Rustic.MODID);

    /** The shared config gate; its codec is the {@link RusticConfigGatedFeature.Configuration} record's. */
    public static final DeferredHolder<Feature<?>, RusticConfigGatedFeature> CONFIG_GATED =
            FEATURES.register("config_gated", () -> new RusticConfigGatedFeature(RusticConfigGatedFeature.Configuration.CODEC));

    /** The beehive-under-leaves feature, which carries no configuration of its own. */
    public static final DeferredHolder<Feature<?>, RusticBeehiveFeature> BEEHIVE =
            FEATURES.register("beehive", () -> new RusticBeehiveFeature(NoneFeatureConfiguration.CODEC));

    /** Attaches the feature register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }

    private RusticWorldGenFeatures() {
    }
}
