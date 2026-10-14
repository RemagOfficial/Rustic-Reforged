package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rustic.content.blocks.RusticFaceShapes.Declarable;
import rustic.content.blocks.RusticFaceShapes.FaceShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * 1.12.2's {@code BlockCandleDouble}: two candles on one holder, either stood on a surface with the pair
 * lined up along one horizontal axis, or clipped to a wall with the pair lined up along the wall.
 *
 * <p>This is the one candle that cannot be a {@link Direction} property, because "standing, pointing up"
 * has two forms - the pair along X or along Z - and a direction has only one UP. The legacy class solved
 * it with a hand-written {@code EnumFacingWallOrXZ}, which is ported below as {@link CandleOrientation}
 * minus the metadata machinery, and it is why this class is not a {@link BlockCandleBase} subclass: the
 * base is built around a direction-valued state. The two placement tests it needs are the base's, reached
 * through the static {@link BlockCandleBase#canPlaceAt}.
 */
public class BlockCandleDouble extends Block implements Declarable {

    /**
     * 1.12.2's {@code BlockCandleDouble.EnumFacingWallOrXZ}, with {@code index}, {@code getFront},
     * {@code byName}, {@code rotateY} and {@code rotateYCCW} dropped: they existed to pack the property
     * into metadata and to rotate the enum by hand, and 1.21 stores the property itself and rotates
     * through {@link Rotation} on the block.
     *
     * <p>The two {@code UP_*} values are the pair stood on a surface, and their names are the legacy
     * ones: {@code up_x} is the holder whose candles are separated along Z, so the axis in the name is
     * the axis the holder bar runs along rather than the axis the candles are spread across. Both the
     * boxes and the particle offsets below keep that reading, because that is what the legacy blockstate
     * and model files were written against.
     */
    public enum CandleOrientation implements StringRepresentable {
        UP_X("up_x", Direction.UP, Direction.Axis.X),
        UP_Z("up_z", Direction.UP, Direction.Axis.Z),
        NORTH("north", Direction.NORTH, Direction.Axis.Z),
        SOUTH("south", Direction.SOUTH, Direction.Axis.Z),
        WEST("west", Direction.WEST, Direction.Axis.X),
        EAST("east", Direction.EAST, Direction.Axis.X);

        private final String name;
        private final Direction facing;
        private final Direction.Axis facingAxis;

        CandleOrientation(String name, Direction facing, Direction.Axis facingAxis) {
            this.name = name;
            this.facing = facing;
            this.facingAxis = facingAxis;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        /** The direction the candles point, which is the direction away from whatever holds them up. */
        public Direction getFacing() {
            return this.facing;
        }

        /** The axis the holder bar runs along; the two candles are separated along the other one. */
        public Direction.Axis getFacingAxis() {
            return this.facingAxis;
        }

        public boolean isUp() {
            return this == UP_X || this == UP_Z;
        }

        public boolean isWall() {
            return !isUp();
        }

        /** 1.12.2's {@code getOpposite}: the two standing forms have no opposite, so they map to themselves. */
        public CandleOrientation getOpposite() {
            return switch (this) {
                case UP_X -> UP_X;
                case UP_Z -> UP_Z;
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case WEST -> EAST;
                case EAST -> WEST;
            };
        }

        /** 1.12.2's {@code fromHorizontalFacing}, which threw for UP and DOWN and still does. */
        public static CandleOrientation fromHorizontalFacing(Direction facing) {
            return switch (facing) {
                case NORTH -> NORTH;
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                case EAST -> EAST;
                default -> throw new IllegalStateException(
                        "Unable to get wall or xz orientation of horizontal facing " + facing);
            };
        }

        /** The orientation a standing pair takes when the block is placed facing along {@code axis}. */
        public static CandleOrientation standing(Direction.Axis axis) {
            return axis == Direction.Axis.X ? UP_X : UP_Z;
        }
    }

    public static final EnumProperty<CandleOrientation> FACING =
            EnumProperty.create("facing", CandleOrientation.class);

    // 1.12.2's axis aligned bounding boxes times sixteen. The standing pair is thin along the holder bar
    // and wide across the two candles; the wall pair is the same box laid against the wall it hangs on.
    protected static final VoxelShape STANDING_X_SHAPE = Block.box(6.4, 0.0, 3.52, 9.6, 15.0, 12.48);
    protected static final VoxelShape STANDING_Z_SHAPE = Block.box(3.52, 0.0, 6.4, 12.48, 15.0, 9.6);
    protected static final VoxelShape NORTH_SHAPE = Block.box(2.8, 0.0, 10.2, 13.2, 12.8, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(2.8, 0.0, 0.0, 13.2, 12.8, 5.8);
    protected static final VoxelShape WEST_SHAPE = Block.box(10.2, 0.0, 2.8, 16.0, 12.8, 13.2);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 2.8, 5.8, 12.8, 13.2);

    /** How far the flame leans into the wall, how far it rises, and how far apart the two candles are. */
    private static final double WALL_OFFSET = 0.2609375;
    private static final double WALL_RISE = 0.25;
    private static final double STANDING_RISE = 0.33;
    private static final double WALL_CANDLE_SPACING = 0.23125;
    private static final double STANDING_CANDLE_SPACING = 0.1875;

    private final Map<CandleOrientation, VoxelShape> shapes = shapes();

    public BlockCandleDouble(BlockBehaviour.Properties properties) {
        // setHardness(1F), Material.CIRCUITS, setSoundType(METAL), setLightLevel(1.0F) all arrive as
        // lit(RusticBlockProperties.circuits(1.0F)); the default orientation is UP_X, as in 1.12.2.
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, CandleOrientation.UP_X));
    }

    private static Map<CandleOrientation, VoxelShape> shapes() {
        Map<CandleOrientation, VoxelShape> shapes = new EnumMap<>(CandleOrientation.class);
        shapes.put(CandleOrientation.UP_X, STANDING_X_SHAPE);
        shapes.put(CandleOrientation.UP_Z, STANDING_Z_SHAPE);
        shapes.put(CandleOrientation.NORTH, NORTH_SHAPE);
        shapes.put(CandleOrientation.SOUTH, SOUTH_SHAPE);
        shapes.put(CandleOrientation.WEST, WEST_SHAPE);
        shapes.put(CandleOrientation.EAST, EAST_SHAPE);
        return shapes;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return BlockCandleBase.canPlaceAt(level, pos, state.getValue(FACING).getFacing());
    }

    /**
     * 1.12.2's {@code neighborChanged} and {@code onBlockAdded}, both of which funnelled into the same
     * {@code checkForDrop}. The legacy version re-tested the anchor on <em>every</em> neighbour change;
     * the 1.21 idiom only asks when the one neighbour that can hold it up changes, which is the same
     * answer with less work.
     */
    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbourState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos
    ) {
        if (direction == state.getValue(FACING).getFacing().getOpposite() && !state.canSurvive(level, pos)) {
            RusticDrops.dropAsItem(level, pos, state);
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // 1.12.2's onBlockAdded -> checkForDrop, for blocks that arrive without a placement context.
        if (!canSurvive(state, level, pos)) {
            RusticDrops.removeAndDrop(level, pos, state);
        }
    }

    /**
     * 1.12.2's {@code getStateForPlacement}, including its odd corner: clicking a floor does not pick an
     * orientation at all, it borrows one from the way the placer is standing, so that the pair of candles
     * lines up across the player's view rather than into it.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clicked = context.getClickedFace();

        if (BlockCandleBase.canPlaceAt(level, pos, clicked)) {
            return this.defaultBlockState().setValue(FACING, orientationFor(clicked, context));
        }
        for (Direction horizontal : Direction.Plane.HORIZONTAL) {
            BlockPos anchorPos = pos.relative(horizontal.getOpposite());
            BlockState anchor = level.getBlockState(anchorPos);
            if (RusticFaceShapes.isSolidFace(anchor, level, anchorPos, horizontal)) {
                return this.defaultBlockState().setValue(FACING, CandleOrientation.fromHorizontalFacing(horizontal));
            }
        }
        // 1.12.2's default state, which canSurvive then refuses: the placement fails as the legacy
        // canPlaceBlockAt loop over every facing did.
        return this.defaultBlockState();
    }

    /**
     * The orientation a placement at {@code clicked} takes. A wall click keeps the direction it clicked
     * from; a floor click reads the placer's facing, and with no placer - a dispenser, a structure - the
     * legacy code fell back to {@code UP_X}, which needs the player lookup to be null-checked rather than
     * trusting {@link BlockPlaceContext#getHorizontalDirection()}, whose no-entity default is SOUTH.
     */
    private CandleOrientation orientationFor(Direction clicked, BlockPlaceContext context) {
        if (clicked.getAxis().isHorizontal()) {
            return CandleOrientation.fromHorizontalFacing(clicked);
        }
        if (context.getPlayer() == null) {
            return CandleOrientation.UP_X;
        }
        return CandleOrientation.standing(context.getHorizontalDirection().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.get(state.getValue(FACING));
    }

    /**
     * 1.12.2's {@code randomDisplayTick}: two smoke and two flame particles, one per candle, spread along
     * the axis the holder does <em>not</em> run along. The legacy code multiplied that spacing into either
     * the X or the Z coordinate depending on {@code getFacingAxis()}, which is the branch below.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        CandleOrientation orientation = state.getValue(FACING);
        Direction facing = orientation.getFacing();

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.7;
        double z = pos.getZ() + 0.5;
        double spacing;

        if (facing.getAxis().isHorizontal()) {
            Direction anchor = facing.getOpposite();
            x += WALL_OFFSET * anchor.getStepX();
            z += WALL_OFFSET * anchor.getStepZ();
            y += WALL_RISE;
            spacing = WALL_CANDLE_SPACING;
        } else {
            y += STANDING_RISE;
            spacing = STANDING_CANDLE_SPACING;
        }

        // The candles sit either side of the centre, along whichever horizontal axis the bar is not on.
        boolean barAlongX = orientation.getFacingAxis() == Direction.Axis.X;
        double firstX = barAlongX ? x : x + spacing;
        double firstZ = barAlongX ? z + spacing : z;
        double secondX = barAlongX ? x : x - spacing;
        double secondZ = barAlongX ? z - spacing : z;

        addFlamePair(level, firstX, y, firstZ);
        addFlamePair(level, secondX, y, secondZ);
    }

    private static void addFlamePair(Level level, double x, double y, double z) {
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }

    /**
     * 1.12.2's {@code getBlockFaceShape}: the underside of the holder is a thin centre stub, the face a
     * chain or another candle may hang from.
     */
    @Override
    public FaceShape rusticFaceShape(BlockState state, Direction face) {
        return face == Direction.DOWN ? FaceShape.CENTER_SMALL : FaceShape.UNDEFINED;
    }

    /**
     * 1.12.2's {@code withRotation}. A wall pair follows the direction it faces; a standing pair has no
     * direction to follow, so a quarter turn either way swaps the two forms - which is right, because the
     * candles were spread across one horizontal axis and are now spread across the other.
     */
    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        CandleOrientation orientation = state.getValue(FACING);
        if (orientation.isWall()) {
            return state.setValue(FACING, CandleOrientation.fromHorizontalFacing(rot.rotate(orientation.getFacing())));
        }
        return switch (rot) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> state.setValue(FACING,
                    orientation.getFacingAxis() == Direction.Axis.X
                            ? CandleOrientation.UP_Z
                            : CandleOrientation.UP_X);
            default -> state;
        };
    }

    /**
     * 1.12.2's {@code withMirror}: a wall pair mirrors to the direction the mirror sends it, a standing
     * pair is unchanged - mirroring along the bar it runs on leaves the two candles where they were.
     */
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        CandleOrientation orientation = state.getValue(FACING);
        if (!orientation.isWall()) {
            return state;
        }
        Direction facing = orientation.getFacing();
        return state.setValue(FACING,
                CandleOrientation.fromHorizontalFacing(mirror.getRotation(facing).rotate(facing)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
