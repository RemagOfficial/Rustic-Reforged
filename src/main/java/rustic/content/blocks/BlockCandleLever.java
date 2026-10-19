package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BlockCandleLever extends BlockCandle {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final int TICK_RATE = 80;

    private static final double PULLED_DROP = 0.125;
    private static final double PULLED_SWING = 0.35;

    public BlockCandleLever(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide && !state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
            notifyNeighbors(level, pos, state.getValue(FACING));
            playClickSound(player, level, pos);
            level.scheduleTick(pos, this, TICK_RATE);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
            notifyNeighbors(level, pos, state.getValue(FACING));
            playReleaseSound(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock()) && state.getValue(POWERED)) {
            notifyNeighbors(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void notifyNeighbors(Level level, BlockPos pos, Direction facing) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(facing.getOpposite()), this);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && state.getValue(FACING) == direction ? 15 : 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected Vec3 flamePosition(BlockState state, BlockPos pos) {
        if (!state.getValue(POWERED)) {
            return super.flamePosition(state, pos);
        }
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.7;
        double z = pos.getZ() + 0.5;

        if (facing.getAxis().isHorizontal()) {

            y += this.wallRise - PULLED_DROP;
        } else {
            y += this.standingRise - PULLED_DROP;
            z -= PULLED_SWING;
        }
        return new Vec3(x, y, z);
    }

    protected void playClickSound(Player player, Level level, BlockPos pos) {
        level.playSound(player, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
    }

    protected void playReleaseSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 0.5F);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }
}
