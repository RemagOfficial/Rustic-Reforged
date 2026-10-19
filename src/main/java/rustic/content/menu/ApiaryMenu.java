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

/**
 * The apiary's container, the 1.21 form of legacy {@code ContainerApiary}. Two machine slots over the
 * {@code apiary.png} panel - the bee slot at (80, 26), the honeycomb slot at (80, 44) - each bound to its own
 * one-slot handler on {@link ApiaryBlockEntity} and guarded to its one item, exactly like the legacy
 * anonymous {@code SlotItemHandler} subclasses. No {@code DataSlot}s: the legacy GUI drew no progress bars,
 * only the two titles, so the slot contents are the whole sync.
 *
 * <p>Shift-click routes bees to the bee slot and honeycomb to the comb slot; everything else falls through
 * to the ordinary player-inventory shuffle, mirroring legacy {@code transferStackInSlot}.
 */
public class ApiaryMenu extends AbstractContainerMenu {

    /** Legacy {@code ContainerApiary} machine-slot count: bee + honeycomb. */
    public static final int MACHINE_SLOTS = 2;

    private final ApiaryBlockEntity tile;
    private final IItemHandler beeHandler;
    private final IItemHandler honeyCombHandler;

    public ApiaryMenu(int containerId, Inventory playerInventory, @Nullable ApiaryBlockEntity tile) {
        super(RusticMenus.APIARY.get(), containerId);
        this.tile = tile;
        // The client copy is rebuilt from the position the server wrote, so by the time the packet is
        // resolved the block entity exists; the fallbacks keep a null (mid-unload) menu slot-functional.
        this.beeHandler = tile != null ? tile.getBeeHandler() : new ItemStackHandler(1);
        this.honeyCombHandler = tile != null ? tile.getHoneyCombHandler() : new ItemStackHandler(1);
        addMachineSlots();
        addPlayerSlots(playerInventory);
    }

    /** Rebuilds the client menu, resolving the world block entity from the position the server wrote. */
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
                // Machine -> player: anything that will not fit anywhere in the 36 slots stays put.
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
                // Main inventory rows -> hotbar, the legacy (2..29 -> 29..38) branch.
                if (!moveItemStackTo(stack, MACHINE_SLOTS + 27, MACHINE_SLOTS + 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 27, false)) {
                // Hotbar -> main inventory, the legacy (29..38 -> 2..29) branch.
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
        // Legacy canInteractWith: the player must stay within 8 blocks (squared distance 64) of the box.
        return tile == null || player.distanceToSqr(tile.getBlockPos().getCenter()) <= 64.0D;
    }

    /** The bound apiary (server copy, or the synced client copy). */
    @Nullable
    public ApiaryBlockEntity getTile() {
        return this.tile;
    }

    // ------------------------------------------------------------------ slot kinds

    /** Only bee items - the legacy anonymous {@code isItemValid} guard on slot 0. */
    static final class BeeSlot extends SlotItemHandler {
        BeeSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(RusticItems.BEE.get());
        }
    }

    /** Only honeycomb items - the same guard on the bottom handler's slot. */
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
