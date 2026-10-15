package rustic.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import com.mojang.serialization.Codec;
import rustic.content.blocks.BlockBeehive;
import rustic.content.blocks.BlockRotatable;
import rustic.content.blocks.RusticFarmBlocks;

/**
 * 1.12.2's {@code WorldGenBeehive} as a 1.21 feature, kept separate from {@link RusticConfigGatedFeature} because a
 * beehive is not a scatter of a data-declared block set: it is one specific block whose placement is decided by a
 * structural test on the world - the candidate cell must sit directly under a leaf canopy, found by walking straight
 * down through {@link net.minecraft.world.level.block.LeavesBlock} until the leaves end.
 *
 * <p>Everything else is shared with the other generators: the dimension whitelist, the {@code !snowy} chunk-origin
 * test, the {@code BEEHIVE_GEN_CHANCE} roll and the {@code MAX_BEEHIVE_ATTEMPTS} loop all come from the
 * {@link RusticGenKnob#BEEHIVE} knob, and the leaves walk-down is that knob's {@link RusticGenKnob.Scatter#BEEHIVE}
 * scatter, which hands back the exact air cell to hang the hive in (or {@code null} to skip the attempt). This is the
 * only thing left to do here: pick a random horizontal facing and set the block. The legacy hive required leaves
 * above it only at placement time; there is no {@code neighborChanged} drop, matching the plan's decision to register
 * {@link BlockBeehive} as a plain block whose "under leaves" rule lives solely in this feature.
 */
public class RusticBeehiveFeature extends Feature<NoneFeatureConfiguration> {

    /** The four facings 1.12.2's {@code WorldGenBeehive.orientations} array offered, in the same order. */
    private static final Direction[] ORIENTATIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public RusticBeehiveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RusticGenKnob knob = RusticGenKnob.BEEHIVE;
        WorldGenLevel level = context.level();

        // dimension + biome + chance gates, read live from config exactly as RusticConfigGatedFeature does
        if (!knob.dimensionAllows(level.getLevel().dimension())) {
            return false;
        }
        BlockPos origin = context.origin();
        if (!knob.biomeAllows(level, origin)) {
            return false;
        }
        RandomSource random = context.random();
        if (knob.rollsChance() && random.nextFloat() >= knob.chance()) {
            return false;
        }

        BlockBeehive beehive = RusticFarmBlocks.BEEHIVE.get();
        for (int i = 0; i < knob.attempts(); i++) {
            BlockPos pos = knob.scatter().apply(random, level, origin);
            if (pos != null) {
                BlockState state = beehive.defaultBlockState().setValue(BlockRotatable.FACING, ORIENTATIONS[random.nextInt(4)]);
                // the legacy setBlockState(..., orientations[rand(4)]): a facing chosen per hive, hanging under the leaves
                level.setBlock(pos, state, 3);
                return true;
            }
        }
        return false;
    }
}
