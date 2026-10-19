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

/**
 * 1.12.2's {@code TileEntityApiary}: the bee box. One slot takes {@code rustic:bee} items (stacking to 64),
 * one slot accumulates {@code rustic:honeycomb}, and every tick the colony reproduces, produces comb and -
 * behind a bee-count-scaled dice roll - forces one nearby crop to age.
 *
 * <p><b>The bee-item design decision (Phase 15, plan assumption 2).</b> No adaptation turned out to be
 * needed: the legacy apiary never spawned a bee entity. {@code ItemBase("bee")} was a plain loot item the
 * beehive dropped, consumed straight out of the GUI slot, and the whole colony ran on slot counts. The port
 * keeps the existing plain {@link RusticItems#BEE} item and the same item-driven simulation verbatim.
 *
 * <p><b>Simulation fidelity.</b> The two timer formulas and the growth roll are lifted line-for-line from
 * legacy {@code update()}: reproduction every {@code 1600 / (bees/20 + 1)} ticks and honeycomb every
 * {@code 800 / (bees/20 + 1)} ticks (each divided by its config multiplier, larger = slower), both clamped
 * to a 64-stack; and one crop boost per {@code ceil(2048 / (bees * growthMultiplier))}-tick chance on
 * average, aimed at a random block in the 9x9 column two blocks tall centred on the apiary (almanac: "four
 * blocks horizontally, one vertically" - the probe reads the top block first, then two below it). The
 * legacy {@code block.updateTick} + {@code updateBlockTick} pair becomes 1.21's public
 * {@link BlockState#randomTick}: both hand the crop to its own tick-driven aging, the exact mechanic the
 * 1.12.2 call reached. A crop reached this way is aged by the crop's own rules (vanilla crops roll their
 * 1/3 chance), exactly as under legacy random-tick dispatch.
 *
 * <p><b>Idle contract.</b> With no bees the timers simply do not advance - legacy froze them too, so a
 * half-filled apiary keeps its progress across an empty spell.
 */
public class ApiaryBlockEntity extends BlockEntity implements MenuProvider {

    /**
     * The legacy {@code update()} formula constant. The 1.12.2 field initializer read 1200, but that value
     * was overwritten before every use; the shipped divisor is 1600F, which is what the sim must reproduce.
     */
    private static final int REPRODUCTION_TIME_BASE = 1600;
    /** The legacy {@code update()} production divisor; the 600 field initializer never reached a tick. */
    private static final int PRODUCTION_TIME_BASE = 800;

    /** The single-slot handler pair legacy built anonymously: slot 0 admits only bees, slot 1 only honeycomb. */
    private final ApiarySlotHandler bees;
    private final ApiarySlotHandler honeyComb;

    /** Legacy {@code reproductionTimer} - free-running while bees are present, frozen (not reset) without. */
    public int reproductionTimer = 0;
    /** Legacy {@code productionTimer}, same idle contract. */
    public int productionTimer = 0;

    /**
     * A one-slot {@code ItemStackHandler} filtered to a single item, the 1.21 form of legacy's anonymous
     * {@code insertItem} override: anything else is refused on insert (hoppers included).
     */
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

    // ------------------------------------------------------------------ accessors

    /** The bee slot's handler (all faces but the bottom, and the GUI's top slot). */
    public ItemStackHandler getBeeHandler() {
        return bees;
    }

    /** The honeycomb slot's handler (the bottom face, and the GUI's lower slot). */
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

    // ------------------------------------------------------------------ pure sim math (unit-testable)

    /**
     * Legacy {@code update()}: {@code (int)(reproductionMultiplier * (1600 / (bees / 20F + 1)))}. Larger
     * multiplier = slower; more bees = faster, saturating around 64.
     */
    public static int reproductionTimeFor(int bees, double reproductionMultiplier) {
        return (int) (reproductionMultiplier * (REPRODUCTION_TIME_BASE / (bees / 20F + 1F)));
    }

    /** Legacy {@code update()}: {@code (int)(honeycombMultiplier * (800 / (bees / 20F + 1)))}. */
    public static int productionTimeFor(int bees, double honeycombMultiplier) {
        return (int) (honeycombMultiplier * (PRODUCTION_TIME_BASE / (bees / 20F + 1F)));
    }

    /**
     * The denominator of the legacy growth roll {@code random.nextInt(denominator) == 0}, i.e. its mean
     * period in ticks: {@code ceil(2048 / (bees * growthMultiplier))}, and 2048 when the product is zero
     * (the roll then just never fires more often than the no-bees case the outer guard already excludes).
     */
    public static int growthGateDenominatorFor(int bees, double growthMultiplier) {
        double scaled = bees * growthMultiplier;
        if (scaled <= 0.0) {
            return 2048;
        }
        return (int) Math.ceil(2048F / scaled);
    }

    // ------------------------------------------------------------------ ticking

    /** Server tick entry point the block's ticker calls (see {@link ApiaryBlock#getTicker}). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ApiaryBlockEntity be) {
        be.tick();
    }

    private void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        int numBees = getBeeCount();
        if (numBees <= 0) {
            // Legacy update() early-exits with no bees: the timers keep their value but do not advance.
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

    /**
     * Legacy growth-roll body: pick a random offset in the 9x9 around the apiary, probe {@code pos.above()}
     * then the two blocks below it, and hand the first crop (or plantable) found to its own random tick -
     * the 1.12.2 {@code updateTick}/{@code updateBlockTick} pair. {@code randX}/{@code randZ} are drawn even
     * when the roll misses nothing - matching legacy, which drew them inside the same if.
     */
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

    /**
     * The 1.21 form of legacy {@code block instanceof IGrowable || block instanceof IPlantable}: the blocks a
     * random tick ages. {@link BonemealableBlock} is that growable family (crops, saplings, stems, berry bushes);
     * {@link BlockState#randomTick} then hands the block to its own aging, which a non-growing block simply
     * ignores - the same "reached by random tick, aged by its own rules" outcome legacy's {@code updateTick}
     * dispatch had for an IGrowable that was not really a crop.
     */
    private static boolean isCropBoostTarget(BlockState state) {
        return state.getBlock() instanceof BonemealableBlock;
    }

    // ------------------------------------------------------------------ menu provider

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rustic.apiary");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ApiaryMenu(containerId, playerInventory, this);
    }

    /** Writes this block's position so the client menu factory can resolve the client BE copy the screen renders. */
    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.worldPosition);
    }

    // ------------------------------------------------------------------ save / load

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Legacy key names kept: "bees" and "honeyComb" are what 1.12.2 wrote, and old saves migrate free.
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
        // The pre-split legacy format: one "items" handler of two slots (bee, honeycomb), as
        // TileEntityApiary.readFromNBT accepted alongside its own keys.
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
