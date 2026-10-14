package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.12.2's {@code BlockRopeBase}: the line of blocks that hang - chains now, lattice and rope with the
 * wooden set. Everything the legacy class did falls into four groups, and each maps onto 1.21
 * differently:
 *
 * <ul>
 *   <li><b>The dangling line itself.</b> The legacy {@code AXIS} property is
 *   {@link BlockStateProperties#AXIS} verbatim - same name, same three values - and its
 *   {@code getBoundingBox} becomes {@link #getShape}. {@code isOpaqueCube}/{@code isFullCube} returning
 *   false becomes {@code noOcclusion()} on the properties, and {@code isPassable} returning false needs
 *   nothing at all, because a non-empty collision shape already stops entities.</li>
 *   <li><b>Climbing.</b> {@code isLadder} was a Forge-added method; 1.21.1 asks the other question -
 *   {@code Entity#isStateClimbable} tests {@code BlockTags.CLIMBABLE} - so the ported behaviour is the
 *   block being added to {@code minecraft:climbable} in {@link rustic.data.RusticBlockTags} instead.</li>
 *   <li><b>Support.</b> {@code canPlaceBlockOnSide} is a placement test in 1.12.2, where the clicked face
 *   is an argument, and becomes {@link #canSurvive} here, where it is not - see that method for how the
 *   face is recovered from the stored axis. {@code neighborChanged} becomes {@link #updateShape}, which
 *   is handed the direction of the changed neighbour directly, and the legacy
 *   {@code world.isSideSolid(pos, side, false)} becomes
 *   {@link RusticFaceShapes#isSolidFace}.</li>
 *   <li><b>Dangle.</b> The legacy {@code getActualState} computed {@code dangle} on the fly for the
 *   model, which is the one 1.12.2 mechanism 1.21 deleted. It is a real property now, recomputed by
 *   {@link #updateShape} whenever the block below changes and by {@link #onPlace} when the block arrives
 *   by world generation or structure, which is how vanilla's own multi-part blocks handle the same
 *   problem.</li>
 * </ul>
 *
 * <p>Dropped: {@code getStateFromMeta}/{@code getMetaFromState}/{@code createBlockState} (1.21 stores
 * states rather than packing them into metadata) and {@code damageDropped} returning 0 (no metadata to
 * damage). {@code withRotation} survives as {@link #rotate}.
 */
public abstract class BlockRopeBase extends Block {

    public static final Property<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty DANGLE = BooleanProperty.create("dangle");

    // 1.12.2 axis-aligned bounding boxes, in sixteenths of a block because Block.box divides by 16.
    protected static final VoxelShape Y_SHAPE = Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
    protected static final VoxelShape X_SHAPE = Block.box(0.0, 7.0, 7.0, 16.0, 9.0, 9.0);
    protected static final VoxelShape Z_SHAPE = Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 16.0);
    protected static final VoxelShape X_DANGLE_SHAPE = Block.box(0.0, 0.0, 7.0, 16.0, 9.0, 9.0);
    protected static final VoxelShape Z_DANGLE_SHAPE = Block.box(7.0, 0.0, 0.0, 9.0, 9.0, 16.0);

    /** How far one right-click will extend a hanging line downwards. 1.12.2: {@code yOffset < 64}. */
    protected static final int MAX_HANG_LENGTH = 64;

    protected BlockRopeBase(BlockBehaviour.Properties properties) {
        super(properties);
        // 1.12.2: blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y) in each subclass
        // constructor, with dangle never set because an actual state was not a stored property.
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(DANGLE, false));
    }

    /**
     * 1.12.2's {@code onBlockActivated}: right-clicking a hanging line with more of the same block in
     * hand extends the line downwards instead of failing to place. The condition is unchanged - only when
     * the click could not have placed a block at the clicked neighbour - but the result type is new:
     * {@link ItemInteractionResult#PASS_TO_DEFAULT_BLOCK_INTERACTION} is what the legacy {@code false}
     * means, i.e. "let the held item do its normal placement".
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        Direction side = hitResult.getDirection();
        // 1.12.2 compared against Item.getItemFromBlock(this); asItem() is the same object now that every
        // block has exactly one block item.
        if (!canPlaceOnSide(level, pos.relative(side), side) && stack.is(this.asItem())) {
            if (!isBlockSupported(level, pos, state)) {
                RusticDrops.removeAndDrop(level, pos, state);
                return ItemInteractionResult.CONSUME;
            }

            int yOffset = 1;
            while (yOffset < MAX_HANG_LENGTH && level.getBlockState(pos.below(yOffset)).is(this)) {
                if (level.getBlockState(pos.below(yOffset)).getValue(AXIS) != Direction.Axis.Y) {
                    // 1.12.2: return false, so the held item gets to place normally.
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                yOffset++;
            }

            BlockPos target = pos.below(yOffset);
            // canPlaceBlockAt(pos) meant "is the material there replaceable", which is the same question
            // BlockState#canBeReplaced answers.
            if (level.getBlockState(target).canBeReplaced()) {
                level.setBlock(target, state.setValue(AXIS, Direction.Axis.Y).setValue(DANGLE, false), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                // 1.12.2 played the place sound at (x, y - yOffset, z), i.e. the corner of the new block.
                RusticDrops.playPlaceSound(level, target, state);
                return ItemInteractionResult.CONSUME;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 1.12.2 ran getBoundingBox through getActualState first so that dangle was visible to the shape;
        // with dangle stored in the state that step is already done.
        return switch (state.getValue(AXIS)) {
            case Y -> Y_SHAPE;
            case X -> state.getValue(DANGLE) ? X_DANGLE_SHAPE : X_SHAPE;
            case Z -> state.getValue(DANGLE) ? Z_DANGLE_SHAPE : Z_SHAPE;
        };
    }

    /**
     * 1.12.2's {@code canPlaceBlockOnSide(world, pos, side)}, recovered as a survival test.
     *
     * <p>The legacy method was only ever asked with the face the player clicked, and the block then
     * stored just {@code side.getAxis()} - the sign of the click was thrown away at placement. So a
     * horizontal line survives when either of the two faces along its axis would have accepted it, which
     * is what {@link #canSurvive} below does.
     */
    protected boolean canPlaceOnSide(LevelReader level, BlockPos pos, Direction side) {
        if (side == Direction.UP) {
            // 1.12.2: "trying to place on the ground? see if there is a block above to hang from".
            return canPlaceOnSide(level, pos, Direction.DOWN);
        }

        BlockPos anchorPos = pos.relative(side.getOpposite());
        BlockState anchor = level.getBlockState(anchorPos);
        boolean isThis = anchor.is(this) && anchor.getValue(AXIS) == side.getAxis();

        return isThis || RusticFaceShapes.isSolidFace(anchor, level, anchorPos, side);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction.Axis axis = state.getValue(AXIS);
        if (axis == Direction.Axis.Y) {
            return canPlaceOnSide(level, pos, Direction.UP);
        }
        Direction positive = firstAlong(axis);
        return canPlaceOnSide(level, pos, positive) || canPlaceOnSide(level, pos, positive.getOpposite());
    }

    /**
     * 1.12.2's {@code neighborChanged}, which derived the direction of the changed neighbour from
     * {@code fromPos} and then dropped the block. 1.21 hands that direction over as an argument, so the
     * derivation and its null case disappear; the two drop branches are copied round.
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
        if (state.getValue(AXIS) == direction.getAxis() && !isSideSupported(level, pos, state, direction)) {
            if (direction == Direction.UP || !isBlockSupported(level, pos, state)) {
                // The legacy dropBlock also called setBlockToAir; here returning AIR is that call, and
                // doing it from inside updateShape would be a re-entrant setBlock in another one.
                RusticDrops.dropAsItem(level, pos, state);
                return Blocks.AIR.defaultBlockState();
            }
        }
        return state.setValue(DANGLE, shouldDangle(level, pos, state));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // Only reachable for blocks that arrive without a placement context - world generation, structures,
        // fill commands - because a real placement already computed dangle in getStateForPlacement.
        boolean dangle = shouldDangle(level, pos, state);
        if (state.getValue(DANGLE) != dangle) {
            level.setBlock(pos, state.setValue(DANGLE, dangle), Block.UPDATE_ALL);
        }
    }

    /**
     * 1.12.2's {@code getActualState}: a horizontal line only sags when the block underneath it is the
     * same kind of block hanging straight down.
     *
     * <p>The legacy test also excluded {@code BlockGrapeLeaves}, which extends this class but is not a
     * rope and does not exist yet - it arrives with the crops. {@link #acceptsDangleFrom} is where that
     * exclusion lives so the leaf block can refuse it when it lands.
     */
    protected boolean shouldDangle(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getValue(AXIS) == Direction.Axis.Y) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof BlockRopeBase ropeBase
                && ropeBase.acceptsDangleFrom(below)
                && below.getValue(AXIS) == Direction.Axis.Y;
    }

    /** Whether a rope hanging directly below may make the block above it sag. 1.12.2 excluded grape leaves. */
    protected boolean acceptsDangleFrom(BlockState self) {
        return true;
    }

    /**
     * 1.12.2's {@code isSideSupported}: is the block in that direction part of the same line, or solid
     * enough on the face it shows us to hold one up.
     */
    public boolean isSideSupported(LevelReader level, BlockPos pos, BlockState state, Direction facing) {
        if (facing == Direction.DOWN) {
            return false;
        }

        BlockPos anchorPos = pos.relative(facing);
        BlockState anchor = level.getBlockState(anchorPos);
        boolean isSame = anchor.is(state.getBlock())
                && ((state.getValue(AXIS) == Direction.Axis.Y && facing.getAxis() == Direction.Axis.Y)
                || anchor.getValue(AXIS) == state.getValue(AXIS));

        return isSame || RusticFaceShapes.isSolidFace(anchor, level, anchorPos, facing.getOpposite());
    }

    /** 1.12.2's {@code isBlockSupported}: a horizontal line needs both ends held, a vertical one only the top. */
    public boolean isBlockSupported(LevelReader level, BlockPos pos, BlockState state) {
        return switch (state.getValue(AXIS)) {
            case X -> isSideSupported(level, pos, state, Direction.WEST) && isSideSupported(level, pos, state, Direction.EAST);
            case Y -> isSideSupported(level, pos, state, Direction.UP);
            case Z -> isSideSupported(level, pos, state, Direction.NORTH) && isSideSupported(level, pos, state, Direction.SOUTH);
        };
    }

    /** The positive direction of an axis, without going through an API the 1.21.1 mappings may not have. */
    private static Direction firstAlong(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.SOUTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 1.12.2: super.getStateForPlacement(...).withProperty(AXIS, facing.getAxis())
        BlockState state = this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
        return state.setValue(DANGLE, shouldDangle(context.getLevel(), context.getClickedPos(), state));
    }

    /** 1.12.2's {@code withRotation}: a quarter turn swaps the two horizontal axes, as for vanilla pillars. */
    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return switch (rot) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                default -> state;
            };
            default -> state;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, DANGLE);
    }
}
