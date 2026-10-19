package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockTable extends Block implements RusticFaceShapes.Declarable {

    public static final BooleanProperty NW = BooleanProperty.create("nw");
    public static final BooleanProperty NE = BooleanProperty.create("ne");
    public static final BooleanProperty SE = BooleanProperty.create("se");
    public static final BooleanProperty SW = BooleanProperty.create("sw");

    private static final VoxelShape SHAPE = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);

    public BlockTable(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NW, true).setValue(NE, true).setValue(SE, true).setValue(SW, true));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.corners(context.getLevel(), context.getClickedPos());
    }

    private BlockState corners(LevelReader level, BlockPos pos) {
        boolean blockNorth = isTable(level.getBlockState(pos.north()));
        boolean blockSouth = isTable(level.getBlockState(pos.south()));
        boolean blockEast = isTable(level.getBlockState(pos.east()));
        boolean blockWest = isTable(level.getBlockState(pos.west()));

        return this.defaultBlockState()
                .setValue(NW, !blockNorth && !blockWest)
                .setValue(NE, !blockNorth && !blockEast)
                .setValue(SE, !blockSouth && !blockEast)
                .setValue(SW, !blockSouth && !blockWest);
    }

    private boolean isTable(BlockState state) {
        return state.getBlock() instanceof BlockTable;
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

        return direction.getAxis() == Direction.Axis.Y
                ? state
                : this.corners(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {

        if (!oldState.is(this)) {
            BlockState corners = this.corners(level, pos);
            if (!corners.equals(state)) {
                level.setBlock(pos, corners, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RusticFaceShapes.FaceShape rusticFaceShape(BlockState state, Direction face) {
        return face == Direction.UP
                ? RusticFaceShapes.FaceShape.SOLID
                : RusticFaceShapes.FaceShape.UNDEFINED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NW, NE, SE, SW);
    }
}
