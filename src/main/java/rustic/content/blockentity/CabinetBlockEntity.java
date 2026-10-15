package rustic.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rustic.content.blocks.CabinetBlock;
import rustic.content.menu.RusticMenus;

/**
 * 1.12.2's {@code TileEntityCabinet}: a 27-slot inventory per cabinet that shows 54 slots when stacked into
 * a vertical double.
 *
 * <p>The inventory half is the same {@link RandomizableContainerBlockEntity} contract the barrel uses (see
 * {@link BarrelBlockEntity} for why the legacy hand-written {@code writeNBT}/{@code readFromNBT} pair becomes
 * a {@link ContainerHelper}-delegated override). What is genuinely cabinet-specific is the double: the
 * legacy code cached a {@code DoubleCabinetItemHandler} and had {@code createContainer} reach into the block
 * state to pick between {@code ContainerCabinet} and {@code ContainerCabinetDouble}. 1.21 already models a
 * two-block inventory as a {@link CompoundContainer} over two {@link Container}s - vanilla's own double chest
 * does exactly that - so {@link #createMenu} reads the {@link CabinetBlock#TOP} flag, finds the partner
 * block entity above or below, and hands vanilla's {@link ChestMenu} six rows over the pair.
 *
 * <p>The chest open/close sound is kept (the user asked for behaviour now, art later) and reproduced the way
 * 1.21's own chest and barrel do it - a {@link ContainerOpenersCounter} driven from {@link #startOpen} /
 * {@link #stopOpen}, whose {@code isOwnContainer} test understands a {@link CompoundContainer} so a shared
 * double counts its openers correctly. The lid-angle ticking of the legacy {@code update()} only existed to
 * feed the deferred door {@code CabinetRenderer}; with no door to animate the counter's open-count callback
 * is left empty, exactly as vanilla's {@code BarrelBlockEntity} leaves it, and no {@code BlockEntityTicker}
 * or {@code triggerEvent} is needed. The Quark {@code IDropoffManager} interface and the {@code material}
 * stack (which the legacy dynamic cabinet craft stamped) are both dropped: the port's shaped recipe carries no
 * material, and the Quark compat half belongs to Phase 10.
 */
public class CabinetBlockEntity extends RandomizableContainerBlockEntity {

    /** The 27 slots the legacy {@code ItemStackHandlerRustic(27)} used; a double pairs two of these. */
    private static final int CONTAINER_SIZE = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            CabinetBlockEntity.this.playSound(level, pos, SoundEvents.CHEST_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            CabinetBlockEntity.this.playSound(level, pos, SoundEvents.CHEST_CLOSE);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int count) {
            // The legacy lid-angle sync lived here; the door renderer that read it is deferred, so nothing
            // has to be signalled - matching vanilla's BarrelBlockEntity, whose override is empty too.
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            if (player.containerMenu instanceof ChestMenu chestMenu) {
                Container container = chestMenu.getContainer();
                return container == CabinetBlockEntity.this
                        || container instanceof CompoundContainer compound
                        && compound.contains(CabinetBlockEntity.this);
            }
            return false;
        }
    };

    public CabinetBlockEntity(BlockPos pos, BlockState state) {
        super(RusticBlockEntities.CABINET.get(), pos, state);
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
        return Component.translatable("container.rustic.cabinet");
    }

    /**
     * A single cabinet opens three rows over its own 27 slots; a stacked one opens six rows over the
     * {@link CompoundContainer} of both halves. The upper half is the block carrying {@link CabinetBlock#TOP}
     * (mirroring 1.12.2's {@code createContainer}, which reached for {@code pos.down()} when the clicked
     * block was the top and {@code pos.up()} when it was the bottom), and the six-row layout fills its top
     * three rows with the upper cabinet and its bottom three with the lower one - the same order the legacy
     * {@code ContainerCabinetDouble} laid {@code teTop} then {@code teBottom}.
     */
    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        Level level = this.getLevel();
        BlockPos pos = this.getBlockPos();
        if (level != null) {
            if (this.getBlockState().getValue(CabinetBlock.TOP)) {
                if (level.getBlockEntity(pos.below()) instanceof CabinetBlockEntity bottom) {
                    return new ChestMenu(RusticMenus.DOUBLE_CABINET.get(), containerId, playerInventory,
                            new CompoundContainer(this, bottom), 6);
                }
            } else if (level.getBlockEntity(pos.above()) instanceof CabinetBlockEntity top
                    && top.getBlockState().getValue(CabinetBlock.TOP)) {
                return new ChestMenu(RusticMenus.DOUBLE_CABINET.get(), containerId, playerInventory,
                        new CompoundContainer(top, this), 6);
            }
        }
        return new ChestMenu(RusticMenus.CABINET.get(), containerId, playerInventory, this, CONTAINER_SIZE / 9);
    }

    @Override
    public void startOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.incrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.decrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    /** The 1.12.2 chest-open/close sample, played at the block centre the way vanilla's own containers play it. */
    private void playSound(Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, SoundSource.BLOCKS,
                0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }
}
