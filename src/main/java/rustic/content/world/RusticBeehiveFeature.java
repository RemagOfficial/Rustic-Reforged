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

public class RusticBeehiveFeature extends Feature<NoneFeatureConfiguration> {

    private static final Direction[] ORIENTATIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public RusticBeehiveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RusticGenKnob knob = RusticGenKnob.BEEHIVE;
        WorldGenLevel level = context.level();

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

                level.setBlock(pos, state, 3);
                return true;
            }
        }
        return false;
    }
}
