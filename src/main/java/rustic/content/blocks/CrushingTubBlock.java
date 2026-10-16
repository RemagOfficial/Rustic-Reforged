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

/**
 * 1.12.2's {@code BlockCrushingTub}: the input/tank machine behind {@link CrushingTubBlockEntity}. No GUI - the block
 * is an interaction surface. Right-click inserts an input item or draws fluid out with a bucket, sneak-right-click
 * pulls the input item back out, and - exactly as the legacy {@code onFallenUpon} trigger - a living entity landing
 * on the tub (a player hopping onto it) runs one {@link CrushingTubBlockEntity#crush()} cycle.
 */
public class CrushingTubBlock extends BaseEntityBlock {

    public static final MapCodec<CrushingTubBlock> CODEC = simpleCodec(CrushingTubBlock::new);

    // 1.12.2's BlockCrushingTub TUB_AABB: full footprint, just over half height.
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
        // S9.1: the crushing tub's fluid surface and stacked items are a BlockEntityRenderer, which only runs under
        // ENTITYBLOCK_ANIMATED; the renderer also redraws this block's own open-vessel model via renderSingleBlock.
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * The legacy {@code BlockCrushingTub.onFallenUpon} hook (1.21.1 renamed it {@code fallOn}): a living entity that
     * lands on the tub after a fall (a player hopping on, a mob dropping onto it) drives one crush cycle. {@code fallOn}
     * only fires when the entity arrives with a nonzero fall distance, so simply walking onto the half-height tub does
     * not crush - the player has to jump on it, matching 1.12.2.
     */
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
            // Prefer drawing fluid out into a bucket/container; the tank is drain-only so nothing pours in.
            if (FluidUtil.interactWithFluidHandler(player, hand, tub.getDrainView())) {
                return ItemInteractionResult.SUCCESS;
            }
            // Otherwise treat the held item as a crush input.
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
