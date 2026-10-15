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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import rustic.content.blockentity.BrewingBarrelBlockEntity;
import rustic.content.blockentity.RusticBlockEntities;

/**
 * 1.12.2's {@code BlockBrewingBarrel}: the fermenting machine behind {@link BrewingBarrelBlockEntity}. Right-clicking
 * it - with or without a held item - opens the brewing GUI (the legacy block always {@code openGui}ed on activation,
 * and the tank IO the GUI needs happens inside the container, so the world interaction never inserts items). The
 * brew itself runs on the server tick.
 *
 * <p>The legacy horizontal {@code FACING} property drove only the old GUI-less item placement; the barrel renders as
 * a symmetric block with no orientation-dependent texture, so it is dropped, matching the other GUI-less machine
 * blocks here.
 */
public class BrewingBarrelBlock extends BaseEntityBlock {

    public static final MapCodec<BrewingBarrelBlock> CODEC = simpleCodec(BrewingBarrelBlock::new);

    // The legacy full-block footprint; the barrel is a plain cube for interaction purposes.
    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    public BrewingBarrelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BrewingBarrelBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewingBarrelBlockEntity(pos, state);
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
                : createTickerHelper(type, RusticBlockEntities.BREWING_BARREL.get(), BrewingBarrelBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        openGui(player, level, pos);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
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
        // Eject the six item slots when the block changes; the fluid tanks are lost, as legacy's breakBlock did.
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BrewingBarrelBlockEntity barrel) {
            ItemStackHandler handler = barrel.getItemHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, handler.getStackInSlot(slot));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
