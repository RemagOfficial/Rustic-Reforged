package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import rustic.content.blockentity.EvaporatingBasinBlockEntity;
import rustic.content.blockentity.RusticBlockEntities;

/**
 * 1.12.2's {@code BlockEvaporatingBasin}: the fill-in / dust-out machine behind {@link EvaporatingBasinBlockEntity}.
 * No GUI. Right-clicking with a bucket (or any fluid container) pours it in or draws it out; an empty-hand click
 * collects the crystallised dust, and sneak-clicking with an otherwise-empty hand pulls a bucket's worth of fluid
 * back out. The evaporation itself runs on the server tick.
 */
public class EvaporatingBasinBlock extends BaseEntityBlock {

    public static final MapCodec<EvaporatingBasinBlock> CODEC = simpleCodec(EvaporatingBasinBlock::new);

    // 1.12.2's BlockEvaporatingBasin: a shallow full-footprint basin.
    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);

    public EvaporatingBasinBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<EvaporatingBasinBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EvaporatingBasinBlockEntity(pos, state);
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
                : createTickerHelper(type, RusticBlockEntities.EVAPORATING_BASIN.get(),
                        EvaporatingBasinBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof EvaporatingBasinBlockEntity basin) {
            // Pour a held container into the tank, or draw the tank into an empty one.
            if (FluidUtil.interactWithFluidHandler(player, hand, basin.getTank())) {
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!(level.getBlockEntity(pos) instanceof EvaporatingBasinBlockEntity basin)) {
            return InteractionResult.PASS;
        }
        // Plain empty-hand click collects the dust output; sneak + empty hand pulls a bucket of fluid back out.
        if (!player.isSecondaryUseActive()) {
            ItemStack stored = basin.getItemHandler().getStackInSlot(EvaporatingBasinBlockEntity.OUTPUT_SLOT);
            if (!stored.isEmpty()) {
                ItemStack extracted = basin.getItemHandler()
                        .extractItem(EvaporatingBasinBlockEntity.OUTPUT_SLOT, stored.getCount(), false);
                if (!player.getAbilities().instabuild) {
                    ItemHandlerHelper.giveItemToPlayer(player, extracted);
                }
                return InteractionResult.sidedSuccess(false);
            }
            return InteractionResult.PASS;
        }
        FluidStack simulated = basin.getTank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
        if (!simulated.isEmpty() && !FluidUtil.getFilledBucket(simulated).isEmpty()) {
            ItemStack bucket = FluidUtil.getFilledBucket(simulated);
            basin.getTank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
            if (!player.getAbilities().instabuild) {
                ItemHandlerHelper.giveItemToPlayer(player, bucket);
            }
            return InteractionResult.sidedSuccess(false);
        }
        return InteractionResult.PASS;
    }
}
