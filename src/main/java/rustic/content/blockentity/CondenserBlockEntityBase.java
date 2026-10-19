package rustic.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.crafting.CondenserRecipeInput;
import rustic.registry.RusticRegistries;

public abstract class CondenserBlockEntityBase extends BlockEntity implements MenuProvider, ContainerData {

    public static final int SLOT_RESULT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_BOTTLE = 2;
    public static final int SLOT_INGREDIENTS_START = 3;

    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 8;

    private static final int DATA_BURN_TIME = 0;
    private static final int DATA_CURRENT_ITEM_BURN = 1;
    private static final int DATA_BREW_TIME = 2;
    private static final int DATA_TOTAL_BREW_TIME = 3;
    private static final int DATA_COUNT = 4;

    protected final ItemStackHandler itemHandler;
    protected final FluidTank tank;

    @Nullable
    protected CondenserRecipe currentRecipe;
    protected boolean hasContentChanged = true;

    public int condenserBurnTime;
    public int currentItemBurnTime;
    public int brewTime;
    public int totalBrewTime;

    protected CondenserBlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize) {
        super(type, pos, state);
        this.itemHandler = new ItemStackHandler(inventorySize) {
            @Override
            protected void onContentsChanged(int slot) {
                CondenserBlockEntityBase.this.hasContentChanged = true;
                sync();
            }
        };
        this.tank = new FluidTank(CAPACITY) {
            @Override
            protected void onContentsChanged() {
                sync();
            }
        };
    }

    protected abstract int[] ingredientSlots();

    protected abstract int modifierSlot();

    protected abstract boolean recipeAllowed(CondenserRecipe recipe);

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public boolean isBurning() {
        return this.condenserBurnTime > 0;
    }

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().getBurnTime(stack, RecipeType.SMELTING) > 0;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    protected void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean isBrewing = canBrew();
        boolean hasRecipe = isBrewing && hasValidRecipe();
        if (burnFuel(isBrewing && hasRecipe) && isBrewing && hasRecipe) {
            brewTime++;
            if (brewTime >= currentRecipe.getTime()) {
                brewTime = 0;
                brew();
            }
        } else if (brewTime > 0) {
            brewTime = Math.max(0, Math.min(brewTime - 2, totalBrewTime));
        }
    }

    protected boolean canBrew() {
        for (int slot : ingredientSlots()) {
            if (!itemHandler.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasValidRecipe() {
        if (this.condenserBurnTime <= 0 && itemHandler.getStackInSlot(SLOT_FUEL).isEmpty()) {
            return false;
        }
        if (this.hasContentChanged) {
            refreshCurrentRecipe();
        }
        if (this.currentRecipe == null) {
            return false;
        }
        if (this.tank.getFluidAmount() < this.currentRecipe.getFluid().getAmount()) {
            return false;
        }
        return itemHandler.insertItem(SLOT_RESULT, this.currentRecipe.getResultItem(level.registryAccess()), true).isEmpty();
    }

    protected void refreshCurrentRecipe() {
        this.hasContentChanged = false;
        CondenserRecipeInput in = buildInput();
        if (this.currentRecipe != null && this.currentRecipe.matches(in, level)) {
            return;
        }
        this.brewTime = 0;
        this.currentRecipe = null;
        for (var holder : level.getRecipeManager().getAllRecipesFor(RusticRegistries.CONDENSER_TYPE.get())) {
            CondenserRecipe candidate = holder.value();
            if (!recipeAllowed(candidate)) {
                continue;
            }
            if (candidate.matches(in, level)) {
                this.currentRecipe = candidate;
                this.totalBrewTime = candidate.getTime();
                return;
            }
        }
    }

    private boolean burnFuel(boolean consumeNewFuel) {
        if (this.condenserBurnTime > 0) {
            this.condenserBurnTime--;
            return true;
        }
        ItemStack fuel = itemHandler.getStackInSlot(SLOT_FUEL);
        if (consumeNewFuel && !fuel.isEmpty()) {
            int burn = fuel.getItem().getBurnTime(fuel, RecipeType.SMELTING);
            if (burn > 0) {
                this.condenserBurnTime = burn;
                this.currentItemBurnTime = burn;
                fuel.shrink(1);

                return true;
            }
        }
        return false;
    }

    protected void brew() {
        if (this.currentRecipe == null || !canBrew()) {
            return;
        }
        CondenserRecipeInput in = buildInput();
        FluidStack required = this.currentRecipe.getFluid();
        if (!this.currentRecipe.matches(in, level) || this.tank.getFluidAmount() < required.getAmount()) {
            return;
        }
        ItemStack result = this.currentRecipe.getResultItem(level.registryAccess()).copy();
        if (!result.isEmpty()) {
            if (!itemHandler.insertItem(SLOT_RESULT, result, true).isEmpty()) {
                return;

            }
            itemHandler.insertItem(SLOT_RESULT, result, false);
        }
        for (int slot : ingredientSlots()) {
            itemHandler.extractItem(slot, 1, false);
        }
        int modifier = modifierSlot();
        if (modifier >= 0) {
            itemHandler.extractItem(modifier, 1, false);
        }
        itemHandler.extractItem(SLOT_BOTTLE, 1, false);
        this.tank.drain(required.getAmount(), IFluidHandler.FluidAction.EXECUTE);
    }

    private CondenserRecipeInput buildInput() {
        int[] slots = ingredientSlots();
        ItemStack[] ingredients = new ItemStack[slots.length];
        for (int i = 0; i < slots.length; i++) {
            ingredients[i] = itemHandler.getStackInSlot(slots[i]);
        }
        int modifier = modifierSlot();
        ItemStack modifierStack = modifier >= 0 ? itemHandler.getStackInSlot(modifier) : ItemStack.EMPTY;
        return new CondenserRecipeInput(ingredients, itemHandler.getStackInSlot(SLOT_BOTTLE),
                modifierStack, this.tank.getFluid());
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
            case DATA_BURN_TIME -> this.condenserBurnTime;
            case DATA_CURRENT_ITEM_BURN -> this.currentItemBurnTime;
            case DATA_BREW_TIME -> this.brewTime;
            case DATA_TOTAL_BREW_TIME -> this.totalBrewTime;
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {

        switch (index) {
            case DATA_BURN_TIME -> this.condenserBurnTime = value;
            case DATA_CURRENT_ITEM_BURN -> this.currentItemBurnTime = value;
            case DATA_BREW_TIME -> this.brewTime = value;
            case DATA_TOTAL_BREW_TIME -> this.totalBrewTime = value;
            default -> { }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getMenuTitleKey());
    }

    protected abstract String getMenuTitleKey();

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return createCondenserMenu(containerId, playerInventory);
    }

    protected abstract AbstractContainerMenu createCondenserMenu(int containerId, Inventory playerInventory);

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", this.itemHandler.serializeNBT(registries));
        tag.put("Tank", this.tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("BurnTime", this.condenserBurnTime);
        tag.putInt("ItemBurnTime", this.currentItemBurnTime);
        tag.putInt("BrewTime", this.brewTime);
        tag.putInt("BrewTimeTotal", this.totalBrewTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            this.itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        }
        if (tag.contains("Tank")) {
            this.tank.readFromNBT(registries, tag.getCompound("Tank"));
        }
        this.condenserBurnTime = tag.getInt("BurnTime");
        this.currentItemBurnTime = tag.getInt("ItemBurnTime");
        this.brewTime = tag.getInt("BrewTime");
        this.totalBrewTime = tag.getInt("BrewTimeTotal");
        this.hasContentChanged = true;
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
