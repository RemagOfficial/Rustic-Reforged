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

public class BlockWoodLantern extends Block implements Declarable {

    public static final DirectionProperty FACING = DirectionProperty.create(
            "facing", Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP);

    protected static final VoxelShape STANDING_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    protected static final VoxelShape NORTH_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 12.0);
    protected static final VoxelShape WEST_SHAPE = Block.box(4.0, 0.0, 4.0, 16.0, 16.0, 12.0);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 4.0, 12.0, 16.0, 12.0);

    private static final double FLAME_WANDER = 0.3;
    private static final double FLAME_RISE = 6.0 / 16.0;

    private static final Map<Direction, VoxelShape> SHAPES = shapes();

    public BlockWoodLantern(BlockBehaviour.Properties properties) {

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

        if (!state.canSurvive(level, pos)) {
            RusticDrops.removeAndDrop(level, pos, state);
        }
    }

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

    @Override
    public FaceShape rusticFaceShape(BlockState state, Direction face) {
        return face.getAxis().isVertical() ? FaceShape.CENTER_BIG : FaceShape.UNDEFINED;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {

        Direction facing = rot.rotate(state.getValue(FACING));
        return state.setValue(FACING, facing == Direction.DOWN ? Direction.UP : facing);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);

        return facing.getAxis().isHorizontal()
                ? state.setValue(FACING, mirror.getRotation(facing).rotate(facing))
                : state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
