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

/**
 * 1.12.2's {@code BlockCandleLever}: a candle whose holder works as a redstone lever. It extends the
 * plain candle - same five boxes, same placement, same survival rules - and adds one {@code powered}
 * bit plus the self-releasing timer vanilla's own button has.
 *
 * <p>The legacy class declared its own copy of the {@code facing} property and its own metadata packing
 * for the powered bit (bit 3); both are gone, since 1.21 stores the pair of properties directly.
 * {@code setTickRandomly(true)} and the empty {@code randomTick} it came with are dropped too: the timer
 * that actually released the handle was {@code scheduleUpdate(pos, this, tickRate())}, which is
 * {@link net.minecraft.world.level.block.Block#scheduleTick} and {@link #tick} here. Random ticks would
 * only have called the empty method.
 */
public class BlockCandleLever extends BlockCandle {

    /** 1.12.2's {@code BlockButton.POWERED}, which is {@link BlockStateProperties#POWERED} verbatim. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    /** 1.12.2's {@code tickRate(world)}, which ignored the world and returned 80 - four seconds. */
    private static final int TICK_RATE = 80;

    /** How far the flame drops when the handle is pulled down, and how far back it swings. */
    private static final double PULLED_DROP = 0.125;
    private static final double PULLED_SWING = 0.35;

    public BlockCandleLever(BlockBehaviour.Properties properties) {
        super(properties);
        // 1.12.2: getBaseState().withProperty(FACING, UP).withProperty(POWERED, false). The facing half is
        // already the super's default, so only the new bit is added here.
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    /**
     * 1.12.2's {@code onBlockActivated}. The legacy method answered true whether or not the handle moved,
     * so a pulled lever simply swallowed the click; {@link InteractionResult#sidedSuccess} is the modern
     * way to answer "handled" on both sides without the client trying to move the handle itself.
     *
     * <p>The order of the calls is the legacy one - set the state, notify, sound, schedule - which
     * differs from vanilla's lever only in that a lever here can be pulled up but never pushed down by
     * hand: the handle releases itself.
     */
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

    /**
     * 1.12.2's {@code updateTick}, reached through the scheduled tick the click registered: the handle
     * falls back on its own four seconds later, and the release sound is the legacy
     * {@code playReleaseSound} call that sat next to it.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
            notifyNeighbors(level, pos, state.getValue(FACING));
            playReleaseSound(level, pos);
        }
    }

    /**
     * 1.12.2's {@code breakBlock}: a lever that is broken while still pulling has to tell the blocks it
     * was powering to stop. The two guards are vanilla 1.21's own - {@code onRemove} also runs when the
     * block is replaced by itself, and when a piston moves it, neither of which the legacy
     * {@code breakBlock} ever saw.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock()) && state.getValue(POWERED)) {
            notifyNeighbors(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * 1.12.2's {@code notifyNeighbors}: the block itself, because it is a power source, and the anchor it
     * is clipped to - which is {@code pos.offset(facing.getOpposite())}, the block behind it.
     */
    private void notifyNeighbors(Level level, BlockPos pos, Direction facing) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(facing.getOpposite()), this);
    }

    /**
     * 1.12.2's {@code getWeakPower}: fifteen to every side, as vanilla's button and lever do.
     *
     * <p>{@code getStrongPower} compared the stored {@code facing} with the queried side <em>without</em>
     * negating it, so the direct signal went to the block in front of the handle rather than into the wall
     * behind it - vanilla's lever powers {@code getConnectedDirection(state).getOpposite()} instead. The
     * legacy expression is kept as written (see {@link #getDirectSignal}); in practice a candle lever
     * therefore powers only the weakly-connected blocks around it.
     */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    /** 1.12.2's {@code getStrongPower}, kept literally: {@code facing == side}. See {@link #getSignal}. */
    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && state.getValue(FACING) == direction ? 15 : 0;
    }

    /** 1.12.2's {@code canProvidePower}. */
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * 1.12.2's {@code randomDisplayTick} for a pulled lever: the flame no longer leans into the wall - the
     * handle has swung away from it - and it sits a head lower, or, standing, swings back over the block.
     */
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
            // The legacy code multiplied the wall offset by (pulled ? 0.0 : 1.0) and dropped the height.
            y += this.wallRise - PULLED_DROP;
        } else {
            y += this.standingRise - PULLED_DROP;
            z -= PULLED_SWING;
        }
        return new Vec3(x, y, z);
    }

    /** 1.12.2's {@code playClickSound}, at the volume and pitch the legacy class hard-coded. */
    protected void playClickSound(Player player, Level level, BlockPos pos) {
        level.playSound(player, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
    }

    /** 1.12.2's {@code playReleaseSound}, which passed a null player so that everyone nearby hears it. */
    protected void playReleaseSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 0.5F);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }
}
