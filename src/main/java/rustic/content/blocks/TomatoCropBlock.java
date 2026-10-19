package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import rustic.content.items.RusticItems;

public class TomatoCropBlock extends StakeCropBlock {

    public static final MapCodec<TomatoCropBlock> CODEC = simpleCodec(TomatoCropBlock::new);

    public TomatoCropBlock(BlockBehaviour.Properties properties) {
        super(properties,
                () -> RusticFarmBlocks.CROP_STAKE.get(),
                () -> RusticItems.TOMATO_SEEDS.get(),
                () -> RusticItems.TOMATO.get(),
                null,
                0,
                3);
    }

    @Override
    public MapCodec<? extends StakeCropBlock> codec() {
        return CODEC;
    }
}
