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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import rustic.config.RusticConfig;
import rustic.content.blocks.ApiaryBlock;
import rustic.content.items.RusticItems;
import rustic.content.menu.ApiaryMenu;

public class ApiaryBlockEntity extends BlockEntity implements MenuProvider {

    private static final int REPRODUCTION_TIME_BASE = 1600;

    private static final int PRODUCTION_TIME_BASE = 800;

    private final ApiarySlotHandler bees;
    private final ApiarySlotHandler honeyComb;

    public int reproductionTimer = 0;

    public int productionTimer = 0;

    private final class ApiarySlotHandler extends ItemStackHandler {
        private final java.util.function.Predicate<ItemStack> allowed;

        ApiarySlotHandler(java.util.function.Predicate<ItemStack> allowed) {
            super(1);
            this.allowed = allowed;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!allowed.test(stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    }

    public ApiaryBlockEntity(BlockPos pos, BlockState state) {
        super(RusticBlockEntities.APIARY.get(), pos, state);
        this.bees = new ApiarySlotHandler(stack -> stack.is(RusticItems.BEE.get()));
        this.honeyComb = new ApiarySlotHandler(stack -> stack.is(RusticItems.HONEYCOMB.get()));
    }

    public ItemStackHandler getBeeHandler() {
        return bees;
    }

    public ItemStackHandler getHoneyCombHandler() {
        return honeyComb;
    }

    public int getBeeCount() {
        ItemStack stack = bees.getStackInSlot(0);
        return stack.is(RusticItems.BEE.get()) ? stack.getCount() : 0;
    }

    public int getHoneyCombCount() {
        ItemStack stack = honeyComb.getStackInSlot(0);
        return stack.is(RusticItems.HONEYCOMB.get()) ? stack.getCount() : 0;
    }

    public static int reproductionTimeFor(int bees, double reproductionMultiplier) {
        return (int) (reproductionMultiplier * (REPRODUCTION_TIME_BASE / (bees / 20F + 1F)));
    }

    public static int productionTimeFor(int bees, double honeycombMultiplier) {
        return (int) (honeycombMultiplier * (PRODUCTION_TIME_BASE / (bees / 20F + 1F)));
    }

    public static int growthGateDenominatorFor(int bees, double growthMultiplier) {
        double scaled = bees * growthMultiplier;
        if (scaled <= 0.0) {
            return 2048;
        }
        return (int) Math.ceil(2048F / scaled);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ApiaryBlockEntity be) {
        be.tick();
    }

    private void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        int numBees = getBeeCount();
        if (numBees <= 0) {

            return;
        }
        int numHoneycomb = getHoneyCombCount();

        double reproductionMultiplier = RusticConfig.BEE_REPRODUCTION_MULTIPLIER.get();
        double honeycombMultiplier = RusticConfig.BEE_HONEYCOMB_MULTIPLIER.get();
        double growthMultiplier = RusticConfig.BEE_GROWTH_MULTIPLIER.get();

        int reproductionTime = reproductionTimeFor(numBees, reproductionMultiplier);
        int productionTime = productionTimeFor(numBees, honeycombMultiplier);

        reproductionTimer++;
        productionTimer++;
        if (reproductionTimer >= reproductionTime) {
            reproductionTimer = 0;
            if (numBees < 64) {
                bees.getStackInSlot(0).grow(1);
            }
        }
        if (productionTimer >= productionTime) {
            productionTimer = 0;
            if (numHoneycomb == 0) {
                honeyComb.setStackInSlot(0, new ItemStack(RusticItems.HONEYCOMB.get(), 1));
            } else if (numHoneycomb < 64) {
                honeyComb.getStackInSlot(0).grow(1);
            }
        }

        if (growthMultiplier != 0 && level.getRandom().nextInt(growthGateDenominatorFor(numBees, growthMultiplier)) == 0) {
            boostNearbyCrop(level, worldPosition, level.getRandom(), growthMultiplier);
        }

        setChanged();
    }

    private static void boostNearbyCrop(Level level, BlockPos origin, RandomSource random, double growthMultiplier) {
        int randX = random.nextInt(9) - 4;
        int randZ = random.nextInt(9) - 4;
        for (int i = 0; i < 3; i++) {
            BlockPos probe = origin.offset(randX, 1 - i, randZ);
            BlockState state = level.getBlockState(probe);
            if (isCropBoostTarget(state)) {
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    state.randomTick(serverLevel, probe, random);
                }
                break;
            }
        }
    }

    private static boolean isCropBoostTarget(BlockState state) {
        return state.getBlock() instanceof BonemealableBlock;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rustic.apiary");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ApiaryMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put("bees", this.bees.serializeNBT(registries));
        tag.put("honeyComb", this.honeyComb.serializeNBT(registries));
        tag.putInt("reproductionTimer", this.reproductionTimer);
        tag.putInt("productionTimer", this.productionTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("bees")) {
            this.bees.deserializeNBT(registries, tag.getCompound("bees"));
        }
        if (tag.contains("honeyComb")) {
            this.honeyComb.deserializeNBT(registries, tag.getCompound("honeyComb"));
        }

        if (tag.contains("items")) {
            ItemStackHandler combined = new ItemStackHandler(2);
            combined.deserializeNBT(registries, tag.getCompound("items"));
            this.bees.setStackInSlot(0, combined.getStackInSlot(0));
            this.honeyComb.setStackInSlot(0, combined.getStackInSlot(1));
        }
        this.reproductionTimer = tag.getInt("reproductionTimer");
        this.productionTimer = tag.getInt("productionTimer");
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
