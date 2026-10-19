package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;
import rustic.content.blockentity.CrushingTubBlockEntity;

public class CrushingTubBlock extends BaseEntityBlock {

    public static final MapCodec<CrushingTubBlock> CODEC = simpleCodec(CrushingTubBlock::new);

    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5625D, 1.0D);

    public CrushingTubBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CrushingTubBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrushingTubBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance);
        if (!level.isClientSide && entity instanceof LivingEntity
                && level.getBlockEntity(pos) instanceof CrushingTubBlockEntity tub) {
            tub.crush();
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof CrushingTubBlockEntity tub)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {

            if (FluidUtil.interactWithFluidHandler(player, hand, tub.getDrainView())) {
                return ItemInteractionResult.SUCCESS;
            }

            ItemStack remainder = tub.getItemHandler()
                    .insertItem(CrushingTubBlockEntity.INPUT_SLOT, held, false);
            player.setItemInHand(hand, remainder);
            if (remainder.getCount() != held.getCount()) {
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
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof CrushingTubBlockEntity tub) {
            ItemStack stored = tub.getItemHandler().getStackInSlot(CrushingTubBlockEntity.INPUT_SLOT);
            if (!stored.isEmpty()) {
                ItemStack extracted = tub.getItemHandler()
                        .extractItem(CrushingTubBlockEntity.INPUT_SLOT, stored.getCount(), false);
                if (!player.getAbilities().instabuild) {
                    ItemHandlerHelper.giveItemToPlayer(player, extracted);
                }
                return InteractionResult.sidedSuccess(false);
            }
        }
        return InteractionResult.PASS;
    }
}
