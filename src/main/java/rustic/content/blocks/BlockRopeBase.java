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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockRopeBase extends Block {

    public static final Property<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty DANGLE = BooleanProperty.create("dangle");

    protected static final VoxelShape Y_SHAPE = Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
    protected static final VoxelShape X_SHAPE = Block.box(0.0, 7.0, 7.0, 16.0, 9.0, 9.0);
    protected static final VoxelShape Z_SHAPE = Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 16.0);
    protected static final VoxelShape X_DANGLE_SHAPE = Block.box(0.0, 0.0, 7.0, 16.0, 9.0, 9.0);
    protected static final VoxelShape Z_DANGLE_SHAPE = Block.box(7.0, 0.0, 0.0, 9.0, 9.0, 16.0);

    protected static final int MAX_HANG_LENGTH = 64;

    protected BlockRopeBase(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(DANGLE, false));
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        Direction side = hitResult.getDirection();

        if (!canPlaceOnSide(level, pos.relative(side), side) && stack.is(this.asItem())) {
            if (!isBlockSupported(level, pos, state)) {
                RusticDrops.removeAndDrop(level, pos, state);
                return ItemInteractionResult.CONSUME;
            }

            int yOffset = 1;
            while (yOffset < MAX_HANG_LENGTH && level.getBlockState(pos.below(yOffset)).is(this)) {
                if (level.getBlockState(pos.below(yOffset)).getValue(AXIS) != Direction.Axis.Y) {

                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                yOffset++;
            }

            BlockPos target = pos.below(yOffset);

            if (level.getBlockState(target).canBeReplaced()) {
                level.setBlock(target, state.setValue(AXIS, Direction.Axis.Y).setValue(DANGLE, false), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                RusticDrops.playPlaceSound(level, target, state);
                return ItemInteractionResult.CONSUME;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return switch (state.getValue(AXIS)) {
            case Y -> Y_SHAPE;
            case X -> state.getValue(DANGLE) ? X_DANGLE_SHAPE : X_SHAPE;
            case Z -> state.getValue(DANGLE) ? Z_DANGLE_SHAPE : Z_SHAPE;
        };
    }

    protected boolean canPlaceOnSide(LevelReader level, BlockPos pos, Direction side) {
        if (side == Direction.UP) {

            return canPlaceOnSide(level, pos, Direction.DOWN);
        }

        BlockPos anchorPos = pos.relative(side.getOpposite());
        BlockState anchor = level.getBlockState(anchorPos);
        boolean isThis = anchor.is(this) && anchor.getValue(AXIS) == side.getAxis();

        return isThis || RusticFaceShapes.isSolidFace(anchor, level, anchorPos, side);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction.Axis axis = state.getValue(AXIS);
        if (axis == Direction.Axis.Y) {
            return canPlaceOnSide(level, pos, Direction.UP);
        }
        Direction positive = firstAlong(axis);
        return canPlaceOnSide(level, pos, positive) || canPlaceOnSide(level, pos, positive.getOpposite());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbourState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos
    ) {
        if (state.getValue(AXIS) == direction.getAxis() && !isSideSupported(level, pos, state, direction)) {
            if (direction == Direction.UP || !isBlockSupported(level, pos, state)) {

                RusticDrops.dropAsItem(level, pos, state);
                return Blocks.AIR.defaultBlockState();
            }
        }
        return state.setValue(DANGLE, shouldDangle(level, pos, state));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {

        boolean dangle = shouldDangle(level, pos, state);
        if (state.getValue(DANGLE) != dangle) {
            level.setBlock(pos, state.setValue(DANGLE, dangle), Block.UPDATE_ALL);
        }
    }

    protected boolean shouldDangle(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getValue(AXIS) == Direction.Axis.Y) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof BlockRopeBase ropeBase
                && ropeBase.acceptsDangleFrom(below)
                && below.getValue(AXIS) == Direction.Axis.Y;
    }

    protected boolean acceptsDangleFrom(BlockState self) {
        return true;
    }

    public boolean isSideSupported(LevelReader level, BlockPos pos, BlockState state, Direction facing) {
        if (facing == Direction.DOWN) {
            return false;
        }

        BlockPos anchorPos = pos.relative(facing);
        BlockState anchor = level.getBlockState(anchorPos);
        boolean isSame = anchor.is(state.getBlock())
                && ((state.getValue(AXIS) == Direction.Axis.Y && facing.getAxis() == Direction.Axis.Y)
                || anchor.getValue(AXIS) == state.getValue(AXIS));

        return isSame || RusticFaceShapes.isSolidFace(anchor, level, anchorPos, facing.getOpposite());
    }

    public boolean isBlockSupported(LevelReader level, BlockPos pos, BlockState state) {
        return switch (state.getValue(AXIS)) {
            case X -> isSideSupported(level, pos, state, Direction.WEST) && isSideSupported(level, pos, state, Direction.EAST);
            case Y -> isSideSupported(level, pos, state, Direction.UP);
            case Z -> isSideSupported(level, pos, state, Direction.NORTH) && isSideSupported(level, pos, state, Direction.SOUTH);
        };
    }

    private static Direction firstAlong(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.SOUTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        BlockState state = this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
        return state.setValue(DANGLE, shouldDangle(context.getLevel(), context.getClickedPos(), state));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return switch (rot) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                default -> state;
            };
            default -> state;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, DANGLE);
    }
}
