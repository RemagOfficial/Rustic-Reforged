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
 * 1.12.2's {@code TileEntityBarrel}: a plain 27-slot storage inventory behind the rustic barrel block.
 *
 * <p>The legacy tile entity built its inventory out of {@code ItemStackHandlerRustic} and paired it with
 * {@code ContainerBarrel} and a {@code GuiBarrel} that drew the standard chest background. That is exactly
 * vanilla's own {@link RandomizableContainerBlockEntity} contract - a {@link NonNullList} of stacks with a
 * {@link ChestMenu} over it and optional loot-table support - so it is reused rather than re-implemented:
 * {@link #saveAdditional saveAdditional}/{@link #loadAdditional loadAdditional} delegate to
 * {@link ContainerHelper} the same way vanilla's barrel does, and the {@code writeNBT}/{@code readFromNBT}
 * pair the 1.12.2 class wrote by hand becomes two overrides with a {@link HolderLookup.Provider} argument.
 *
 * <p>The legacy "lockable" behaviour (a {@code checkIsLocked}/{@code lockKeyTag} test) was never given a
 * key by any Rustic code, so it is dropped - {@link #canOpen} inherits the always-open default from
 * {@link net.minecraft.world.level.block.entity.BaseContainerBlockEntity}.
 */
public class BarrelBlockEntity extends RandomizableContainerBlockEntity {

    /** The 27 slots the legacy {@code ItemStackHandlerRustic(27)} and {@code GuiBarrel}'s three rows used. */
    private static final int CONTAINER_SIZE = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public BarrelBlockEntity(BlockPos pos, BlockState state) {
        super(RusticBlockEntities.BARREL.get(), pos, state);
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
        return Component.translatable("container.rustic.barrel");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        // The menu is built with the mod's own barrel MenuType - not ChestMenu.threeRows, which would
        // report MenuType.GENERIC_9x3 and open vanilla's chest screen instead of the generic_27 one bound
        // to RusticMenus.BARREL. Passing `this` keeps the real inventory behind the server-side slots.
        return new ChestMenu(RusticMenus.BARREL.get(), containerId, playerInventory, this, CONTAINER_SIZE / 9);
    }
}
