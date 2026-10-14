package rustic.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rustic.content.menu.RusticMenus;

/**
 * 1.12.2's {@code TileEntityVase}: a plain 27-slot storage inventory behind the rustic vase block, the same
 * shape as {@link BarrelBlockEntity} and the legacy {@code TileEntityCabinet}.
 *
 * <p>As with the barrel, the legacy class was {@code TileEntityLockableLoot} over an
 * {@code ItemStackHandlerRustic(27)} with a {@code ContainerVase}/{@code GuiVase} drawing the standard
 * {@code generic_27.png} - i.e. vanilla's {@link RandomizableContainerBlockEntity} contract verbatim - so it
 * is reused rather than re-implemented, and the hand-written {@code writeToNBT}/{@code readFromNBT} pair
 * collapses to {@link #saveAdditional}/{@link #loadAdditional} over {@link ContainerHelper}.
 *
 * <p>The legacy Quark {@code IDropoffManager} hook and the custom-name handling need no port here: the former
 * is compat work (Phase 10), the latter is inherited from
 * {@link net.minecraft.world.level.block.entity.BaseContainerBlockEntity}, which already reads
 * {@code CustomName}. There is no open/close sound because {@code TileEntityVase} never had one (unlike the
 * cabinet's lid); the vase simply opens.
 */
public class VaseBlockEntity extends RandomizableContainerBlockEntity {

    /** The 27 slots the legacy {@code ItemStackHandlerRustic(27)} and {@code GuiVase}'s three rows used. */
    private static final int CONTAINER_SIZE = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public VaseBlockEntity(BlockPos pos, BlockState state) {
        super(RusticBlockEntities.VASE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.items, registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.items, registries);
        }
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rustic.vase");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        // The vase's own MenuType, not ChestMenu.threeRows (which would report MenuType.GENERIC_9x3 and open
        // vanilla's chest screen). Passing `this` keeps the real inventory behind the server-side slots.
        return new ChestMenu(RusticMenus.VASE.get(), containerId, playerInventory, this, CONTAINER_SIZE / 9);
    }
}
