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

public abstract class BlockCandleBase extends Block implements Declarable {

    public static final DirectionProperty FACING = DirectionProperty.create(
            "facing", Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP);

    private final Map<Direction, VoxelShape> shapes;

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

    protected static boolean canPlaceAt(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos anchorPos = pos.relative(facing.getOpposite());
        BlockState anchor = level.getBlockState(anchorPos);
        return facing.getAxis().isHorizontal()
                ? RusticFaceShapes.isSolidFace(anchor, level, anchorPos, facing)
                : facing == Direction.UP && canPlaceOn(level, anchorPos);
    }

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

        if (direction.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)) {
            RusticDrops.dropAsItem(level, pos, state);
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {

        if (!canPlaceAt(level, pos, state.getValue(FACING))) {
            RusticDrops.removeAndDrop(level, pos, state);
        }
    }

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

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Vec3 flame = this.flamePosition(state, pos);
        level.addParticle(ParticleTypes.SMOKE, flame.x, flame.y, flame.z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, flame.x, flame.y, flame.z, 0.0, 0.0, 0.0);
    }

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

        return facing.getAxis().isHorizontal()
                ? state.setValue(FACING, mirror.getRotation(facing).rotate(facing))
                : state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
