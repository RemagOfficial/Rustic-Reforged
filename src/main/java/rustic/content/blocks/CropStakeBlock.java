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

public class CropStakeBlock extends Block {

    public static final MapCodec<CropStakeBlock> CODEC = simpleCodec(
            p -> new CropStakeBlock(p, () -> RusticFarmBlocks.STAKE_TIED.get()));

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
