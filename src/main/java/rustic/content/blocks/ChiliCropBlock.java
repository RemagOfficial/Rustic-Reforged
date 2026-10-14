package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import rustic.content.items.RusticItems;

/**
 * 1.12.2's {@code ModBlocks.CHILI_CROP}: the {@code BlockStakeCrop("chili_crop")} whose crop is
 * {@code ModItems.CHILI_PEPPER} but which rolled a {@code ModItems.GHOST_PEPPER} one time in forty-two
 * ({@code rand.nextInt(42) == 0}), whose seed is {@code ModItems.CHILI_PEPPER_SEEDS} and which climbs only
 * two stakes tall. The rare variant and the shorter column are the whole difference from the tomato, and
 * {@link StakeCropBlock} carries them as constructor values.
 */
public class ChiliCropBlock extends StakeCropBlock {

    public static final MapCodec<ChiliCropBlock> CODEC = simpleCodec(ChiliCropBlock::new);

    public ChiliCropBlock(BlockBehaviour.Properties properties) {
        super(properties,
                () -> RusticFarmBlocks.CROP_STAKE.get(),
                () -> RusticItems.CHILI_PEPPER_SEEDS.get(),
                () -> RusticItems.CHILI_PEPPER.get(),
                () -> RusticItems.GHOST_PEPPER.get(),
                42,
                2);
    }

    @Override
    public MapCodec<? extends StakeCropBlock> codec() {
        return CODEC;
    }
}
