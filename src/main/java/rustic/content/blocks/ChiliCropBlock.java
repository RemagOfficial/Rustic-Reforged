package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import rustic.content.items.RusticItems;

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
