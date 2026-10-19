package rustic.content.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import rustic.content.blockentity.BrewingBarrelBlockEntity;
import rustic.content.fluids.RusticBooze;

public class BrewingBarrelMenu extends AbstractContainerMenu {

    private final BrewingBarrelBlockEntity tile;
    private final IItemHandler itemHandler;

    public BrewingBarrelMenu(int containerId, Inventory playerInventory, @Nullable BrewingBarrelBlockEntity tile) {
        super(RusticMenus.BREWING_BARREL.get(), containerId);
        this.tile = tile;
        this.itemHandler = tile != null ? tile.getItemHandler() : new ItemStackHandler(6);
        addMachineSlots();
        addPlayerSlots(playerInventory);

        if (tile != null) {
            addDataSlots(tile.getContainerData());
        }
    }

    public static BrewingBarrelMenu client(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BrewingBarrelBlockEntity tile = playerInventory.player.level().getBlockEntity(pos)
                instanceof BrewingBarrelBlockEntity be ? be : null;
        return new BrewingBarrelMenu(containerId, playerInventory, tile);
    }

    private void addMachineSlots() {

        addSlot(new InputSlot(itemHandler, BrewingBarrelBlockEntity.INPUT_IN_SLOT, 62, 7, SlotKind.CONTAINER));
        addSlot(new InputSlot(itemHandler, BrewingBarrelBlockEntity.OUTPUT_IN_SLOT, 116, 7, SlotKind.BOTTLE));
        addSlot(new InputSlot(itemHandler, BrewingBarrelBlockEntity.AUX_IN_SLOT, 26, 15, SlotKind.CULTURE));

        addSlot(new OutputSlot(itemHandler, BrewingBarrelBlockEntity.INPUT_OUT_SLOT, 62, 63));
        addSlot(new OutputSlot(itemHandler, BrewingBarrelBlockEntity.OUTPUT_OUT_SLOT, 116, 63));
        addSlot(new OutputSlot(itemHandler, BrewingBarrelBlockEntity.AUX_OUT_SLOT, 26, 55));
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
            if (index < 6) {

                if (!moveItemStackTo(stack, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 6, false)) {
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
    public BrewingBarrelBlockEntity getTile() {
        return this.tile;
    }

    public boolean isMachineSlotEmpty(int slotIndex) {
        return this.itemHandler.getStackInSlot(slotIndex).isEmpty();
    }

    private enum SlotKind { CONTAINER, BOTTLE, CULTURE }

    private static final class InputSlot extends SlotItemHandler {
        private final SlotKind kind;

        InputSlot(IItemHandler handler, int index, int x, int y, SlotKind kind) {
            super(handler, index, x, y);
            this.kind = kind;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.isEmpty() || !super.mayPlace(stack)) {
                return false;
            }
            return switch (kind) {
                case BOTTLE -> stack.is(Items.GLASS_BOTTLE);
                case CONTAINER -> isFluidContainer(stack) || stack.is(Items.GLASS_BOTTLE);
                case CULTURE -> stack.is(Items.GLASS_BOTTLE) || holdsBooze(stack);
            };
        }
    }

    private static final class OutputSlot extends SlotItemHandler {
        OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return FluidUtil.getFluidHandler(stack).isPresent();
    }

    private static boolean holdsBooze(ItemStack stack) {
        FluidStack contained = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        return contained.getFluid() instanceof RusticBooze;
    }
}
