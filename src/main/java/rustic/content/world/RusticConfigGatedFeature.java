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

public class RusticConfigGatedFeature extends Feature<RusticConfigGatedFeature.Configuration> {

    public RusticConfigGatedFeature(Codec<Configuration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Configuration> context) {
        Configuration config = context.config();
        RusticGenKnob knob = config.knob();
        WorldGenLevel level = context.level();

        if (!knob.dimensionAllows(level.getLevel().dimension())) {
            return false;
        }

        BlockPos origin = context.origin();
        if (!knob.biomeAllows(level, origin)) {
            return false;
        }

        RandomSource random = context.random();
        boolean natural = knob.naturalSpread();
        if (knob.rollsChance() && !natural && random.nextFloat() >= knob.chance()) {
            return false;
        }

        ConfiguredFeature<?, ?> inner = config.inner().value();
        boolean placed = false;
        for (int i = 0; i < knob.attempts(); i++) {

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

    public record Configuration(Holder<ConfiguredFeature<?, ?>> inner, RusticGenKnob knob)
            implements FeatureConfiguration {

        private static final Codec<RusticGenKnob> KNOB_CODEC =
                Codec.STRING.xmap(RusticGenKnob::valueOf, RusticGenKnob::name);

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ConfiguredFeature.CODEC.fieldOf("inner").forGetter(Configuration::inner),
                KNOB_CODEC.fieldOf("knob").forGetter(Configuration::knob)
        ).apply(instance, Configuration::new));

        @Override
        public Stream<ConfiguredFeature<?, ?>> getFeatures() {

            return inner.value().getFeatures();
        }
    }
}
