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

/**
 * 1.12.2's {@code BlockGrapeLeaves}: the fruiting leaf of the vinery. It is a member of the rope family - it
 * shares {@link BlockRopeBase}'s horizontal line, its support graph and the way a broken leaf reverts to the
 * rope it grew on - but it is a plant rather than a hanging line: it spreads one block outward along an
 * adjacent rope, then ripens grapes on the outer tips, and it never sags.
 *
 * <p>Behaviour translated from the legacy class:
 * <ul>
 *   <li>{@code GRAPES}/{@code DIST} are the legacy {@code PropertyBool}/{@code PropertyInteger 0..1} verbatim;
 *       the legacy {@code AXIS} was restricted to the two horizontal directions, and this block never stores
 *       {@code Y} - it is only ever placed from a rope whose axis is horizontal.</li>
 *   <li>The leaf's own support test ({@code isSideSupported}) is the legacy one - same-axis leaf, same-axis rope,
 *       a solid face, a tied stake or a lattice - because {@link BlockRopeBase}'s solid-face test alone would
 *       refuse a rope, and a leaf between two ropes must survive.</li>
 *   <li>The legacy {@code getActualState} dangle is {@link BlockRopeBase}'s stored {@code DANGLE}; a grape leaf
 *       overrides {@link #acceptsDangleFrom} to {@code false} so a rope hanging below a leaf never makes the
 *       block above it sag - the one exclusion the rope family's own notes had been holding for this block.</li>
 *   <li>{@code onBlockActivated} harvest becomes {@link #useWithoutItem}; {@code breakBlock}'s revert-to-rope
 *       becomes {@link #onDestroyedByPlayer} (mirroring {@link StakeTiedBlock}); {@code getPickBlock} becomes
 *       {@link #getCloneItemStack}, handing back the grapes rather than the (nonexistent) leaf block item.</li>
 * </ul>
 *
 * <p>Phase 17 S17.1 restored both visuals the earlier phases had deferred together: the foliage biome tint the legacy
 * {@code IColoredBlock} applied is registered for this block in {@code RusticClient.registerBlockColors}, and the
 * blockstate emits the legacy multipart leaf/vine/grape assembly (see {@code RusticBlockStates.grape}).
 */
public class GrapeLeavesBlock extends BlockRopeBase implements BonemealableBlock {

    public static final BooleanProperty GRAPES = BooleanProperty.create("grapes");
    public static final IntegerProperty DIST = IntegerProperty.create("distance", 0, 1);

    // Legacy growth rolls: getGrowthChance() is a fixed 1F, so nextInt((int)(35.0F/1F)+1) and nextInt(31).
    private static final int GRAPES_ROLL = (int) (35.0F / 1.0F) + 1;
    private static final int SPREAD_ROLL = (int) (30.0F / 1.0F) + 1;

    public GrapeLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // 1.12.2: setDefaultState(baseState AXIS=X, GRAPES=false, DIST=0). dangle is a real property here, false.
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

    /** A grape leaf is not a rope, so it never passes a sag on to the block hanging above it (1.12.2 exclusion). */
    @Override
    protected boolean acceptsDangleFrom(BlockState self) {
        return false;
    }

    /**
     * 1.12.2's {@code updateTick}: drop when the leaf has lost its support, then while lit either ripen grapes
     * on an outer tip (distance {@code > 0}, no grapes yet, nothing directly beneath) or creep one block further
     * along an adjacent rope (distance {@code < 1} and a rope to grow onto).
     */
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

    /**
     * 1.12.2's {@code onBlockActivated}: right-clicking a leaf that carries grapes picks them (one or two), sets
     * the grapes flag off and leaves the vine in place, exactly as the legacy {@code return true} did. With no
     * grapes the click is passed on, so the held item still gets to do its normal placement.
     */
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

    /**
     * 1.12.2's {@code breakBlock}: after a break the leaf reverts to the rope it grew along, keeping its axis.
     * The grapes, if any, are handled by the block's loot table - as with {@link StakeTiedBlock}, the revert is
     * the destroy hook and the produce is the loot. The hook is {@code onDestroyedByPlayer} rather than
     * {@code playerWillDestroy} because 1.21 never writes the state that hook returns to the world.
     */
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

    /** 1.12.2's {@code getPickBlock}: middle-click gives the grapes, never a leaf block item (there is none). */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(RusticItems.GRAPES.get());
    }

    // ------------------------------------------------------------------
    // Support - the legacy leaf graph, wider than the rope base's solid-face test.
    // ------------------------------------------------------------------

    /**
     * 1.12.2's {@code isSideSupported} for grape leaves: the same-axis leaf, the same-axis rope, a solid face, a
     * tied stake or a lattice all hold a vine up. {@link BlockRopeBase}'s version would refuse a rope, which is
     * precisely what a vinery hangs on, so the whole test is restated here.
     */
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

    /** A leaf only ever runs along X or Z, so (unlike the rope base) a {@code Y} axis is never supported. */
    @Override
    public boolean isBlockSupported(LevelReader level, BlockPos pos, BlockState state) {
        return switch (state.getValue(AXIS)) {
            case X -> isSideSupported(level, pos, state, Direction.WEST) && isSideSupported(level, pos, state, Direction.EAST);
            case Z -> isSideSupported(level, pos, state, Direction.NORTH) && isSideSupported(level, pos, state, Direction.SOUTH);
            case Y -> false;
        };
    }

    /** 1.12.2 recovered the clicked-face placement as survival; for a leaf that is simply "still held at both ends". */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isBlockSupported(level, pos, state);
    }

    // ------------------------------------------------------------------
    // Spreading along a rope, and bonemeal.
    // ------------------------------------------------------------------

    /** 1.12.2's {@code canSpread}: a distance-0 leaf with a same-axis rope on either side of its line. */
    public boolean canSpread(LevelReader level, BlockPos pos, BlockState state) {
        if (state.getValue(DIST) != 0) {
            return false;
        }
        Direction.Axis axis = state.getValue(AXIS);
        Direction along = firstAlong(axis);
        return isAxisRope(level.getBlockState(pos.relative(along)), axis)
                || isAxisRope(level.getBlockState(pos.relative(along.getOpposite())), axis);
    }

    /** 1.12.2's {@code spread}: turn one same-axis rope neighbour into a distance-1 leaf (random side if two). */
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
