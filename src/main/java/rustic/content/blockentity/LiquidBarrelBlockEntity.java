package rustic.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * 1.12.2's {@code TileEntityLiquidBarrel}: a single {@link FluidTank} (16 buckets) with no inventory and no GUI.
 * Legacy extended Forge's {@code TileFluidHandler}; 1.21 drops that base, so this owns a {@link FluidTank} directly
 * and exposes it through the {@code Capabilities.FluidHandler.BLOCK} provider registered in
 * {@link RusticBlockEntities#registerCapabilities}. Fill/drain/temperature gating lives on the {@link FluidTank}
 * validator and the block's interaction code, since the legacy {@code setCanFill}/{@code setCanDrain} flags no
 * longer exist.
 *
 * <p>Any tank change marks the block dirty and pushes an update packet to watching clients, replacing the legacy
 * {@code markDirty} + {@code notifyBlockUpdate} + {@code addBlockEvent(1)} dance; the client then re-reads the tank
 * through {@link #getUpdateTag(HolderLookup.Provider)}.
 */
public class LiquidBarrelBlockEntity extends BlockEntity {

    /** Legacy {@code TileEntityLiquidBarrel.capacity}: sixteen buckets. */
    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 16;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    };

    public LiquidBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(RusticBlockEntities.LIQUID_BARREL.get(), pos, state);
    }

    public FluidTank getTank() {
        return this.tank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", this.tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
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
