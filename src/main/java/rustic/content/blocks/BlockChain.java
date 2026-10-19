package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockChain extends BlockRopeBase implements RusticFaceShapes.Declarable {

    public BlockChain(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSideSupported(LevelReader level, BlockPos pos, BlockState state, Direction facing) {
        if (facing == Direction.DOWN) {

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

    @Override
    protected boolean canPlaceOnSide(LevelReader level, BlockPos pos, Direction side) {
        if (side == Direction.UP) {

            return canPlaceOnSide(level, pos, Direction.DOWN);
        }

        BlockPos anchorPos = pos.relative(side.getOpposite());
        BlockState anchor = level.getBlockState(anchorPos);
        if (anchor.is(this) && anchor.getValue(AXIS) == side.getAxis()) {
            return true;
        }

        return RusticFaceShapes.canAttach(anchor, level, anchorPos, side);
    }

    @Override
    public RusticFaceShapes.FaceShape rusticFaceShape(BlockState state, Direction face) {
        return state.getValue(AXIS) == face.getAxis()
                ? RusticFaceShapes.FaceShape.CENTER_SMALL
                : RusticFaceShapes.FaceShape.UNDEFINED;
    }
}
