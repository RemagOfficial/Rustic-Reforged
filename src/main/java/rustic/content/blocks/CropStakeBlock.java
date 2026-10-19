package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rustic.content.items.StakeCropSeedItem;

import java.util.function.Supplier;

/**
 * 1.12.2's {@code BlockCropStake}: the bare wooden pole a tomato or chili vine is planted onto, and the
 * block a vine reverts to when it is harvested or loses its support. It is the interactive half of the
 * stake-crop trio - {@link #useItemOn} turns the pole into a crop when a {@link StakeCropSeedItem} is
 * clicked on it, or into a {@link StakeTiedBlock} when rope is.
 *
 * <p>Everything else is the legacy pole read straight across: a non-solid wood of hardness 2 and the
 * resistant explosion value (so {@link RusticBlockProperties#stakePole(float)}, {@code woodResistant}
 * with {@code forceSolidOff} - the {@code BlockState#isSolid} answer 1.21's farmland revert reads, kept
 * off so the pole does not destroy the soil it stands on), a selection box the
 * width of the stake and a narrower collision box (the two AABBs below). The legacy
 * {@code canPlaceTorchOnTop} override needs no counterpart: a {@code noOcclusion} pole still reports a
 * solid, flat top face for the torch check, so torches already sit on it.
 *
 * <p>The tied-stake reference is a {@link Supplier} because {@code STAKE_TIED} is registered alongside this
 * block and is not bound yet while the pole's own factory runs; it is only read when a player ties rope.
 */
public class CropStakeBlock extends Block {

    public static final MapCodec<CropStakeBlock> CODEC = simpleCodec(
            p -> new CropStakeBlock(p, () -> RusticFarmBlocks.STAKE_TIED.get()));

    // 1.12.2 STAKE_SELECTION_AABB 0.375..0.625 x/z full height; STAKE_AABB collision 0.4375..0.5625.
    private static final VoxelShape STAKE_SHAPE = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    private static final VoxelShape STAKE_COLLISION = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);

    private final Supplier<? extends Block> stakeTied;

    public CropStakeBlock(BlockBehaviour.Properties properties, Supplier<? extends Block> stakeTied) {
        super(properties);
        this.stakeTied = stakeTied;
    }

    @Override
    public MapCodec<CropStakeBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return STAKE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return STAKE_COLLISION;
    }

    /**
     * 1.12.2's {@code onBlockActivated}. Clicking the pole with a stake-crop seed raises a vine here, but
     * only if the ground under the pole will actually take it - the legacy {@code canSustainPlant} test, which
     * is {@link StakeCropBlock#canBePlanted} (the soil rule alone, with no light term, exactly as the legacy
     * activation checked it). Clicking it with rope knots the pole into a {@link StakeTiedBlock}. Both consume
     * the held item outside creative, exactly as the legacy {@code shrink(1)} guarded on {@code isCreativeMode}
     * did.
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
        if (stack.getItem() instanceof StakeCropSeedItem seedItem) {
            if (seedItem.getCrop().canBePlanted(level, pos)) {
                BlockState cropState = seedItem.getCropState();
                if (!level.isClientSide) {
                    level.setBlock(pos, cropState, Block.UPDATE_ALL);
                    RusticDrops.playPlaceSound(level, pos, cropState);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(RusticBlocks.ROPE.get().asItem()) && stakeTied.get() != null) {
            if (!level.isClientSide) {
                level.setBlock(pos, stakeTied.get().defaultBlockState(), Block.UPDATE_ALL);
                RusticDrops.playPlaceSound(level, pos, state);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
