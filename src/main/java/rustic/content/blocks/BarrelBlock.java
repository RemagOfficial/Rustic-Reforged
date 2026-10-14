package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rustic.content.blockentity.BarrelBlockEntity;

/**
 * 1.12.2's {@code BlockBarrel} - a wooden storage barrel with a 27-slot {@code TileEntityBarrel}
 * behind it. Everything the legacy class did that was not the tile entity lives on
 * {@link BlockRusticChest}; this class only supplies the block entity.
 *
 * <p>The legacy block was {@code Material.WOOD} at hardness 1.5 with no harvest level (so it always
 * dropped itself) and a {@code CUTOUT} render layer for its {@code BlockBarrelRustic} model. The
 * cutout was only there because the 1.12.2 model had a see-through top face; the port gives it a plain
 * opaque cube, so no render type is set. Its directional open/close animation is vanilla Barrel's,
 * which 1.12.2's barrel never had, so this block stays non-directional.
 */
public class BarrelBlock extends BlockRusticChest {

    // BaseEntityBlock declares codec() abstract in 1.21.1; vanilla's own BarrelBlock supplies one the
    // same way (BarrelBlock.java line 28), and it only matters for structure/datapack block loading.
    public static final MapCodec<BarrelBlock> CODEC = simpleCodec(BarrelBlock::new);

    public BarrelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BarrelBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarrelBlockEntity(pos, state);
    }
}
