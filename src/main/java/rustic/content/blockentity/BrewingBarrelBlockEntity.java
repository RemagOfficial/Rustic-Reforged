package rustic.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import rustic.config.RusticConfig;
import rustic.content.crafting.BrewingBarrelRecipe;
import rustic.content.crafting.FluidRecipeInput;
import rustic.content.fluids.RusticBooze;
import rustic.content.fluids.RusticDataComponents;
import rustic.content.items.RusticItems;
import rustic.content.menu.BrewingBarrelMenu;
import rustic.registry.RusticRegistries;

public class BrewingBarrelBlockEntity extends BlockEntity implements MenuProvider, ContainerData {

    public static final int INPUT_IN_SLOT = 0;
    public static final int OUTPUT_IN_SLOT = 1;
    public static final int AUX_IN_SLOT = 2;
    public static final int INPUT_OUT_SLOT = 3;
    public static final int OUTPUT_OUT_SLOT = 4;
    public static final int AUX_OUT_SLOT = 5;

    public static final int INPUT_CAPACITY = 8000;
    public static final int OUTPUT_CAPACITY = 8000;
    public static final int AUX_CAPACITY = 1000;

    private static final int DATA_BREW_TIME = 0;
    private static final int DATA_MAX_BREW_TIME = 1;
    private static final int DATA_INPUT_AMOUNT = 2;
    private static final int DATA_OUTPUT_AMOUNT = 3;
    private static final int DATA_AUX_AMOUNT = 4;
    private static final int DATA_COUNT = 5;

    private final ItemStackHandler itemHandler = new ItemStackHandler(6) {
        @Override
        protected void onContentsChanged(int slot) {
            sync();
        }
    };

    private final FluidTank input = newTank(INPUT_CAPACITY);
    private final FluidTank output = newTank(OUTPUT_CAPACITY);
    private final FluidTank auxiliary = newTank(AUX_CAPACITY);

    private int brewTime = 0;
    @Nullable
    private BrewingBarrelRecipe recipe = null;

    public BrewingBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(RusticBlockEntities.BREWING_BARREL.get(), pos, state);
    }

    private FluidTank newTank(int capacity) {
        return new FluidTank(capacity) {
            @Override
            protected void onContentsChanged() {
                sync();
            }
        };
    }

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public FluidTank getInputTank() {
        return this.input;
    }

    public FluidTank getOutputTank() {
        return this.output;
    }

    public FluidTank getAuxiliaryTank() {
        return this.auxiliary;
    }

    public int getBrewTime() {
        return this.brewTime;
    }

    public int getMaxBrewTime() {
        return RusticConfig.MAX_BREW_TIME.get();
    }

    public int getInputAmount() {
        return this.input.getFluidAmount();
    }

    public int getOutputAmount() {
        return this.output.getFluidAmount();
    }

    public int getAuxAmount() {
        return this.auxiliary.getFluidAmount();
    }

    public boolean isBrewing() {
        return this.brewTime > 0;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BrewingBarrelBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        boolean fluidChanged = be.tryTankIO(be.input, INPUT_IN_SLOT, INPUT_OUT_SLOT, true);
        fluidChanged |= be.tryTankIO(be.output, OUTPUT_IN_SLOT, OUTPUT_OUT_SLOT, false);
        fluidChanged |= be.tryTankIO(be.auxiliary, AUX_IN_SLOT, AUX_OUT_SLOT, true);
        if (fluidChanged) {
            be.brewTime = 0;
            if (be.recipe != null && !be.recipe.matches(new FluidRecipeInput(be.input.getFluid()), level)) {
                be.recipe = null;
            }
            be.sync();
        }

        if (be.recipe == null && be.input.getFluidAmount() > 0) {
            be.recipe = be.findRecipe();
        }
        if (be.recipe != null) {
            be.brewTime++;
            if (be.brewTime >= be.getMaxBrewTime()) {
                be.brew();
                be.brewTime = 0;
                be.recipe = null;
            }
        } else {
            be.brewTime = 0;
        }
    }

    private boolean tryTankIO(FluidTank tank, int inSlot, int outSlot, boolean allowFill) {
        ItemStack stack = itemHandler.getStackInSlot(inSlot);
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack container = stack.is(Items.GLASS_BOTTLE)
                ? new ItemStack(RusticItems.FLUID_BOTTLE.get())
                : stack.copyWithCount(1);

        if (allowFill) {
            IFluidHandlerItem filled = FluidUtil.getFluidHandler(container.copyWithCount(1)).orElse(null);
            if (filled != null && !filled.getFluidInTank(0).isEmpty()) {
                if (!FluidUtil.tryFluidTransfer(tank, filled, Integer.MAX_VALUE, false).isEmpty()) {
                    IFluidHandlerItem exec = FluidUtil.getFluidHandler(container.copyWithCount(1)).orElse(null);
                    if (exec != null && !FluidUtil.tryFluidTransfer(tank, exec, Integer.MAX_VALUE, true).isEmpty()) {
                        return moveContainer(exec.getContainer(), inSlot, outSlot);
                    }
                }
            }
        }

        if (!tank.getFluid().isEmpty()) {
            IFluidHandlerItem exec = FluidUtil.getFluidHandler(container.copyWithCount(1)).orElse(null);
            if (exec != null && exec.getFluidInTank(0).isEmpty()
                    && !FluidUtil.tryFluidTransfer(exec, tank, Integer.MAX_VALUE, true).isEmpty()) {
                return moveContainer(exec.getContainer(), inSlot, outSlot);
            }
        }
        return false;
    }

    private boolean moveContainer(ItemStack result, int inSlot, int outSlot) {
        if (result.isEmpty() || !itemHandler.insertItem(outSlot, result, true).isEmpty()) {
            return false;
        }
        itemHandler.extractItem(inSlot, 1, false);
        itemHandler.insertItem(outSlot, result, false);
        return true;
    }

    @Nullable
    private BrewingBarrelRecipe findRecipe() {
        FluidRecipeInput in = new FluidRecipeInput(input.getFluid());
        for (var holder : level.getRecipeManager()
                .getAllRecipesFor(RusticRegistries.BREWING_BARREL_TYPE.get())) {
            BrewingBarrelRecipe candidate = holder.value();
            if (candidate.matches(in, level)) {
                FluidStack out = candidate.getOutputFluid();

                boolean fits = output.getFluid().isEmpty()
                        || FluidStack.isSameFluidSameComponents(output.getFluid(), out);
                if (fits && output.getFluidAmount() < output.getCapacity()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void brew() {
        if (recipe == null) {
            return;
        }
        int amount = Math.min(output.getCapacity() - output.getFluidAmount(), input.getFluidAmount());
        if (amount <= 0) {
            return;
        }
        FluidStack result = recipe.getOutputFluid();
        if (result.isEmpty()) {
            return;
        }
        if (result.getFluid() instanceof RusticBooze) {
            result.set(RusticDataComponents.QUALITY.get(), computeQuality(result));
        }
        FluidStack toFill = result.copyWithAmount(amount);
        if (output.fill(toFill, IFluidHandler.FluidAction.SIMULATE) == amount
                && input.drain(amount, IFluidHandler.FluidAction.SIMULATE).getAmount() == amount) {
            output.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
            input.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private float computeQuality(FluidStack out) {
        net.minecraft.util.RandomSource rand = level.getRandom();
        FluidStack aux = auxiliary.getFluid();
        if (!aux.isEmpty() && aux.getFluid() instanceof RusticBooze culture
                && aux.getFluid() == out.getFluid()) {
            float auxQuality = culture.getQuality(aux);
            int min = RusticConfig.MIN_BREW_QUALITY_CHANGE.get();
            int max = RusticConfig.MAX_BREW_QUALITY_CHANGE.get();
            if (max < min) {
                max = min;
            }
            int delta = rand.nextInt((max - min) + 1) + min;
            return Math.max(Math.min((delta + (int) (100 * auxQuality)) / 100F, 1), 0);
        }
        return (5 + rand.nextInt(71)) / 100F;
    }

    public ContainerData getContainerData() {
        return this;
    }

    @Override
    public int getCount() {
        return DATA_COUNT;
    }

    @Override
    public int get(int index) {
        return switch (index) {
            case DATA_BREW_TIME -> this.brewTime;
            case DATA_MAX_BREW_TIME -> this.getMaxBrewTime();
            case DATA_INPUT_AMOUNT -> this.getInputAmount();
            case DATA_OUTPUT_AMOUNT -> this.getOutputAmount();
            case DATA_AUX_AMOUNT -> this.getAuxAmount();
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {

        if (index == DATA_BREW_TIME) {
            this.brewTime = value;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rustic.brewing_barrel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BrewingBarrelMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(net.minecraft.world.inventory.AbstractContainerMenu menu,
                                   net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", this.itemHandler.serializeNBT(registries));
        tag.put("InputTank", this.input.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", this.output.writeToNBT(registries, new CompoundTag()));
        tag.put("AuxTank", this.auxiliary.writeToNBT(registries, new CompoundTag()));
        tag.putInt("BrewTime", this.brewTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            this.itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        if (tag.contains("InputTank")) {
            this.input.readFromNBT(registries, tag.getCompound("InputTank"));
        }
        if (tag.contains("OutputTank")) {
            this.output.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
        if (tag.contains("AuxTank")) {
            this.auxiliary.readFromNBT(registries, tag.getCompound("AuxTank"));
        }
        this.brewTime = tag.getInt("BrewTime");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
