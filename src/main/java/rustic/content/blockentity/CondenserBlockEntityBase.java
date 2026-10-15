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

/**
 * The shared engine behind both condensers, the 1.21 form of legacy {@code TileEntityCondenserBase}. A fuel slot
 * burns down over ticks; while it burns and a {@link CondenserRecipe} matches (a bottle in the bottle slot, the
 * recipe's water in the tank, the exact ingredient multiset in the ingredient slots and - for the advanced machine -
 * a modifier herb), a brew countdown runs and, at the end, the batch is consumed into the result slot. The whole
 * furnace-shaped loop is lifted verbatim in structure from the legacy {@code update()}/{@code burnFuel()}/{@code brew()}.
 *
 * <p><b>Retort gate dropped (a deliberate phase deviation).</b> Legacy gated {@code canBrew} and every capability
 * lookup on a {@code hasRetorts} check - the condenser only ran when matching RETORT blocks flanked it. Retorts are
 * not one of this phase's machines, so the gate is removed: the plan's M5 contract is simply "matches a {@code
 * CondenserRecipe}", and until Phase 7 fills the recipe JSON both machines place, hold state, sync and open their GUI
 * but craft nothing. Re-adding the retort requirement is a later-phase concern, not a functional-shell one.
 *
 * <p><b>What the subclasses supply.</b> The slot count, which slots are ingredients, whether there is a modifier slot
 * and which recipes are allowed differ between the basic (5 slots, no modifier, only non-advanced recipes) and the
 * advanced (7 slots, modifier + 3 ingredient slots, any recipe) machines; everything else - the tank, the timers, the
 * {@link ContainerData} menu sync, save/load and block-update sync - lives here. The two are modelled as one logical
 * inventory each: the advanced condenser's upper block half carries no block entity at all (it forwards the player's
 * click down here), so unlike the double cabinet there is no {@code CompoundContainer} to merge - the single bottom
 * inventory <em>is</em> the merged view. That is exactly how legacy {@code TileEntityCondenserAdvancedTop} behaved
 * (it only delegated capabilities to the block below).
 */
public abstract class CondenserBlockEntityBase extends BlockEntity implements MenuProvider, ContainerData {

    // Legacy TileEntityCondenserBase slot indices.
    public static final int SLOT_RESULT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_BOTTLE = 2;
    public static final int SLOT_INGREDIENTS_START = 3;

    /** Legacy {@code CAPACITY}: eight buckets of water. */
    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 8;

    // The four furnace-style ints the menu syncs every tick.
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

    // ------------------------------------------------------------------ subclass shape

    /** The slots offered to a {@link CondenserRecipe} as its ingredient array (basic: two; advanced: three). */
    protected abstract int[] ingredientSlots();

    /** The modifier herb slot, or {@code -1} for the basic condenser (which has none and always offers {@link ItemStack#EMPTY}). */
    protected abstract int modifierSlot();

    /** Whether this machine will run the given recipe at all (basic refuses advanced recipes; advanced accepts any). */
    protected abstract boolean recipeAllowed(CondenserRecipe recipe);

    // ------------------------------------------------------------------ accessors

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public boolean isBurning() {
        return this.condenserBurnTime > 0;
    }

    /** Whether the given stack is a furnace fuel, the 1.21 form of legacy {@code TileEntityFurnace.isItemFuel}. */
    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().getBurnTime(stack, RecipeType.SMELTING) > 0;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ ticking

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

    /** Legacy {@code canBrew} minus the retort gate: at least one ingredient slot is loaded. */
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

    /** Legacy {@code burnFuel}: keep an existing burn burning, else light a fresh fuel item. */
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
                // The legacy getContainerItem hand-off (lava bucket -> empty bucket) is dropped: no shell fuel here
                // has a container item, and ItemStack#getCraftingRemainingItem edge handling is deferred with the
                // retore/automation polish. Emptying the slot on shrink-to-zero is the visible behaviour we need.
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
                return; // no room for the result yet
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

    // ------------------------------------------------------------------ container data (menu sync)

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
        // The four timers flow to the client here so the screen reads them off this synced copy; the tank rides the
        // ordinary block update.
        switch (index) {
            case DATA_BURN_TIME -> this.condenserBurnTime = value;
            case DATA_CURRENT_ITEM_BURN -> this.currentItemBurnTime = value;
            case DATA_BREW_TIME -> this.brewTime = value;
            case DATA_TOTAL_BREW_TIME -> this.totalBrewTime = value;
            default -> { }
        }
    }

    // ------------------------------------------------------------------ menu provider

    @Override
    public Component getDisplayName() {
        return Component.translatable(getMenuTitleKey());
    }

    /** The {@code container.rustic.*} key each subclass names for its GUI title. */
    protected abstract String getMenuTitleKey();

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return createCondenserMenu(containerId, playerInventory);
    }

    /** Builds this machine's bespoke menu over its own container (the server half of the {@link MenuProvider}). */
    protected abstract AbstractContainerMenu createCondenserMenu(int containerId, Inventory playerInventory);

    /** Writes this block's position so the client menu factory can resolve the client BE copy the screen renders. */
    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.worldPosition);
    }

    // ------------------------------------------------------------------ save / load / sync

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
