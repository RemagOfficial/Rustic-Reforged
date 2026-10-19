package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * 1.12.2's {@code BlockStakeTied}: a {@link CropStakeBlock} knot tied with rope, made by right-clicking
 * rope onto a stake. It renders the four horizontal connections of the pole to neighbouring rope, grape
 * vines or other tied stakes.
 *
 * <p>Those connections were a legacy {@code getActualState} - a purely visual, on-the-fly state the model
 * read but that was never stored. 1.21 deleted {@code getActualState}, so the port keeps the same four
 * flags as <em>real</em> {@link BooleanProperty}s recomputed by {@link #updateShape} and
 * {@link #getStateForPlacement}, the approach {@link BlockRopeBase} already takes for its own {@code dangle}
 * flag. The neighbour test is the legacy one: a rope counts only when its stored axis matches the direction
 * being connected, and another tied stake always counts.
 *
 * <p>Deferred per the phase boundary: the grape-leaf branch of the neighbour test arrives with the vinery
 * (Phase 5's grape group), and the four-way knot model is Phase 9 art. This class owns the block, the
 * behaviour and the rope drop.
 */
public class StakeTiedBlock extends Block {

    public static final MapCodec<StakeTiedBlock> CODEC = simpleCodec(
            p -> new StakeTiedBlock(p, () -> RusticFarmBlocks.CROP_STAKE.get()));

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");

    private static final java.util.Map<Direction, BooleanProperty> CONNECTION_BY_HORIZONTAL =
            java.util.Map.of(
                    Direction.NORTH, NORTH,
                    Direction.SOUTH, SOUTH,
                    Direction.WEST, WEST,
                    Direction.EAST, EAST);

    // 1.12.2 KNOT_AABB 0.3125..0.6875 selection; STAKE_AABB 0.375..0.625 collision.
    private static final VoxelShape KNOT_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape STAKE_COLLISION = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);

    private final Supplier<? extends Block> cropStake;

    public StakeTiedBlock(BlockBehaviour.Properties properties, Supplier<? extends Block> cropStake) {
        super(properties);
        this.cropStake = cropStake;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(WEST, false).setValue(SOUTH, false).setValue(EAST, false));
    }

    @Override
    public MapCodec<StakeTiedBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return KNOT_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return STAKE_COLLISION;
    }

    /** 1.12.2's {@code getPickBlock}: middle-click gives the rope, not the tied stake. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(RusticBlocks.ROPE.get().asItem());
    }

    /**
     * 1.12.2's {@code breakBlock}: after a break the pole reverts to a bare {@link CropStakeBlock} and the
     * rope is returned. The revert is {@link #onDestroyedByPlayer}; the rope is the loot table. (Returning a
     * replacement state from {@code playerWillDestroy} is a dead idiom in 1.21 - the engine only feeds that
     * state to the drop/destroy calls, it never writes it to the world, so the revert must set the block itself.)
     */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                       net.minecraft.world.entity.player.Player player, boolean willHarvest,
                                       FluidState fluid) {
        boolean removed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        if (removed) {
            level.setBlock(pos, cropStake.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        return removed;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbourState,
            net.minecraft.world.level.LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos
    ) {
        return withConnections(state, level, pos);
    }

    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        BlockState updated = withConnections(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    /** 1.12.2's {@code getActualState} neighbour scan, now stored in the state instead of computed per frame. */
    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (java.util.Map.Entry<Direction, BooleanProperty> entry : CONNECTION_BY_HORIZONTAL.entrySet()) {
            Direction direction = entry.getKey();
            state = state.setValue(entry.getValue(), connectsTo(level, pos.relative(direction), direction));
        }
        return state;
    }

    private boolean connectsTo(BlockGetter level, BlockPos neighbourPos, Direction direction) {
        BlockState neighbour = level.getBlockState(neighbourPos);
        Block block = neighbour.getBlock();
        if (block == this) {
            return true;
        }
        if (block instanceof BlockRopeBase && neighbour.getValue(BlockRopeBase.AXIS) == direction.getAxis()) {
            return true;
        }
        // The grape-leaf connection joins this test when the vinery lands (see the class notes).
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return switch (rot) {
            case CLOCKWISE_90 -> state.setValue(NORTH, state.getValue(EAST))
                    .setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST))
                    .setValue(WEST, state.getValue(NORTH));
            case COUNTERCLOCKWISE_90 -> state.setValue(NORTH, state.getValue(WEST))
                    .setValue(WEST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(EAST))
                    .setValue(EAST, state.getValue(NORTH));
            case CLOCKWISE_180 -> state.setValue(NORTH, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(NORTH))
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(WEST, state.getValue(EAST));
            default -> state;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, WEST, SOUTH, EAST);
    }
}
