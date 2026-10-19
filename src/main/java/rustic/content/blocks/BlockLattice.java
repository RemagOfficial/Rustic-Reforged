package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class BlockLattice extends Block implements RusticFaceShapes.Declarable {

    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty LEAVES = BooleanProperty.create("leaves");

    private static final Map<Direction, BooleanProperty> CONNECTION_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;

    private static final VoxelShape BASE_SHAPE = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    private static final VoxelShape DOWN_SHAPE = Block.box(7.0, 0.0, 7.0, 9.0, 7.0, 9.0);
    private static final VoxelShape UP_SHAPE = Block.box(7.0, 9.0, 7.0, 9.0, 16.0, 9.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 7.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(7.0, 7.0, 9.0, 9.0, 9.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0, 7.0, 7.0, 7.0, 9.0, 9.0);
    private static final VoxelShape EAST_SHAPE = Block.box(9.0, 7.0, 7.0, 16.0, 9.0, 9.0);

    public BlockLattice(BlockBehaviour.Properties properties) {
        super(properties);

        BlockState base = this.stateDefinition.any().setValue(LEAVES, false);
        for (BooleanProperty connection : CONNECTION_BY_DIRECTION.values()) {
            base = base.setValue(connection, false);
        }
        this.registerDefaultState(base);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double x1 = 0.375;
        double y1 = 0.375;
        double z1 = 0.375;
        double x2 = 0.625;
        double y2 = 0.625;
        double z2 = 0.625;

        if (state.getValue(DOWN)) {
            y1 = 0.0;
        }
        if (state.getValue(UP)) {
            y2 = 1.0;
        }
        if (state.getValue(NORTH)) {
            z1 = 0.0;
        }
        if (state.getValue(SOUTH)) {
            z2 = 1.0;
        }
        if (state.getValue(WEST)) {
            x1 = 0.0;
        }
        if (state.getValue(EAST)) {
            x2 = 1.0;
        }
        return Shapes.box(x1, y1, z1, x2, y2, z2);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = BASE_SHAPE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(CONNECTION_BY_DIRECTION.get(direction))) {
                shape = Shapes.or(shape, armShape(direction));
            }
        }
        return shape;
    }

    private static VoxelShape armShape(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }

    @Override
    protected BlockState updateShape(
            BlockState state, Direction direction, BlockState neighbourState,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos
    ) {
        return withConnections(state, level, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        BlockState connections = withConnections(state, level, pos);
        if (connections != state) {
            level.setBlock(pos, connections, Block.UPDATE_ALL);
        }
    }

    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            updated = updated.setValue(CONNECTION_BY_DIRECTION.get(direction), canConnectTo(level, pos, direction));
        }
        return updated;
    }

    private boolean canConnectTo(BlockGetter level, BlockPos pos, Direction facing) {
        BlockPos deltaPos = pos.relative(facing);
        BlockState neighbour = level.getBlockState(deltaPos);
        Block block = neighbour.getBlock();

        if (block instanceof BlockLattice) {
            return true;
        }

        if (block instanceof BlockRopeBase) {
            return neighbour.getValue(BlockRopeBase.AXIS) == facing.getAxis();
        }

        RusticFaceShapes.FaceShape faceShape =
                RusticFaceShapes.of(neighbour, level, deltaPos, facing.getOpposite());
        return faceShape.canSupportAttached() && faceShape != RusticFaceShapes.FaceShape.BOWL;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult
    ) {

        if (Block.byItem(stack.getItem()) instanceof LeavesBlock) {
            level.setBlock(pos, state.setValue(LEAVES, true), Block.UPDATE_ALL);
            stack.shrink(1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public RusticFaceShapes.FaceShape rusticFaceShape(BlockState state, Direction face) {
        return RusticFaceShapes.FaceShape.CENTER_SMALL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, LEAVES);
    }
}
