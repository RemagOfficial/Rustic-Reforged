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
import rustic.content.blockentity.CondenserAdvancedBlockEntity;
import rustic.content.blockentity.CondenserBlockEntityBase;

/**
 * The advanced condenser's container, the 1.21 form of legacy {@code ContainerCondenserAdvanced}. Seven machine slots
 * (result / fuel / bottle / modifier / three ingredients) over the {@code condenser_advanced.png} panel, plus the same
 * four furnace {@link net.minecraft.world.inventory.DataSlot}s. It reuses the {@link CondenserMenu.ResultSlot result
 * / fuel / bottle} guards from the basic menu and adds the modifier slot (free placement - the recipe's modifier gate
 * does the validation at match time).
 */
public class CondenserAdvancedMenu extends AbstractContainerMenu {

    private final CondenserBlockEntityBase tile;
    private final IItemHandler itemHandler;

    public CondenserAdvancedMenu(int containerId, Inventory playerInventory, @Nullable CondenserBlockEntityBase tile) {
        super(RusticMenus.CONDENSER_ADVANCED.get(), containerId);
        this.tile = tile;
        this.itemHandler = tile != null ? tile.getItemHandler()
                : new ItemStackHandler(CondenserAdvancedBlockEntity.SLOT_NUM);
        addMachineSlots();
        addPlayerSlots(playerInventory);
        if (tile != null) {
            addDataSlots(tile.getContainerData());
        }
    }

    /** Rebuilds the client menu, resolving the world block entity from the position the server wrote. */
    public static CondenserAdvancedMenu client(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        CondenserBlockEntityBase tile = playerInventory.player.level().getBlockEntity(pos)
                instanceof CondenserBlockEntityBase be ? be : null;
        return new CondenserAdvancedMenu(containerId, playerInventory, tile);
    }

    private void addMachineSlots() {
        // Legacy ContainerCondenserAdvanced coordinates: modifier at (66,7), the three ingredients down the left column.
        addSlot(new CondenserMenu.ResultSlot(itemHandler, CondenserBlockEntityBase.SLOT_RESULT, 105, 35));
        addSlot(new CondenserMenu.FuelSlot(itemHandler, CondenserBlockEntityBase.SLOT_FUEL, 66, 62));
        addSlot(new CondenserMenu.BottleSlot(itemHandler, CondenserBlockEntityBase.SLOT_BOTTLE, 105, 7));
        addSlot(new SlotItemHandler(itemHandler, CondenserAdvancedBlockEntity.SLOT_MODIFIER, 66, 7));
        addSlot(new SlotItemHandler(itemHandler, CondenserBlockEntityBase.SLOT_INGREDIENTS_START + 1, 27, 11));
        addSlot(new SlotItemHandler(itemHandler, CondenserBlockEntityBase.SLOT_INGREDIENTS_START + 2, 27, 35));
        addSlot(new SlotItemHandler(itemHandler, CondenserBlockEntityBase.SLOT_INGREDIENTS_START + 3, 27, 59));
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
            if (index < CondenserAdvancedBlockEntity.SLOT_NUM) {
                if (!moveItemStackTo(stack, CondenserAdvancedBlockEntity.SLOT_NUM,
                        CondenserAdvancedBlockEntity.SLOT_NUM + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, CondenserAdvancedBlockEntity.SLOT_NUM, false)) {
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

    /** The bound advanced condenser (server copy, or the synced client copy the screen renders). */
    @Nullable
    public CondenserBlockEntityBase getTile() {
        return this.tile;
    }
}
