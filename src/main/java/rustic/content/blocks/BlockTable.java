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

/**
 * 1.12.2's {@code BlockTable}: a four legged table that hides the legs which touch another table, so a
 * row of them reads as one long surface.
 *
 * <p>The four corner flags are the whole of the legacy class, and 1.12.2 kept them out of the stored
 * state: {@code getActualState} recomputed them from the four horizontal neighbours at render time, the
 * block itself stored nothing (its {@code getMetaFromState} always answered 0). 1.21 deleted
 * {@code getActualState}, so the corners become real stored properties - the same move the rope's
 * {@code dangle} made, and for the same reason - which means the block has to keep them up to date
 * itself. {@link #updateShape} runs whenever a neighbour changes and {@link #onPlace} covers the case
 * world generation and structure updates leave them stale, which is exactly how vanilla's own
 * {@code FenceBlock} maintains its four connection booleans.
 *
 * <p>The legacy {@code instanceof BlockTable} test is kept rather than narrowed to this wood: any table
 * hides the legs of any other table, oak next to olive included, because that is what the 1.12.2 code
 * did. The shape is the tabletop alone - the legacy {@code (0, 0.875, 0) → (1, 1, 1)} bounding box -
 * and {@code isOpaqueCube}/{@code isFullCube} returning false is {@code noOcclusion()} on the
 * properties, while {@code isSideSolid == true} for UP only is the {@link RusticFaceShapes.Declarable}
 * answer below: a thin top cannot derive {@code RIGID} from its collision shape on any face, so the
 * declaration is not cosmetic.
 */
public class BlockTable extends Block implements RusticFaceShapes.Declarable {

    public static final BooleanProperty NW = BooleanProperty.create("nw");
    public static final BooleanProperty NE = BooleanProperty.create("ne");
    public static final BooleanProperty SE = BooleanProperty.create("se");
    public static final BooleanProperty SW = BooleanProperty.create("sw");

    /** 1.12.2's {@code AABB}: the tabletop, two sixteenths thick, at the top of the block. */
    private static final VoxelShape SHAPE = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);

    public BlockTable(BlockBehaviour.Properties properties) {
        super(properties);
        // 1.12.2: blockState.getBaseState().withProperty(NW, true).withProperty(NE, true).... A lone
        // table therefore shows all four legs, and only joining neighbours take any of them away.
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NW, true).setValue(NE, true).setValue(SE, true).setValue(SW, true));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.corners(context.getLevel(), context.getClickedPos());
    }

    /**
     * 1.12.2 recomputed this from {@code getActualState} every frame; here it happens once, on the two
     * events that can change the answer.
     */
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

    /** The legacy {@code state.getBlock() instanceof BlockTable}: every wood joins every other. */
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
        // The legacy method had no equivalent - getActualState made staleness impossible. Only the four
        // horizontal neighbours matter, and corners() reads all four anyway.
        return direction.getAxis() == Direction.Axis.Y
                ? state
                : this.corners(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // World generation and structure template copies place several tables at once, each of them
        // seeing air where its neighbour will be, so the corners are recomputed once on arrival - the
        // same correction FenceBlock makes.
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

    /** 1.12.2's {@code getBlockFaceShape}: SOLID on the tabletop, nothing anywhere else. */
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
