package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12.2's {@code BlockChain}: a {@link BlockRopeBase} that is willing to hang from almost anything.
 *
 * <p>The hardness (1.0F), the {@link SoundType#METAL} and the default {@code AXIS = Y} the legacy
 * constructors set are properties or the base default here, and the {@code BlockRenderLayer.CUTOUT}
 * override has moved to the model's {@code render_type} plus {@code noOcclusion()} on the properties,
 * which is what the legacy {@code isOpaqueCube == false} already meant.
 *
 * <p>The two support tests widen the base versions: where rope demanded a solid face, chain accepts any
 * of the attachable face shapes, which is the difference between a rope bolted to a wall and a chain
 * that will hang from a fence.
 */
public class BlockChain extends BlockRopeBase implements RusticFaceShapes.Declarable {

    public BlockChain(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * 1.12.2's {@code isSideSupported}. Everything the legacy chain added to the base rule is either the
     * widened face-shape test or the lattice shortcut - and {@link BlockLattice} declares
     * {@link RusticFaceShapes.FaceShape#CENTER_SMALL} on every face, so
     * {@link RusticFaceShapes#canAttach} answers "yes" for it exactly as the legacy
     * {@code instanceof BlockLattice} did. No marker interface is needed.
     */
    @Override
    public boolean isSideSupported(LevelReader level, BlockPos pos, BlockState state, Direction facing) {
        if (facing == Direction.DOWN) {
            // A surface below a chain can never be used as support (1.12.2 comment)
            return false;
        }

        BlockPos anchorPos = pos.relative(facing);
        BlockState anchor = level.getBlockState(anchorPos);
        if (anchor.is(state.getBlock())
                && ((state.getValue(AXIS) == Direction.Axis.Y && facing.getAxis() == Direction.Axis.Y)
                || anchor.getValue(AXIS) == state.getValue(AXIS))) {
            return true;
        }

        return RusticFaceShapes.canAttach(anchor, level, anchorPos, facing.getOpposite());
    }

    /** 1.12.2's {@code canPlaceBlockOnSide}, with the same widened face test and the same lattice rule. */
    @Override
    protected boolean canPlaceOnSide(LevelReader level, BlockPos pos, Direction side) {
        if (side == Direction.UP) {
            // When attempting to place chain on ground see if there's a block above it to hang from (1.12.2 comment)
            return canPlaceOnSide(level, pos, Direction.DOWN);
        }

        BlockPos anchorPos = pos.relative(side.getOpposite());
        BlockState anchor = level.getBlockState(anchorPos);
        if (anchor.is(this) && anchor.getValue(AXIS) == side.getAxis()) {
            return true;
        }

        return RusticFaceShapes.canAttach(anchor, level, anchorPos, side);
    }

    /**
     * 1.12.2's {@code getBlockFaceShape}: the chain presents a thin centre stub on the two faces along
     * its own axis and nothing on the others, which is what lets another chain, candle or lantern hang
     * from its end but not cling to its side.
     */
    @Override
    public RusticFaceShapes.FaceShape rusticFaceShape(BlockState state, Direction face) {
        return state.getValue(AXIS) == face.getAxis()
                ? RusticFaceShapes.FaceShape.CENTER_SMALL
                : RusticFaceShapes.FaceShape.UNDEFINED;
    }
}
