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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rustic.content.blocks.CabinetBlock;
import rustic.content.menu.RusticMenus;

public class CabinetBlockEntity extends RandomizableContainerBlockEntity implements LidBlockEntity {

    private static final int EVENT_SET_OPEN_COUNT = 1;

    private static final int CONTAINER_SIZE = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    private final ChestLidController lidController = new ChestLidController();

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

            CabinetBlockEntity.this.signalOpenCount(level, pos, state, previous, count);
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

    private void playSound(Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, SoundSource.BLOCKS,
                0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    public static void lidAnimateTick(Level level, BlockPos pos, BlockState state, CabinetBlockEntity blockEntity) {
        blockEntity.lidController.tickLid();
    }

    @Override
    public float getOpenNess(float partialTicks) {
        return this.lidController.getOpenness(partialTicks);
    }

    @Override
    public boolean triggerEvent(int id, int count) {
        if (id == EVENT_SET_OPEN_COUNT) {
            this.lidController.shouldBeOpen(count > 0);
            return true;
        }
        return super.triggerEvent(id, count);
    }

    protected void signalOpenCount(Level level, BlockPos pos, BlockState state, int previous, int count) {
        Block block = state.getBlock();
        level.blockEvent(pos, block, EVENT_SET_OPEN_COUNT, count);
    }
}
