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
import rustic.content.blockentity.CondenserBlockEntity;
import rustic.content.blockentity.CondenserBlockEntityBase;

public class CondenserMenu extends AbstractContainerMenu {

    private final CondenserBlockEntityBase tile;
    private final IItemHandler itemHandler;

    public CondenserMenu(int containerId, Inventory playerInventory, @Nullable CondenserBlockEntityBase tile) {
        super(RusticMenus.CONDENSER.get(), containerId);
        this.tile = tile;
        this.itemHandler = tile != null ? tile.getItemHandler() : new ItemStackHandler(CondenserBlockEntity.SLOT_NUM);
        addMachineSlots();
        addPlayerSlots(playerInventory);
        if (tile != null) {
            addDataSlots(tile.getContainerData());
        }
    }

    public static CondenserMenu client(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        CondenserBlockEntityBase tile = playerInventory.player.level().getBlockEntity(pos)
                instanceof CondenserBlockEntityBase be ? be : null;
        return new CondenserMenu(containerId, playerInventory, tile);
    }

    private void addMachineSlots() {

        addSlot(new ResultSlot(itemHandler, CondenserBlockEntityBase.SLOT_RESULT, 105, 35));
        addSlot(new FuelSlot(itemHandler, CondenserBlockEntityBase.SLOT_FUEL, 66, 62));
        addSlot(new BottleSlot(itemHandler, CondenserBlockEntityBase.SLOT_BOTTLE, 105, 7));
        addSlot(new SlotItemHandler(itemHandler, CondenserBlockEntityBase.SLOT_INGREDIENTS_START, 27, 23));
        addSlot(new SlotItemHandler(itemHandler, CondenserBlockEntityBase.SLOT_INGREDIENTS_START + 1, 27, 47));
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
            if (index < CondenserBlockEntity.SLOT_NUM) {
                if (!moveItemStackTo(stack, CondenserBlockEntity.SLOT_NUM, CondenserBlockEntity.SLOT_NUM + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, CondenserBlockEntity.SLOT_NUM, false)) {
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
        return true;
    }

    @Nullable
    public CondenserBlockEntityBase getTile() {
        return this.tile;
    }

    static final class ResultSlot extends SlotItemHandler {
        ResultSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    static final class FuelSlot extends SlotItemHandler {
        FuelSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return CondenserBlockEntityBase.isFuel(stack);
        }
    }

    static final class BottleSlot extends SlotItemHandler {
        BottleSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !CondenserBlockEntityBase.isFuel(stack);
        }
    }
}
