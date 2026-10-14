package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import rustic.content.items.RusticItems;

/**
 * 1.12.2's {@code ModBlocks.TOMATO_CROP}: an anonymous {@code BlockStakeCrop("tomato_crop")} whose crop is
 * {@code ModItems.TOMATO}, whose seed is {@code ModItems.TOMATO_SEEDS} and which climbs three stakes tall.
 * Everything about it is {@link StakeCropBlock} unchanged; this class only supplies the four values, with no
 * rare variant.
 */
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
