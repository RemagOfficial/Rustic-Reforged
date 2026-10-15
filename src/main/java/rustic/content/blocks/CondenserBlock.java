package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
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
 * 1.12.2's {@code BlockCondenser}, rebuilt as a single functional block behind {@link CondenserBlockEntity}. The
 * legacy machine was a two-block column whose GUI only opened when flanked RETORTs were present; the plan's M5
 * contract treats the basic condenser as a one-block functional shell with no retort dependency (retorts are not this
 * phase's machines), so the {@code BOTTOM} pairing and {@code hasRetorts} gate both drop away, and the horizontal
 * {@code FACING} property - which only drove the retort check and the old smoke puffs - goes with them.
 *
 * <p>Right-clicking pours a held bucket/container into the water tank or draws one back (the legacy {@code activate}
 * fluid branch), and any other click opens the condenser GUI where fuel, bottle and ingredients are loaded.
 */
public class CondenserBlock extends BaseEntityBlock {

    public static final MapCodec<CondenserBlock> CODEC = simpleCodec(CondenserBlock::new);

    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    public CondenserBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CondenserBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CondenserBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, RusticBlockEntities.CONDENSER.get(), CondenserBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof CondenserBlockEntity condenser) {
            // A held fluid container pours in / draws out first; anything else opens the GUI (legacy activate order).
            if (FluidUtil.interactWithFluidHandler(player, hand, condenser.getTank())) {
                return ItemInteractionResult.SUCCESS;
            }
            openGui(player, level, pos);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!(level.getBlockEntity(pos) instanceof CondenserBlockEntity condenser)) {
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
        openGui(player, level, pos);
        return InteractionResult.CONSUME;
    }

    private void openGui(Player player, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        // Eject the item slots when the block actually leaves the world; the water tank is lost, as legacy breakBlock did.
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CondenserBlockEntity condenser) {
            net.neoforged.neoforge.items.ItemStackHandler handler = condenser.getItemHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, handler.getStackInSlot(slot));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
