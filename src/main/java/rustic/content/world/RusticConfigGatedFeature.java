package rustic.content.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.stream.Stream;

/**
 * The single reusable config-gate that every Phase 8 generator shares. It is the code half of the hybrid the plan
 * chose: the {@link ConfiguredFeature} it wraps describes <em>what</em> to place (a slate vein, a tree shape, one
 * herb cluster) purely as data, while this feature reproduces 1.12.2's {@code WorldGeneratorRustic} decision-making -
 * the dimension whitelist, the per-chunk chance roll, the {@code MAX_*_ATTEMPTS} scatter loop and the biome test -
 * live from {@link RusticConfig} through its {@link RusticGenKnob}.
 *
 * <p><b>Why not {@code NoneFeatureConfiguration}.</b> The plan sketched this as {@code Feature<NoneFeatureConfiguration>},
 * but that configuration is a {@code Codec.unit} singleton and so cannot carry the reference to the wrapped feature.
 * The {@link Configuration} record below is the minimal correction: it holds the inner {@link Holder} plus the named
 * {@link RusticGenKnob}, which is all the gate needs to read.
 *
 * <p>The {@link #place(FeaturePlaceContext)} body runs the legacy loop in the same order the dispatcher did: bail out
 * if the dimension is not whitelisted, bail out if the chunk-origin biome is not allowed, roll the chance, then try
 * {@link RusticGenKnob#attempts()} scattered positions and place the inner feature at each.
 */
public class RusticConfigGatedFeature extends Feature<RusticConfigGatedFeature.Configuration> {

    public RusticConfigGatedFeature(Codec<Configuration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Configuration> context) {
        Configuration config = context.config();
        RusticGenKnob knob = config.knob();
        WorldGenLevel level = context.level();

        // (1) which dimension is generating, and whether the slate toggles route it here at all
        if (!knob.dimensionAllows(level.getLevel().dimension())) {
            return false;
        }
        // (2) the legacy BiomeDictionary test, evaluated once at the chunk origin like the 1.12.2 generators did
        BlockPos origin = context.origin();
        if (!knob.biomeAllows(level, origin)) {
            return false;
        }
        // (3) the per-chunk chance roll (skipped by the ore loop, which has no chance gate). The optional
        // natural-spread toggle (trees only, off by default) moves that single roll into the loop below so the
        // legacy all-or-nothing burst becomes an even spread; when it is off the faithful path is unchanged.
        RandomSource random = context.random();
        boolean natural = knob.naturalSpread();
        if (knob.rollsChance() && !natural && random.nextFloat() >= knob.chance()) {
            return false;
        }

        // (4) scatter MAX_*_ATTEMPTS times and place the wrapped data feature at each surviving candidate
        ConfiguredFeature<?, ?> inner = config.inner().value();
        boolean placed = false;
        for (int i = 0; i < knob.attempts(); i++) {
            // natural mode pays the chance once per attempt, so most attempts skip and the few that pass land in
            // different chunks; faithful mode already paid the single roll above and runs every attempt as a burst.
            if (natural && random.nextFloat() >= knob.chance()) {
                continue;
            }
            BlockPos pos = knob.scatter().apply(random, level, origin);
            if (pos != null && inner.place(level, context.chunkGenerator(), random, pos)) {
                placed = true;
            }
        }
        return placed;
    }

    /**
     * The wrapped data feature plus the code knob that gates it. {@code inner} is a configured-feature reference so a
     * datapack can retarget the block set without touching this class; {@code knob} names one {@link RusticGenKnob}
     * constant so the shipped JSON says which generator's live config/scatter/biome rules apply.
     */
    public record Configuration(Holder<ConfiguredFeature<?, ?>> inner, RusticGenKnob knob)
            implements FeatureConfiguration {

        /**
         * The knob is stored by its {@link Enum#name()} (so the JSON reads {@code "OLIVE"}, {@code "SLATE_NETHER"},
         * ...): a plain string codec avoids making {@link RusticGenKnob} implement {@code StringRepresentable} just for
         * the codec and keeps the shipped data readable and stable against reordering of the constants.
         */
        private static final Codec<RusticGenKnob> KNOB_CODEC =
                Codec.STRING.xmap(RusticGenKnob::valueOf, RusticGenKnob::name);

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ConfiguredFeature.CODEC.fieldOf("inner").forGetter(Configuration::inner),
                KNOB_CODEC.fieldOf("knob").forGetter(Configuration::knob)
        ).apply(instance, Configuration::new));

        @Override
        public Stream<ConfiguredFeature<?, ?>> getFeatures() {
            // expose the wrapped feature so the loader counts it as a dependency of this configured feature
            return inner.value().getFeatures();
        }
    }
}
