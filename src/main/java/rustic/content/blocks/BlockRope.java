package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockRope extends BlockRopeBase implements RusticFaceShapes.Declarable {

    public BlockRope(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSideSupported(LevelReader level, BlockPos pos, BlockState state, Direction facing) {
        if (facing == Direction.DOWN) {
            return false;
        }
        return super.isSideSupported(level, pos, state, facing)
                || attachesTo(level.getBlockState(pos.relative(facing)));
    }

    @Override
    protected boolean canPlaceOnSide(LevelReader level, BlockPos pos, Direction side) {
        if (side == Direction.UP) {

            return canPlaceOnSide(level, pos, Direction.DOWN);
        }
        return super.canPlaceOnSide(level, pos, side)
                || attachesTo(level.getBlockState(pos.relative(side.getOpposite())));
    }

    private static boolean attachesTo(BlockState anchor) {
        Block block = anchor.getBlock();
        return block instanceof BlockLattice
                || block == RusticFarmBlocks.STAKE_TIED.get()
                || (block instanceof GrapeLeavesBlock && anchor.getValue(AXIS) != Direction.Axis.Y);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {

        if (!level.isClientSide && entity instanceof AbstractArrow arrow && isArrowInAABB(level, pos, state, arrow)) {
            RusticDrops.removeAndDrop(level, pos, state);
        }
    }

    private boolean isArrowInAABB(BlockGetter level, BlockPos pos, BlockState state, AbstractArrow arrow) {
        Direction.Axis axis = state.getValue(AXIS);
        double xExp = axis == Direction.Axis.X ? 0.0 : 0.125;
        double yExp = axis == Direction.Axis.Y ? 0.0 : 0.125;
        double zExp = axis == Direction.Axis.Z ? 0.0 : 0.125;

        VoxelShape shape = getShape(state, level, pos, CollisionContext.empty());
        AABB aabb = shape.bounds().inflate(xExp, yExp, zExp).move(pos);
        return aabb.intersects(arrow.getBoundingBox());
    }

    @Override
    public RusticFaceShapes.FaceShape rusticFaceShape(BlockState state, Direction face) {
        return state.getValue(AXIS) == face.getAxis()
                ? RusticFaceShapes.FaceShape.CENTER_SMALL
                : RusticFaceShapes.FaceShape.UNDEFINED;
    }
}
