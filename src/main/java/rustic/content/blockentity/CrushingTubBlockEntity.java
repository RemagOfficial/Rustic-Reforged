package rustic.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import rustic.content.crafting.CrushingTubRecipe;
import rustic.registry.RusticRegistries;

import java.util.Optional;

public class CrushingTubBlockEntity extends BlockEntity {

    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 8;

    public static final int INPUT_SLOT = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            sync();
        }
    };

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            sync();
        }
    };

    public CrushingTubBlockEntity(BlockPos pos, BlockState state) {
        super(RusticBlockEntities.CRUSHING_TUB.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public IFluidHandler getDrainView() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return tank.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tankIndex) {
                return tank.getFluidInTank(tankIndex);
            }

            @Override
            public int getTankCapacity(int tankIndex) {
                return tank.getTankCapacity(tankIndex);
            }

            @Override
            public boolean isFluidValid(int tankIndex, FluidStack stack) {
                return false;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return tank.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return tank.drain(maxDrain, action);
            }
        };
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void crush() {
        Level level = this.level;
        BlockPos pos = this.worldPosition;
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack input = this.itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }
        Optional<RecipeHolder<CrushingTubRecipe>> match = level.getRecipeManager()
                .getRecipeFor(RusticRegistries.CRUSHING_TUB_TYPE.get(), new SingleRecipeInput(input), level);
        if (match.isEmpty()) {
            return;
        }
        CrushingTubRecipe recipe = match.get().value();
        FluidStack output = recipe.getFluid();
        if (output.isEmpty()) {
            return;
        }

        if (this.tank.fill(output, IFluidHandler.FluidAction.SIMULATE) < output.getAmount()) {
            return;
        }
        this.tank.fill(output, IFluidHandler.FluidAction.EXECUTE);
        ItemStack crushed = input.copyWithCount(1);
        this.itemHandler.extractItem(INPUT_SLOT, 1, false);
        ItemStack byproduct = recipe.getByproduct();
        if (!byproduct.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, byproduct);
        }
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                SoundEvents.SLIME_BLOCK_FALL, SoundSource.BLOCKS, 0.5F,
                0.8F + level.getRandom().nextFloat() * 0.2F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, crushed),
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    level.getRandom().nextInt(8) + 8, 0.1D, 0.1D, 0.1D, 0.05D);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", this.itemHandler.serializeNBT(registries));
        tag.put("Tank", this.tank.writeToNBT(registries, new CompoundTag()));
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
