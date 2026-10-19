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

    @Override
    // Middle-click / pick-block yields the rope, not the tied-stake knot (the knot has no item of its own).
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(RusticBlocks.ROPE.get().asItem());
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                       net.minecraft.world.entity.player.Player player, boolean willHarvest,
                                       FluidState fluid) {
        // Un-tying a knot reverts the block back to the plain crop-stake pole underneath it.
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
