package rustic.content.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import rustic.content.blockentity.ApiaryBlockEntity;
import rustic.content.items.RusticItems;

public class ApiaryMenu extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 2;

    private final ApiaryBlockEntity tile;
    private final IItemHandler beeHandler;
    private final IItemHandler honeyCombHandler;

    public ApiaryMenu(int containerId, Inventory playerInventory, @Nullable ApiaryBlockEntity tile) {
        super(RusticMenus.APIARY.get(), containerId);
        this.tile = tile;

        this.beeHandler = tile != null ? tile.getBeeHandler() : new ItemStackHandler(1);
        this.honeyCombHandler = tile != null ? tile.getHoneyCombHandler() : new ItemStackHandler(1);
        addMachineSlots();
        addPlayerSlots(playerInventory);
    }

    public static ApiaryMenu client(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        ApiaryBlockEntity tile = playerInventory.player.level().getBlockEntity(pos)
                instanceof ApiaryBlockEntity be ? be : null;
        return new ApiaryMenu(containerId, playerInventory, tile);
    }

    private void addMachineSlots() {
        addSlot(new BeeSlot(beeHandler, 0, 80, 26));
        addSlot(new HoneyCombSlot(honeyCombHandler, 0, 80, 44));
    }

    private void addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {

                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(RusticItems.BEE.get())) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(RusticItems.HONEYCOMB.get())) {
                if (!moveItemStackTo(stack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < MACHINE_SLOTS + 27) {

                if (!moveItemStackTo(stack, MACHINE_SLOTS + 27, MACHINE_SLOTS + 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 27, false)) {

                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {

        return tile == null || player.distanceToSqr(tile.getBlockPos().getCenter()) <= 64.0D;
    }

    @Nullable
    public ApiaryBlockEntity getTile() {
        return this.tile;
    }

    static final class BeeSlot extends SlotItemHandler {
        BeeSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(RusticItems.BEE.get());
        }
    }

    static final class HoneyCombSlot extends SlotItemHandler {
        HoneyCombSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(RusticItems.HONEYCOMB.get());
        }
    }
}
