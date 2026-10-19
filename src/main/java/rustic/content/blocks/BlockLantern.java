package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rustic.content.blocks.RusticFaceShapes.Declarable;
import rustic.content.blocks.RusticFaceShapes.FaceShape;

public class BlockLantern extends Block implements Declarable {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    protected static final VoxelShape DEFAULT_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0);
    protected static final VoxelShape DOWN_SHAPE = Block.box(4.0, 6.0, 4.0, 12.0, 16.0, 12.0);

    private static final double FLAME_HEIGHT = 0.33;
    private static final double HANGING_FLAME_LIFT = 0.375;

    public BlockLantern(BlockBehaviour.Properties properties) {

        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING) == Direction.DOWN ? DOWN_SHAPE : DEFAULT_SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double y = pos.getY() + FLAME_HEIGHT;
        if (state.getValue(FACING) == Direction.DOWN) {
            y += HANGING_FLAME_LIFT;
        }
        level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5, y, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
    }

    @Override
    public FaceShape rusticFaceShape(BlockState state, Direction face) {
        Direction mountFace = state.getValue(FACING).getOpposite();

        if (mountFace == face) {
            return FaceShape.CENTER;
        }
        return face == Direction.DOWN && mountFace != Direction.UP ? FaceShape.CENTER : FaceShape.UNDEFINED;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        return state.setValue(FACING, mirror.getRotation(facing).rotate(facing));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
