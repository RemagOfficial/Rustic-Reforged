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

/**
 * 1.12.2's {@code BlockLattice}: an iron frame whose six arms reach out to whatever is next to it, and
 * which can be planted with leaves by right-clicking it with any leaf block.
 *
 * <p>The legacy class is Forge-specific in the one way that matters. Its six connection flags were
 * <em>unlisted</em> properties, computed on the fly in {@code getExtendedState} for the model and the
 * collision boxes and never stored in the state - a mechanism 1.21 deleted. They are ordinary listed
 * {@link BooleanProperty properties} here, in vanilla's own {@link BlockStateProperties} names, which is
 * what {@link PipeBlock} does for glass panes and iron bars and what the multipart model conditions on.
 * The consequence is that a lattice now has to recompute its arms when a neighbour changes
 * ({@link #updateShape}) and when it arrives without ever being placed ({@link #onPlace}), exactly as the
 * panes do, and the 128 states are all in the blockstate file - the same file the custom baked model used
 * to replace.
 *
 * <p><b>Two documented deviations.</b> First, the legacy face-shape test refused {@code MIDDLE_POLE},
 * {@code MIDDLE_POLE_THIN} and {@code MIDDLE_POLE_THICK} - the answers of fences, panes and bars - and
 * {@link RusticFaceShapes} cannot tell those apart from {@code CENTER_SMALL}, because 1.21 derives face
 * sturdiness from the shape rather than reading a declared value. A lattice therefore reaches into a fence
 * or a pane it would not have touched in 1.12.2; the geometry still looks right, since both have a centre
 * post. Second, {@code Block.isExceptBlockForAttachWithPiston} - the "no glass, no ice, no leaves, no
 * pistons" list - is gone from 1.21 entirely, so those blocks are now accepted wherever their face answers
 * sturdy, which for glass and ice it does.
 *
 * <p><b>Deferred to later phases:</b> the {@code BlockGrapeLeaves} arm (Phase 5) and the foliage tint that
 * colouring the leaf model's {@code tintindex} 1 faces needs, a client colour handler (Phase 9).
 */
public class BlockLattice extends Block implements RusticFaceShapes.Declarable {

    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty LEAVES = BooleanProperty.create("leaves");

    /**
     * The six connection properties in the legacy {@code CONNECTIONS} order, which is
     * {@code Direction.values()}: down, up, north, south, west, east. Vanilla's own map of direction to
     * property is the same six, so the model provider and this class can share it.
     */
    private static final Map<Direction, BooleanProperty> CONNECTION_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;

    // The legacy AxisAlignedBB set, in sixteenths: a 6..10 centre cube, and a 7..9 bar reaching the face.
    private static final VoxelShape BASE_SHAPE = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    private static final VoxelShape DOWN_SHAPE = Block.box(7.0, 0.0, 7.0, 9.0, 7.0, 9.0);
    private static final VoxelShape UP_SHAPE = Block.box(7.0, 9.0, 7.0, 9.0, 16.0, 9.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 7.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(7.0, 7.0, 9.0, 9.0, 9.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0, 7.0, 7.0, 7.0, 9.0, 9.0);
    private static final VoxelShape EAST_SHAPE = Block.box(9.0, 7.0, 7.0, 16.0, 9.0, 9.0);

    public BlockLattice(BlockBehaviour.Properties properties) {
        super(properties);
        // 1.12.2's base state was "any" plus LEAVES false, with the unlisted connections unset.
        BlockState base = this.stateDefinition.any().setValue(LEAVES, false);
        for (BooleanProperty connection : CONNECTION_BY_DIRECTION.values()) {
            base = base.setValue(connection, false);
        }
        this.registerDefaultState(base);
    }

    /**
     * 1.12.2's {@code getBoundingBox}: one box that starts as the centre cube and is stretched to the edge
     * of the block on every axis it is connected along.
     */
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

    /**
     * 1.12.2's {@code addCollisionBoxToList}: the centre cube unioned with one bar per connected
     * direction. Unlike the outline above the bars keep their own thin cross-section, so an entity walks
     * through the gaps between them.
     */
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

    /**
     * 1.12.2's {@code getExtendedState}, which ran the same six questions every time the state was read.
     * Here all six are recomputed on any neighbour change rather than only the changed one, so a state
     * cannot go stale - the cost is six block reads per update instead of one.
     */
    @Override
    protected BlockState updateShape(
            BlockState state, Direction direction, BlockState neighbourState,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos
    ) {
        return withConnections(state, level, pos);
    }

    /** 1.12.2's {@code getBlockState} for a placement: the arms are known the moment the position is. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    /**
     * 1.12.2 had no equivalent, because its arms were never stored: a lattice that arrives from a
     * structure, a fill command or world generation has to be asked to work them out once. The same
     * approach {@link BlockRopeBase#onPlace} takes for its sagging flag.
     */
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

    /**
     * 1.12.2's private {@code canConnectTo(world, pos, facing)}: the question asked from the lattice
     * outwards, about the block in the {@code facing} direction and the face it shows back.
     *
     * <p>The leafy case of the legacy method is the grape leaves, which match on axis; they arrive with the
     * crops in Phase 5 and belong next to the rope case below.
     */
    private boolean canConnectTo(BlockGetter level, BlockPos pos, Direction facing) {
        BlockPos deltaPos = pos.relative(facing);
        BlockState neighbour = level.getBlockState(deltaPos);
        Block block = neighbour.getBlock();

        // Another lattice always takes the arm, whatever the face it shows would say.
        if (block instanceof BlockLattice) {
            return true;
        }
        // A rope, chain or grape leaf only connects when it runs along the same axis as the arm.
        if (block instanceof BlockRopeBase) {
            return neighbour.getValue(BlockRopeBase.AXIS) == facing.getAxis();
        }

        // Then the face-shape rule: anything with a face to hold on to, except a hollow top. The three
        // middle-pole exclusions and the piston exception list are documented at the class note.
        RusticFaceShapes.FaceShape faceShape =
                RusticFaceShapes.of(neighbour, level, deltaPos, facing.getOpposite());
        return faceShape.canSupportAttached() && faceShape != RusticFaceShapes.FaceShape.BOWL;
    }

    /**
     * 1.12.2's {@code onBlockActivated}: right-clicking the frame with any leaf block in hand puts the
     * foliage on it.
     *
     * <p>Reproduced bug-for-bug: the legacy method consumed the item unconditionally, in creative as well
     * as survival, and happily re-consumed one on a lattice that already had leaves. Only the already-empty
     * case differs, because 1.21 hands over the stack rather than the held item.
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        // 1.12.2's Block.getBlockFromItem(heldItem.getItem()) instanceof BlockLeaves pair.
        if (Block.byItem(stack.getItem()) instanceof LeavesBlock) {
            level.setBlock(pos, state.setValue(LEAVES, true), Block.UPDATE_ALL);
            stack.shrink(1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * 1.12.2's {@code getBlockFaceShape}: every face of the lattice answers {@code CENTER_SMALL}, which is
     * what lets a rope, a chain or a candle cling to it from the side rather than only hang off its ends -
     * and what the chain's widened support test leans on in place of the legacy
     * {@code instanceof BlockLattice} clause.
     */
    @Override
    public RusticFaceShapes.FaceShape rusticFaceShape(BlockState state, Direction face) {
        return RusticFaceShapes.FaceShape.CENTER_SMALL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, LEAVES);
    }
}
