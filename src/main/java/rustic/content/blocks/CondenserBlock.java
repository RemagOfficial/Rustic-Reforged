package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;
import rustic.content.blockentity.CondenserBlockEntity;
import rustic.content.blockentity.RusticBlockEntities;

/**
 * 1.12.2's {@code BlockCondenser}: the two-block basic elixir still. The lower half ({@code top=false}) owns the five-slot
 * inventory and the eight-bucket water tank in its {@link CondenserBlockEntity}; the upper half ({@code top=true}) carries
 * no block entity and only forwards interaction down, exactly like {@link CondenserAdvancedBlock} does for its column.
 * This restores the legacy two-block shape ({@code onBlockAdded} grew a {@code bottom=false} top over the placed base, and
 * breaking either half removed the partner), which the earlier single-block rebuild had collapsed.
 *
 * <p>The legacy {@code FACING} property and the {@code hasRetorts} gate are restored too: the still only opens when flanked
 * on both sides (perpendicular to its facing) by outward-turned {@link RusticMachineBlocks#RETORT retorts} of its own tier.
 * {@link CondenserFlanking} holds the shared rule. {@code FACING} also drives the model rotation, so the front panel turns
 * with the block just as the 1.12.2 orientable body did.
 *
 * <p>Right-clicking the (correctly flanked) still pours a held bucket into the water tank or draws one back, and any other
 * click opens the GUI where fuel, bottle and ingredients are loaded - the legacy {@code activate} order.
 */
public class CondenserBlock extends BaseEntityBlock {

    public static final MapCodec<CondenserBlock> CODEC = simpleCodec(CondenserBlock::new);

    /** 1.12.2 {@code BOTTOM}, inverted to the {@code top} convention the cabinet and advanced condenser use. */
    public static final BooleanProperty TOP = CondenserAdvancedBlock.TOP;
    /** Legacy {@code FACING}: which way the front panel looks and which axis the retorts flank. */
    public static final DirectionProperty FACING = RetortBlock.FACING;

    private static final VoxelShape BOTTOM_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final VoxelShape TOP_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);

    public CondenserBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TOP, false).setValue(FACING,
                net.minecraft.core.Direction.NORTH));
    }

    @Override
    public MapCodec<CondenserBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, FACING);
    }

    /** Place as a bottom half facing the placer, and only when there is headroom above - the legacy placement rule. */
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
        return state.getValue(TOP) ? null : new CondenserBlockEntity(pos, state);
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
        return createTickerHelper(type, RusticBlockEntities.CONDENSER.get(), CondenserBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        CondenserBlockEntity condenser = operatableBottom(level, pos, state);
        if (condenser == null) {
            // The still is not correctly flanked by its retorts (or the bottom half is not resolved yet). CONSUME the
            // interaction so the held item is not used: ServerPlayerGameMode.useItemOn only skips a held BlockItem's
            // own placement when the block returns a result whose consumesAction() is true (SUCCESS/CONSUME); PASS,
            // SKIP_DEFAULT and FAIL all fall through to stack.useOn and STILL place the block. CONSUME (no swing) is
            // therefore "do nothing until the column is shaped", matching the hasRetorts gate on the GUI/bucket paths.
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
        CondenserBlockEntity condenser = operatableBottom(level, pos, state);
        if (condenser == null) {
            return InteractionResult.PASS;
        }
        // Empty hand: sneak pulls a bucket of water back out; a plain click opens the GUI.
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
     * The bottom half's block entity when the column is correctly flanked by retorts, otherwise {@code null}. This is
     * the restored legacy {@code hasRetorts} gate: without its two outward-facing tier retorts a still will not open, so
     * both the GUI and the bucket interaction require a properly shaped multi-block. Resolves a top-half click down to
     * the inventory-owning bottom before checking, then evaluates the flank at that bottom position.
     */
    @Nullable
    private CondenserBlockEntity operatableBottom(Level level, BlockPos pos, BlockState state) {
        BlockPos bottom = state.getValue(TOP) ? pos.below() : pos;
        if (!(level.getBlockEntity(bottom) instanceof CondenserBlockEntity condenser)) {
            return null;
        }
        BlockState bottomState = level.getBlockState(bottom);
        return CondenserFlanking.hasRetorts(level, bottom, bottomState, RusticMachineBlocks.RETORT.get())
                ? condenser : null;
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
        // Eject the item slots when the block actually leaves the world; the water tank is lost, as legacy breakBlock did.
        if (!state.is(newState.getBlock()) && !state.getValue(TOP)
                && level.getBlockEntity(pos) instanceof CondenserBlockEntity condenser) {
            net.neoforged.neoforge.items.ItemStackHandler handler = condenser.getItemHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, handler.getStackInSlot(slot));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
