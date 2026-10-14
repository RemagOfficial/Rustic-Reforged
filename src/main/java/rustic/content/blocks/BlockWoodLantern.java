package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rustic.content.blocks.RusticFaceShapes.Declarable;
import rustic.content.blocks.RusticFaceShapes.FaceShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * 1.12.2's {@code BlockWoodLantern}: the wooden hanging lantern, which the mod built as a third mount
 * class beside {@link BlockCandleBase} and {@link BlockLantern} rather than as a subclass of either. It
 * takes the candle's five-way facing property - up, or one of the four walls, never down - and the
 * metal lantern's habit of presenting a mountable face of its own, but nothing else: it is wooden
 * (hardness 1.0F, wood sound, axe-mined) and it is the only lantern that falls when its anchor goes.
 *
 * <p>The placement and survival triangle is the legacy one, not vanilla's: a wall lantern needs a
 * {@code SOLID} face to cling to, a standing one needs an attachable top or anywhere a torch could
 * stand, and - a quirk of the legacy {@code checkForDrop} - a lantern whose state says "standing" also
 * survives when there is something under it to hang from instead. 1.12.2 wired that through
 * {@code onBlockAdded}/{@code neighborChanged}; here it is {@link #canSurvive}, {@link #updateShape}
 * and {@link #onPlace}, the same collapse {@link BlockCandleBase} made.
 */
public class BlockWoodLantern extends Block implements Declarable {

    /**
     * 1.12.2's {@code PropertyDirection.create("facing", facing != DOWN)} - the candle's property
     * re-declared rather than borrowed, because the two classes share no code and the legacy file read
     * as one self-contained block.
     */
    public static final DirectionProperty FACING = DirectionProperty.create(
            "facing", Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP);

    protected static final VoxelShape STANDING_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    protected static final VoxelShape NORTH_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 12.0);
    protected static final VoxelShape WEST_SHAPE = Block.box(4.0, 0.0, 4.0, 16.0, 16.0, 12.0);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 4.0, 12.0, 16.0, 12.0);

    /**
     * The flame in the middle of the box, given a third of a block to wander in horizontally and
     * 6/16ths of one to rise through: the legacy {@code randomDisplayTick} drew smoke and flame at the
     * same wandering point, and {@code animateTick} is the hook that replaced it.
     */
    private static final double FLAME_WANDER = 0.3;
    private static final double FLAME_RISE = 6.0 / 16.0;

    /** The five boxes, keyed by the facing they were selected for in 1.12.2's {@code getBoundingBox}. */
    private static final Map<Direction, VoxelShape> SHAPES = shapes();

    public BlockWoodLantern(BlockBehaviour.Properties properties) {
        // setHardness(1F), Material.WOOD, setSoundType(WOOD) and setLightLevel(1.0F) are
        // lit(RusticBlockProperties.wood(1.0F)); the CUTOUT layer and isOpaqueCube == false are
        // noOcclusion() plus the render_type in the model. The default is the standing lantern.
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    private static Map<Direction, VoxelShape> shapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.UP, STANDING_SHAPE);
        shapes.put(Direction.NORTH, NORTH_SHAPE);
        shapes.put(Direction.SOUTH, SOUTH_SHAPE);
        shapes.put(Direction.WEST, WEST_SHAPE);
        shapes.put(Direction.EAST, EAST_SHAPE);
        return Map.copyOf(shapes);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /**
     * 1.12.2's {@code canPlaceAt}: the block on the far side of the anchor face has to hold the lantern.
     * A wall mount asks for a {@code SOLID} face - {@code world.isSideSolid(pos, side, true)}, whose
     * {@code defaultInside} argument only answered for air - while the standing one accepts anything the
     * candle family also accepted, which is what {@link BlockCandleBase#canPlaceOn} is.
     *
     * <p>DOWN is answered rather than unreachable: the property never stores it, but the legacy
     * {@code checkForDrop} tested it as an alternative anchor for a standing lantern, and
     * {@link #canSurvive} does the same.
     */
    private static boolean canPlaceAt(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos anchorPos = pos.relative(facing.getOpposite());
        BlockState anchor = level.getBlockState(anchorPos);
        if (facing.getAxis().isHorizontal()) {
            return RusticFaceShapes.isSolidFace(anchor, level, anchorPos, facing);
        }
        return facing == Direction.DOWN
                ? RusticFaceShapes.canAttach(anchor, level, anchorPos, Direction.UP)
                : BlockCandleBase.canPlaceOn(level, anchorPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // 1.12.2's checkForDrop, in the same order: the anchor the state points at, or - for a standing
        // lantern only - the one above the floor it is meant to be resting on.
        Direction facing = state.getValue(FACING);
        return canPlaceAt(level, pos, facing)
                || facing == Direction.UP && canPlaceAt(level, pos, Direction.DOWN);
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
        // The legacy onNeighborChangedInternal: only a change at the anchor can knock it off, and the
        // ceiling it may alternatively hang from is DOWN, not whatever else changed.
        Direction facing = state.getValue(FACING);
        Direction anchor = facing.getOpposite();
        if ((direction == anchor || facing == Direction.UP && direction == Direction.DOWN)
                && !state.canSurvive(level, pos)) {
            RusticDrops.dropAsItem(level, pos, state);
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // 1.12.2's onBlockAdded -> checkForDrop, for lanterns that arrive without a placement context.
        if (!state.canSurvive(level, pos)) {
            RusticDrops.removeAndDrop(level, pos, state);
        }
    }

    /**
     * 1.12.2's {@code getStateForPlacement}: the clicked face when it holds - with DOWN read as a
     * standing lantern, because the property has no DOWN - else the first wall that does, else the
     * default. Returning the default state rather than null matters: it lets {@link #canSurvive} stay
     * the single place that refuses a placement, as the legacy {@code canPlaceBlockAt} loop did.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clicked = context.getClickedFace();
        Direction facing = clicked == Direction.DOWN ? Direction.UP : clicked;
        if (canPlaceAt(level, pos, facing)) {
            return this.defaultBlockState().setValue(FACING, facing);
        }
        for (Direction horizontal : Direction.Plane.HORIZONTAL) {
            if (canPlaceAt(level, pos, horizontal)) {
                return this.defaultBlockState().setValue(FACING, horizontal);
            }
        }
        return this.defaultBlockState();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5 + (random.nextFloat() * 2.0 * FLAME_WANDER - FLAME_WANDER);
        double y = pos.getY() + 0.5 + random.nextFloat() * FLAME_RISE;
        double z = pos.getZ() + 0.5 + (random.nextFloat() * 2.0 * FLAME_WANDER - FLAME_WANDER);
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }

    /**
     * 1.12.2's {@code getBlockFaceShape}: the lantern is a mount for anything vertical - chains,
     * candles and chandeliers hang from its top or bottom, and the metal lantern's cap is a
     * {@code CENTER_BIG} in the same two places - while none of its four sides holds anything.
     */
    @Override
    public FaceShape rusticFaceShape(BlockState state, Direction face) {
        return face.getAxis().isVertical() ? FaceShape.CENTER_BIG : FaceShape.UNDEFINED;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        // The legacy withRotation clamped a turned-downward lantern back to standing; the property has no
        // DOWN, so the clamp is what keeps the state valid.
        Direction facing = rot.rotate(state.getValue(FACING));
        return state.setValue(FACING, facing == Direction.DOWN ? Direction.UP : facing);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        // As in BlockCandleBase: 1.12.2 handed the direction straight to Mirror#toRotation, which throws
        // for UP, so the symmetric answer for a standing lantern is leaving it alone.
        return facing.getAxis().isHorizontal()
                ? state.setValue(FACING, mirror.getRotation(facing).rotate(facing))
                : state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
