package rustic.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import rustic.content.crafting.EvaporatingBasinRecipe;
import rustic.registry.RusticRegistries;

public class EvaporatingBasinBlockEntity extends BlockEntity {

    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 6;

    public static final int OUTPUT_SLOT = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirtyAndSync();
        }
    };

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            markDirtyAndSync();
        }
    };

    private int evaporated = 0;

    @javax.annotation.Nullable
    private Fluid evaporatingFluid = null;

    public EvaporatingBasinBlockEntity(BlockPos pos, BlockState state) {
        super(RusticBlockEntities.EVAPORATING_BASIN.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public FluidTank getTank() {
        return this.tank;
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EvaporatingBasinBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        FluidStack tankFluid = be.tank.getFluid();
        if (tankFluid.isEmpty()) {
            be.evaporated = 0;
            be.evaporatingFluid = null;
            return;
        }

        EvaporatingBasinRecipe recipe = be.findRecipe(level, tankFluid.getFluid());
        if (recipe == null) {
            be.evaporated = 0;
            be.evaporatingFluid = null;
            return;
        }

        if (be.evaporatingFluid != null && be.evaporatingFluid != tankFluid.getFluid()) {
            be.evaporated = 0;
        }
        be.evaporatingFluid = tankFluid.getFluid();

        FluidStack drained = be.tank.drain(1, IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
            be.evaporated += drained.getAmount();
        }

        int required = recipe.getInput().getAmount();
        ItemStack output = recipe.getOutput();

        while (be.evaporated >= required && be.itemHandler.insertItem(OUTPUT_SLOT, output, true).isEmpty()) {
            be.evaporated -= required;
            be.itemHandler.insertItem(OUTPUT_SLOT, output, false);
        }
    }

    @javax.annotation.Nullable
    private EvaporatingBasinRecipe findRecipe(Level level, Fluid fluid) {
        for (RecipeHolder<EvaporatingBasinRecipe> holder
                : level.getRecipeManager().getAllRecipesFor(RusticRegistries.EVAPORATING_BASIN_TYPE.get())) {
            EvaporatingBasinRecipe recipe = holder.value();
            if (!recipe.getInput().isEmpty() && recipe.getInput().getFluid() == fluid) {
                return recipe;
            }
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", this.itemHandler.serializeNBT(registries));
        tag.put("Tank", this.tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Evaporated", this.evaporated);
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
        this.evaporated = tag.getInt("Evaporated");
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
