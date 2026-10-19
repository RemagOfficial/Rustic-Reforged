package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.CommonHooks;
import rustic.content.items.RusticItems;

public class GrapeLeavesBlock extends BlockRopeBase implements BonemealableBlock {

    public static final BooleanProperty GRAPES = BooleanProperty.create("grapes");
    public static final IntegerProperty DIST = IntegerProperty.create("distance", 0, 1);

    private static final int GRAPES_ROLL = (int) (35.0F / 1.0F) + 1;
    private static final int SPREAD_ROLL = (int) (30.0F / 1.0F) + 1;

    public GrapeLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AXIS, Direction.Axis.X)
                .setValue(DANGLE, false)
                .setValue(GRAPES, false)
                .setValue(DIST, 0));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected boolean acceptsDangleFrom(BlockState self) {
        return false;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isBlockSupported(level, pos, state)) {
            RusticDrops.removeAndDrop(level, pos, state);
            return;
        }
        if (level.getRawBrightness(pos.above(), 0) < 9) {
            return;
        }
        int dist = state.getValue(DIST);
        if (dist > 0 && !state.getValue(GRAPES) && level.isEmptyBlock(pos.below())) {
            if (CommonHooks.canCropGrow(level, pos, state, random.nextInt(GRAPES_ROLL) == 0)) {
                level.setBlock(pos, state.setValue(GRAPES, true), Block.UPDATE_CLIENTS);
                CommonHooks.fireCropGrowPost(level, pos, state);
            }
        } else if (dist < 1 && canSpread(level, pos, state)) {
            if (CommonHooks.canCropGrow(level, pos, state, random.nextInt(SPREAD_ROLL) == 0)) {
                spread(level, pos, state);
                CommonHooks.fireCropGrowPost(level, pos, state);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(GRAPES)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(GRAPES, false), Block.UPDATE_ALL);
            ItemStack stack = new ItemStack(RusticItems.GRAPES.get(), level.random.nextInt(2) + 1);
            if (!player.getAbilities().instabuild && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        boolean removed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        if (removed) {
            level.setBlock(pos, RusticBlocks.ROPE.get().defaultBlockState()
                    .setValue(AXIS, state.getValue(AXIS)), Block.UPDATE_ALL);
        }
        return removed;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(RusticItems.GRAPES.get());
    }

    @Override
    public boolean isSideSupported(LevelReader level, BlockPos pos, BlockState state, Direction facing) {
        BlockState anchor = level.getBlockState(pos.relative(facing));
        Direction.Axis axis = state.getValue(AXIS);
        return (anchor.is(this) && anchor.getValue(AXIS) == axis)
                || (anchor.is(RusticBlocks.ROPE.get()) && anchor.getValue(AXIS) == axis)
                || anchor.is(RusticFarmBlocks.STAKE_TIED.get())
                || anchor.getBlock() instanceof BlockLattice
                || RusticFaceShapes.isSolidFace(anchor, level, pos.relative(facing), facing.getOpposite());
    }

    @Override
    public boolean isBlockSupported(LevelReader level, BlockPos pos, BlockState state) {
        return switch (state.getValue(AXIS)) {
            case X -> isSideSupported(level, pos, state, Direction.WEST) && isSideSupported(level, pos, state, Direction.EAST);
            case Z -> isSideSupported(level, pos, state, Direction.NORTH) && isSideSupported(level, pos, state, Direction.SOUTH);
            case Y -> false;
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isBlockSupported(level, pos, state);
    }

    public boolean canSpread(LevelReader level, BlockPos pos, BlockState state) {
        if (state.getValue(DIST) != 0) {
            return false;
        }
        Direction.Axis axis = state.getValue(AXIS);
        Direction along = firstAlong(axis);
        return isAxisRope(level.getBlockState(pos.relative(along)), axis)
                || isAxisRope(level.getBlockState(pos.relative(along.getOpposite())), axis);
    }

    public void spread(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(DIST) >= 1) {
            return;
        }
        Direction.Axis axis = state.getValue(AXIS);
        Direction along = firstAlong(axis);
        BlockPos plus = pos.relative(along);
        BlockPos minus = pos.relative(along.getOpposite());
        boolean plusRope = isAxisRope(level.getBlockState(plus), axis);
        boolean minusRope = isAxisRope(level.getBlockState(minus), axis);
        BlockPos target;
        if (plusRope && minusRope) {
            target = level.random.nextBoolean() ? plus : minus;
        } else if (plusRope) {
            target = plus;
        } else if (minusRope) {
            target = minus;
        } else {
            return;
        }
        level.setBlock(target, this.defaultBlockState().setValue(AXIS, axis).setValue(DIST, 1), Block.UPDATE_ALL);
    }

    private boolean isAxisRope(BlockState test, Direction.Axis axis) {
        return test.is(RusticBlocks.ROPE.get()) && test.getValue(AXIS) == axis;
    }

    private static Direction firstAlong(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.EAST;
            case Z -> Direction.SOUTH;
            default -> Direction.UP;
        };
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (state.getValue(DIST) > 0) {
            return !state.getValue(GRAPES) && level.isEmptyBlock(pos.below());
        }
        return canSpread(level, pos, state);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (state.getValue(DIST) > 0) {
            level.setBlock(pos, state.setValue(GRAPES, true), Block.UPDATE_ALL);
        } else {
            spread(level, pos, state);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DIST, GRAPES);
    }
}
