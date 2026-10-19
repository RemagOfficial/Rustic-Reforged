package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;
import rustic.content.blockentity.CondenserAdvancedBlockEntity;
import rustic.content.blockentity.RusticBlockEntities;

/**
 * 1.12.2's {@code BlockCondenserAdvanced}: the two-block advanced condenser. The lower half ({@code top=false}) owns
 * the entire seven-slot inventory, water tank and brew timers in its {@link CondenserAdvancedBlockEntity}; the upper
 * half ({@code top=true}) carries no block entity at all and only forwards interaction down, exactly as legacy
 * {@code TileEntityCondenserAdvancedTop} merely delegated capabilities to the tile below. Because there is one real
 * inventory, this is <em>not</em> a {@link net.minecraft.world.CompoundContainer} merge like the double cabinet -
 * clicking either half opens the same bottom container, which is the "shared logical inventory" the pairing test
 * checks.
 *
 * <p>The legacy {@code FACING} property and {@code hasRetorts} gate are restored here too: the advanced still only opens
 * when flanked on both sides (perpendicular to its facing) by outward-turned {@link RusticMachineBlocks#RETORT_ADVANCED}
 * retorts of its own tier (see {@link CondenserFlanking} for the shared rule). {@code FACING} also turns the front panel
 * with the block, reproducing the 1.12.2 orientable body. The two-block lifecycle - place a bottom, it grows a top;
 * break either half, the other follows - mirrors legacy {@code onBlockAdded}/{@code onBlockHarvested} via 1.21's
 * {@link #onPlace} and {@link #playerWillDestroy}.
 */
public class CondenserAdvancedBlock extends BaseEntityBlock {

    public static final MapCodec<CondenserAdvancedBlock> CODEC = simpleCodec(CondenserAdvancedBlock::new);

    /** 1.12.2 {@code BOTTOM}, inverted to the more common {@code top} convention the cabinet uses. */
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    /** Legacy {@code FACING}: which way the front panel looks and which axis the retorts flank. */
    public static final DirectionProperty FACING = RetortBlock.FACING;

    private static final VoxelShape BOTTOM_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final VoxelShape TOP_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);

    public CondenserAdvancedBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TOP, false)
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public MapCodec<CondenserAdvancedBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, FACING);
    }

    /** Place as a bottom half facing the placer, and only when there is headroom above - the legacy {@code canPlaceBlockAt} rule. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        if (!level.getBlockState(context.getClickedPos().above()).canBeReplaced()) {
            return null;
        }
        return this.defaultBlockState().setValue(TOP, false)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** A freshly placed bottom grows its top half above it, mirroring legacy {@code onBlockAdded}. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.getValue(TOP)) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).canBeReplaced()) {
                level.setBlock(above, state.setValue(TOP, true), Block.UPDATE_ALL);
            }
        }
    }

    /** Breaking either half silently removes the partner, mirroring legacy {@code onBlockHarvested}. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos partner = state.getValue(TOP) ? pos.below() : pos.above();
            BlockState partnerState = level.getBlockState(partner);
            if (partnerState.is(this) && partnerState.getValue(TOP) != state.getValue(TOP)) {
                level.setBlock(partner, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Only the bottom half has an inventory to hold; the top half is decoration + a click forwarder.
        return state.getValue(TOP) ? null : new CondenserAdvancedBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TOP) ? TOP_SHAPE : BOTTOM_SHAPE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(TOP)) {
            return null;
        }
        return createTickerHelper(type, RusticBlockEntities.CONDENSER_ADVANCED.get(),
                CondenserAdvancedBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        CondenserAdvancedBlockEntity condenser = resolveBottom(level, pos, state);
        if (condenser == null) {
            // Not correctly flanked by its advanced retorts (or the bottom half is not resolved yet): CONSUME the
            // interaction so the held item is not used. Only a consuming result makes ServerPlayerGameMode.useItemOn
            // return before stack.useOn places the held BlockItem (PASS/SKIP_DEFAULT/FAIL all still place).
            return ItemInteractionResult.CONSUME;
        }
        if (level.isClientSide) {
            // Operable, but the menu/bucket action is server-side: consume so the client never predicts a placement.
            return ItemInteractionResult.sidedSuccess(true);
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, condenser.getTank())) {
            return ItemInteractionResult.SUCCESS;
        }
        player.openMenu(condenser);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        CondenserAdvancedBlockEntity condenser = resolveBottom(level, pos, state);
        if (condenser == null) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            FluidStack simulated = condenser.getTank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
            if (!simulated.isEmpty() && !FluidUtil.getFilledBucket(simulated).isEmpty()) {
                ItemStack bucket = FluidUtil.getFilledBucket(simulated);
                condenser.getTank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
                if (!player.getAbilities().instabuild) {
                    ItemHandlerHelper.giveItemToPlayer(player, bucket);
                }
                return InteractionResult.sidedSuccess(false);
            }
        }
        player.openMenu(condenser);
        return InteractionResult.CONSUME;
    }

    /**
     * The bottom half's block entity when the column is correctly flanked by advanced retorts, otherwise {@code null}.
     * This is the restored legacy {@code hasRetorts} gate (see {@link CondenserFlanking}): without its two
     * outward-facing {@link RusticMachineBlocks#RETORT_ADVANCED} retorts the still will not open, so both the GUI and
     * the bucket interaction require a properly shaped multi-block. A top-half click resolves down to the
     * inventory-owning bottom before the flank is checked, so the gate is always evaluated at the bottom position.
     */
    @Nullable
    private CondenserAdvancedBlockEntity resolveBottom(Level level, BlockPos pos, BlockState state) {
        BlockPos bottom = state.getValue(TOP) ? pos.below() : pos;
        if (!(level.getBlockEntity(bottom) instanceof CondenserAdvancedBlockEntity be)) {
            return null;
        }
        return CondenserFlanking.hasRetorts(level, bottom, level.getBlockState(bottom),
                RusticMachineBlocks.RETORT_ADVANCED.get()) ? be : null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        // The bottom half owns the inventory; empty it when the block actually leaves the world (top half has none).
        if (!state.is(newState.getBlock()) && !state.getValue(TOP)
                && level.getBlockEntity(pos) instanceof CondenserAdvancedBlockEntity condenser) {
            ItemStackHandler handler = condenser.getItemHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, handler.getStackInSlot(slot));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
