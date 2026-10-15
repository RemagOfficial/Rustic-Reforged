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
import rustic.content.blockentity.LiquidBarrelBlockEntity;

/**
 * 1.12.2's {@code BlockLiquidBarrel}: a fluid-tank barrel with no GUI. The whole block is an interaction surface
 * over the {@link LiquidBarrelBlockEntity}'s tank - right-clicking with a bucket (or any {@code IFluidHandlerItem})
 * fills or empties it through {@link FluidUtil}, and sneak-clicking with an otherwise-empty hand drains one bucket
 * worth into the player. Unlike {@link BlockRusticChest} it never opens a {@code MenuProvider} and never ejects item
 * contents, so it extends {@link BaseEntityBlock} directly.
 *
 * <p>The legacy {@code getDrops}/{@code breakBlock} that moved the tank fluid into the dropped item on break is not
 * reproduced here (out of scope for this phase's functional shell): the block drops itself empty via the standard
 * loot table, matching the plan's M1 contract of a persistent-but-not-item-carried tank.
 */
public class LiquidBarrelBlock extends BaseEntityBlock {

    public static final MapCodec<LiquidBarrelBlock> CODEC = simpleCodec(LiquidBarrelBlock::new);

    // 1.12.2's BlockLiquidBarrel AABB: 0.125..0.875 horizontally, full height.
    private static final VoxelShape SHAPE = Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);

    public LiquidBarrelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<LiquidBarrelBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LiquidBarrelBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Bucket / fluid-container fill and drain, both directions, delegated to NeoForge's interaction helper. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            // Let the client swing; the server is the only side that actually mutates the tank.
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Sneak with a hand that is not a fluid container to pull one bucket worth of fluid out, if it can be bottled. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof LiquidBarrelBlockEntity be) {
            FluidStack simulated = be.getTank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
            if (!simulated.isEmpty()) {
                ItemStack bucket = FluidUtil.getFilledBucket(simulated);
                if (!bucket.isEmpty()) {
                    be.getTank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
                    if (!player.getAbilities().instabuild) {
                        ItemHandlerHelper.giveItemToPlayer(player, bucket);
                    }
                    return InteractionResult.sidedSuccess(false);
                }
            }
        }
        return InteractionResult.PASS;
    }
}
