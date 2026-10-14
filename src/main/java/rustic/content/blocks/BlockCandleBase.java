package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rustic.content.blocks.RusticFaceShapes.Declarable;
import rustic.content.blocks.RusticFaceShapes.FaceShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * 1.12.2's {@code BlockCandle} and {@code BlockIronTorch}, which were the same class typed out twice:
 * identical properties, identical placement and survival rules, identical particle code, differing only
 * in the hardness (1.0F against 0.0F), the five collision boxes and how far the flame sits off the wall
 * and up from the wick. What is shared lives here; the two subclasses supply those five numbers.
 *
 * <p>{@code BlockCandleDouble} is deliberately not a subclass - it hangs a pair of candles along one
 * axis, so its orientation cannot be a {@link Direction} and it needs its own property and boxes.
 *
 * <p>The legacy {@code onBlockAdded}/{@code neighborChanged}/{@code checkForDrop} triangle collapses to
 * the pair 1.21 uses for every wall-mounted block: {@link #canSurvive} vets a position (which is also
 * what the placement context asks before it accepts the state this class returns), and
 * {@link #updateShape} clears the block when the anchor in the direction the state points at stops
 * holding it. The drop itself is the legacy one - item plus break sound - through {@link RusticDrops}.
 */
public abstract class BlockCandleBase extends Block implements Declarable {

    /**
     * 1.12.2's {@code PropertyDirection.create("facing", facing != DOWN)}. The allowed-value list is the
     * only part that carries over: 1.21 stores whole states, so nothing needs a metadata packing scheme
     * to keep DOWN out of existence.
     */
    public static final DirectionProperty FACING = DirectionProperty.create(
            "facing", Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP);

    private final Map<Direction, VoxelShape> shapes;
    /**
     * The three particle offsets, in blocks: how far the flame leans into the wall it is clipped to, how
     * far above the block it sits there, and how far above it when the candle is standing. Protected
     * because {@link BlockCandleLever} re-uses them to move the flame when the handle is pulled.
     */
    protected final double wallOffset;
    protected final double wallRise;
    protected final double standingRise;

    protected BlockCandleBase(
            BlockBehaviour.Properties properties,
            Map<Direction, VoxelShape> shapes,
            double wallOffset,
            double wallRise,
            double standingRise
    ) {
        super(properties);
        this.shapes = shapes;
        this.wallOffset = wallOffset;
        this.wallRise = wallRise;
        this.standingRise = standingRise;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    /** Builds the direction-to-box map in the order the legacy AABB constant block read. */
    protected static Map<Direction, VoxelShape> shapesFor(
            VoxelShape standing,
            VoxelShape north,
            VoxelShape south,
            VoxelShape west,
            VoxelShape east
    ) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.UP, standing);
        shapes.put(Direction.NORTH, north);
        shapes.put(Direction.SOUTH, south);
        shapes.put(Direction.WEST, west);
        shapes.put(Direction.EAST, east);
        return shapes;
    }

    /**
     * 1.12.2's {@code canPlaceAt}, which is both the placement test and the survival test: a wall candle
     * needs a solid face to cling to, a standing one needs something to stand on.
     *
     * <p>{@code world.isSideSolid(pos, side, true)} becomes
     * {@link RusticFaceShapes#isSolidFace}: the legacy {@code defaultInside} argument only answered for
     * air, and air cannot hold a wall candle up either way.
     *
     * <p>Static and package-visible because {@link BlockCandleDouble} asks the same question of an
     * orientation that is not a {@link Direction} property, and so is not a subclass of this class.
     */
    protected static boolean canPlaceAt(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos anchorPos = pos.relative(facing.getOpposite());
        BlockState anchor = level.getBlockState(anchorPos);
        return facing.getAxis().isHorizontal()
                ? RusticFaceShapes.isSolidFace(anchor, level, anchorPos, facing)
                : facing == Direction.UP && canPlaceOn(level, anchorPos);
    }

    /**
     * 1.12.2's {@code canPlaceOn}: an attachable top face, or anywhere Forge's
     * {@code canPlaceTorchOnTop} said a torch could stand. The second test is 1.21's
     * {@link Block#canSupportCenter} - the rule vanilla torches themselves now use - which is worth
     * keeping even though {@link RusticFaceShapes#canAttach} already covers most of it: the legacy list
     * of torch-friendly blocks was hand-written and not all of them are attachable faces here. Saplings
     * were on it and have no collision shape, so that one case is a documented loss.
     */
    protected static boolean canPlaceOn(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return RusticFaceShapes.canAttach(state, level, pos, Direction.UP) || Block.canSupportCenter(level, pos, Direction.UP);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canPlaceAt(level, pos, state.getValue(FACING));
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
        // The vanilla idiom for a mounted block, and the same question the legacy checkForDrop asked:
        // only the anchor the state points at can knock it off. Returning AIR is the legacy
        // setBlockToAir - the drop that used to sit next to it is the part that has to be done here.
        if (direction.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)) {
            RusticDrops.dropAsItem(level, pos, state);
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // 1.12.2's onBlockAdded -> checkForDrop, for blocks that arrive without a placement context.
        if (!canPlaceAt(level, pos, state.getValue(FACING))) {
            RusticDrops.removeAndDrop(level, pos, state);
        }
    }

    /**
     * 1.12.2's {@code getStateForPlacement}: keep the clicked face if it works, else try the four walls,
     * else fall back to standing. Returning the default state rather than null matters - it lets
     * {@link #canSurvive} be the single place that refuses the placement, exactly as the legacy
     * {@code canPlaceBlockAt} loop over every facing did.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clicked = context.getClickedFace();
        if (canPlaceAt(level, pos, clicked)) {
            return this.defaultBlockState().setValue(FACING, clicked);
        }
        for (Direction horizontal : Direction.Plane.HORIZONTAL) {
            if (canPlaceAt(level, pos, horizontal)) {
                return this.defaultBlockState().setValue(FACING, horizontal);
            }
        }
        return this.defaultBlockState();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.get(state.getValue(FACING));
    }

    /**
     * 1.12.2's {@code randomDisplayTick}: one smoke and one flame particle at the position
     * {@link #flamePosition} works out. {@link #animateTick} is the client-only hook that replaced it, and
     * it is called with the same state.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Vec3 flame = this.flamePosition(state, pos);
        level.addParticle(ParticleTypes.SMOKE, flame.x, flame.y, flame.z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, flame.x, flame.y, flame.z, 0.0, 0.0, 0.0);
    }

    /**
     * Where the flame particle draws: the centre of the block raised to the height of the wick, leaned
     * into the wall when the candle is on one. Out of {@link #animateTick} so that
     * {@link BlockCandleLever} can drop it down the side of a pulled handle.
     */
    protected Vec3 flamePosition(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.7;
        double z = pos.getZ() + 0.5;

        if (facing.getAxis().isHorizontal()) {
            Direction anchor = facing.getOpposite();
            x += this.wallOffset * anchor.getStepX();
            z += this.wallOffset * anchor.getStepZ();
            y += this.wallRise;
        } else {
            y += this.standingRise;
        }
        return new Vec3(x, y, z);
    }

    /**
     * 1.12.2's {@code getBlockFaceShape}: the underside of a candle is a thin centre stub - the face
     * another candle, chain or lantern may hang from - and no other face of it holds anything.
     */
    @Override
    public FaceShape rusticFaceShape(BlockState state, Direction face) {
        return face == Direction.DOWN ? FaceShape.CENTER_SMALL : FaceShape.UNDEFINED;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        // A standing candle has nothing to mirror. 1.12.2 handed EnumFacing.UP to Mirror#toRotation
        // anyway, which throws, so a structure mirror containing a standing candle would have crashed;
        // the symmetric answer is the state unchanged.
        return facing.getAxis().isHorizontal()
                ? state.setValue(FACING, mirror.getRotation(facing).rotate(facing))
                : state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
