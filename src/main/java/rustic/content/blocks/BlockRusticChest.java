package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The base shared by every Rustic block that is just a chest-like inventory: the legacy
 * {@code TileEntityBarrel}, {@code TileEntityVase} and {@code TileEntityCabinet} all wrapped a plain
 * lockable inventory of the same shape, so all four behaviour points are identical.
 *
 * <p>The legacy blocks each re-declared {@code hasTileEntity}/{@code createTileEntity}/{@code onBlockActivated}
 * /{@code breakBlock}. 1.21 moves the same four onto {@link BaseEntityBlock} and its {@code BlockEntity}
 * contract: open the menu in {@code useWithoutItem} rather than a GUI id, drop the contents in
 * {@code onRemove} rather than a hand-rolled {@code breakBlock}, and report the fill level to a
 * comparator the same way vanilla's own containers do.
 *
 * <p>{@link #getRenderShape(BlockState)} is the forced deviation to read: {@code BaseEntityBlock}'s
 * default is {@link RenderShape#ENTITYBLOCK_ANIMATED}, but the barrel, vase and cabinet of this slice
 * are ordinary block models, so it is pinned back to {@link RenderShape#MODEL}. A block that does grow a
 * {@code BlockEntityRenderer} (the cabinet lid animation is Phase 9's) overrides it again.
 */
public abstract class BlockRusticChest extends BaseEntityBlock {

    protected BlockRusticChest(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        // The client only ever returns a success/consume result to stop the hand swinging; the menu is
        // opened on the server, which then tells the client what to draw through the MenuType.
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        // A block becomes a different block: eject the inventory first, exactly as the legacy breakBlock
        // did, and exactly as vanilla's chest/barrel/hopper do.
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);
}
