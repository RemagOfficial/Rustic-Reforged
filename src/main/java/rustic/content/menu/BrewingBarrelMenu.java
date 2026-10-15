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

/**
 * The brewing-barrel container, the 1.21 form of legacy {@code ContainerBrewingBarrel}. It is deliberately bespoke
 * (not a {@code ChestMenu}): the barrel mixes six machine slots with furnace-style integer synchronisation, so it
 * extends {@link AbstractContainerMenu} directly, lays the six {@link SlotItemHandler}s over the block entity's
 * {@link ItemStackHandler}, and hangs the brew read-outs off the block entity's own
 * {@link net.minecraft.world.inventory.ContainerData} via {@link #addDataSlots}.
 *
 * <p><b>Slot roles.</b> The three top slots are inputs (a fluid container into the input tank, a glass bottle under
 * the output, a booze culture into the auxiliary tank); the three bottom slots are outputs, take-only. Placement
 * validity is enforced here on {@link Slot#mayPlace} rather than inside the {@code ItemStackHandler}, so the machine's
 * own {@code tryTankIO} inserts into the output slots are not blocked by the player-facing restrictions - exactly the
 * legacy internal-vs-external handler split, collapsed onto one handler with slot-level guards.
 *
 * <p><b>Client copy.</b> The client instance is rebuilt by the menu type factory (see {@link RusticMenus#BREWING_BARREL})
 * from the block position the server wrote, so it binds to the <em>client</em> block entity copy whose tanks the
 * screen renders. When that copy is somehow absent the menu still builds over a detached empty handler, so opening
 * never crashes.
 */
public class BrewingBarrelMenu extends AbstractContainerMenu {

    private final BrewingBarrelBlockEntity tile;
    private final IItemHandler itemHandler;

    public BrewingBarrelMenu(int containerId, Inventory playerInventory, @Nullable BrewingBarrelBlockEntity tile) {
        super(RusticMenus.BREWING_BARREL.get(), containerId);
        this.tile = tile;
        this.itemHandler = tile != null ? tile.getItemHandler() : new ItemStackHandler(6);
        addMachineSlots();
        addPlayerSlots(playerInventory);
        // The block entity is itself the ContainerData; on the client its set() feeds the fields the screen reads.
        if (tile != null) {
            addDataSlots(tile.getContainerData());
        }
    }

    /** Rebuilds the client menu, resolving the world block entity from the position the server wrote. */
    public static BrewingBarrelMenu client(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BrewingBarrelBlockEntity tile = playerInventory.player.level().getBlockEntity(pos)
                instanceof BrewingBarrelBlockEntity be ? be : null;
        return new BrewingBarrelMenu(containerId, playerInventory, tile);
    }

    private void addMachineSlots() {
        // Input slots: fluid container (0), glass bottle (1), booze culture (2).
        addSlot(new InputSlot(itemHandler, BrewingBarrelBlockEntity.INPUT_IN_SLOT, 62, 7, SlotKind.CONTAINER));
        addSlot(new InputSlot(itemHandler, BrewingBarrelBlockEntity.OUTPUT_IN_SLOT, 116, 7, SlotKind.BOTTLE));
        addSlot(new InputSlot(itemHandler, BrewingBarrelBlockEntity.AUX_IN_SLOT, 26, 15, SlotKind.CULTURE));
        // Output slots: take-only.
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
                // Machine slot -> player inventory.
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

    /** The barrel entity this menu is bound to (server copy, or the synced client copy the screen renders). */
    @Nullable
    public BrewingBarrelBlockEntity getTile() {
        return this.tile;
    }

    /** Whether the bound slot currently holds nothing, driving the GUI's empty-slot overlays. */
    public boolean isMachineSlotEmpty(int slotIndex) {
        return this.itemHandler.getStackInSlot(slotIndex).isEmpty();
    }

    // ------------------------------------------------------------------ slot kinds

    private enum SlotKind { CONTAINER, BOTTLE, CULTURE }

    /** An input slot whose {@code mayPlace} mirrors legacy {@code ContainerBrewingBarrel}'s per-slot validity tests. */
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

    /** A machine output slot: the player can always take, never place. */
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
