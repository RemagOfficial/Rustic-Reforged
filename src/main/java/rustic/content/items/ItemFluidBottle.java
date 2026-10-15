package rustic.content.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import rustic.content.fluids.DrinkableFluid;
import rustic.content.fluids.FluidContent;
import rustic.content.fluids.RusticBooze;
import rustic.content.fluids.RusticDataComponents;
import rustic.content.fluids.RusticFluids;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The drinkable fluid bottle, ported from 1.12.2's {@code ItemFluidBottle} (which extended Forge's
 * {@code ItemFluidContainer} and attached a {@code FluidHandlerItemStack.SwapEmpty} capability).
 *
 * <p><b>Storage deviation.</b> Legacy kept the fluid in an NBT {@code "Fluid"} compound and the booze quality in a
 * nested {@code Tag} compound; 1.21 replaces both with the typed {@code rustic:fluid_content} data component holding
 * a whole {@link FluidStack}, whose own components carry {@code rustic:quality}. The {@link BottleHandler} below is
 * the 1.21 form of the legacy capability: the same whole-swap contract - {@code fill} only accepts a resource of at
 * least {@link #CAPACITY} into an <em>empty</em> bottle, and {@code drain} only yields a full {@link #CAPACITY} - so
 * a bottle is always full or always gone, never partially filled.</p>
 *
 * <p><b>Rarity deviation.</b> 1.12.2 pulled the bottle's rarity off {@code Fluid.getRarity}; 1.21 has no per-item
 * hook (rarity is a data component set at item construction), so an EPIC fluid's bottle is stamped
 * {@link DataComponents#RARITY} EPIC when it is filled. Only golden apple juice and ambrosia are EPIC, matching the
 * baseline.</p>
 *
 * <p><b>Rendering deviation.</b> The tinted fluid-sprite {@code FluidBottleModel} and its item colour handler are
 * Phase 9; for now the bottle is a plain generated item and the drinkable/quality tooltip lines below are the only
 * visible content.</p>
 */
public class ItemFluidBottle extends Item {

    /** Legacy {@code ItemFluidContainer(1000)} capacity, in millibuckets. */
    public static final int CAPACITY = 1000;

    /**
     * The sixteen source fluids the bottle accepts - the eight placeables plus vanta oil plus the seven boozes.
     * Held as deferred holders and resolved through {@link #isValidFluid} so the class can be initialised before the
     * fluid registry is bound; the flowing variants are deliberately absent (a bottle never holds a flowing fluid).
     */
    private static final List<DeferredHolder<Fluid, ? extends Fluid>> VALID_FLUIDS = List.of(
            RusticFluids.OLIVE_OIL, RusticFluids.IRONBERRY_JUICE, RusticFluids.WILDBERRY_JUICE,
            RusticFluids.GRAPE_JUICE, RusticFluids.APPLE_JUICE, RusticFluids.ALE_WORT, RusticFluids.HONEY,
            RusticFluids.GOLDEN_APPLE_JUICE, RusticFluids.VANTA_OIL,
            RusticFluids.ALE, RusticFluids.CIDER, RusticFluids.IRON_WINE, RusticFluids.MEAD,
            RusticFluids.WILDBERRY_WINE, RusticFluids.WINE, RusticFluids.AMBROSIA);

    public ItemFluidBottle(Properties properties) {
        // A bottle holds one fluid, so it never stacks; the glass-bottle craft remainder is the 1.21 form of the
        // legacy hasContainerItem/getContainerItem pair that handed an empty bottle back after crafting.
        super(properties.stacksTo(1).craftRemainder(Items.GLASS_BOTTLE));
    }

    public static boolean isValidFluid(@Nullable Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        for (DeferredHolder<Fluid, ? extends Fluid> holder : VALID_FLUIDS) {
            if (holder.get() == fluid) {
                return true;
            }
        }
        return false;
    }

    /** The bottle's content, or {@link FluidStack#EMPTY} when the bottle is empty. */
    public static FluidStack getFluidContent(ItemStack stack) {
        FluidContent content = stack.get(RusticDataComponents.FLUID_CONTENT.get());
        return content == null ? FluidStack.EMPTY : content.stack();
    }

    private static boolean isDrinkable(ItemStack stack) {
        return getFluidContent(stack).getFluid() instanceof DrinkableFluid;
    }

    /**
     * A full bottle of {@code fluid}, mirroring legacy {@code getFilledBottle}: the whole-capacity stack goes into
     * the content component, and an EPIC fluid's bottle is stamped EPIC. {@code quality} is written into the fluid
     * stack's own components for the booze tab entries (legacy seeded {@code 0.75F}); pass {@code null} otherwise.
     */
    public ItemStack getFilledBottle(Fluid fluid, @Nullable Float quality) {
        ItemStack bottle = new ItemStack(this);
        if (isValidFluid(fluid)) {
            FluidStack content = new FluidStack(fluid, CAPACITY);
            if (quality != null) {
                content.set(RusticDataComponents.QUALITY.get(), quality);
            }
            bottle.set(RusticDataComponents.FLUID_CONTENT.get(), new FluidContent(content));
            if (fluid.getFluidType().getRarity() == Rarity.EPIC) {
                bottle.set(DataComponents.RARITY, Rarity.EPIC);
            }
        }
        return bottle;
    }

    // ------------------------------------------------------------------ drinking

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Only a drinkable content starts the use; the base food path is irrelevant here (no FOOD component).
        if (isDrinkable(stack)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isDrinkable(stack) ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        Player player = livingEntity instanceof Player p ? p : null;
        FluidStack content = getFluidContent(stack);
        if (!level.isClientSide && content.getFluid() instanceof DrinkableFluid drinkable
                && player != null && level instanceof ServerLevel serverLevel) {
            drinkable.onDrank(serverLevel, player, stack, content);
        }
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (player instanceof ServerPlayer serverPlayer) {
                net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
        }
        // Creative keeps the bottle; otherwise it is consumed and a glass bottle is returned (added to the
        // inventory when the stack had more, dropped whole when the last one was drunk), as legacy did.
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            if (player != null) {
                ItemStack remainder = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.getInventory().add(remainder)) {
                    player.drop(remainder, false);
                }
            }
        }
        return stack;
    }

    // ------------------------------------------------------------------ display

    @Override
    public Component getName(ItemStack stack) {
        FluidStack content = getFluidContent(stack);
        if (content.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE).getHoverName();
        }
        // "Bottle of <fluid>" - the 1.21 form of the legacy translateToLocalFormatted(unloc + ".name", localizedName).
        return Component.translatable(this.getDescriptionId(stack) + ".name", content.getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack content = getFluidContent(stack);
        if (content.getFluid() instanceof DrinkableFluid) {
            tooltip.add(Component.translatable("tooltip.rustic.drinkable")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x555555)).withItalic(true)));
        }
        if (content.getFluid() instanceof RusticBooze booze) {
            // The ported getQualityTooltip thresholds live in RusticBooze.getQualityLabelColor; the colour-coded
            // percent line is reproduced here, minus the legacy player-UUID word variants (a Phase 9 client nicety).
            float quality = booze.getQuality(content);
            int color = RusticBooze.getQualityLabelColor(quality);
            tooltip.add(Component.translatable("tooltip.rustic.quality")
                    .append(Component.literal(String.format(" %.0f%%", quality * 100))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)))));
        }
    }

    /**
     * The whole-swap fluid handler, the 1.21 replacement for the legacy {@code FluidHandlerItemStack.SwapEmpty}
     * capability. It reads and writes the container stack's {@code rustic:fluid_content} component directly, so the
     * caller's stack is the one that changes (NeoForge assigns the returned {@link #getContainer()} back).
     */
    public static final class BottleHandler implements IFluidHandlerItem {
        private final ItemStack container;

        public BottleHandler(ItemStack container) {
            this.container = container;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? getFluidContent(container) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return isValidFluid(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // Whole-swap: only an empty bottle takes a resource of at least a full capacity of a valid fluid.
            if (resource.isEmpty() || resource.getAmount() < CAPACITY
                    || !getFluidContent(container).isEmpty() || !isValidFluid(resource.getFluid())) {
                return 0;
            }
            if (action.execute()) {
                FluidStack content = resource.copyWithAmount(CAPACITY);
                container.set(RusticDataComponents.FLUID_CONTENT.get(), new FluidContent(content));
                container.set(DataComponents.RARITY,
                        resource.getFluid().getFluidType().getRarity() == Rarity.EPIC ? Rarity.EPIC : Rarity.COMMON);
            }
            return CAPACITY;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getAmount() < CAPACITY) {
                return FluidStack.EMPTY;
            }
            FluidStack content = getFluidContent(container);
            if (content.isEmpty() || !FluidStack.isSameFluidSameComponents(content, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(CAPACITY, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            // Whole-swap: a bottle only ever gives up its entire contents, never a partial amount.
            if (maxDrain < CAPACITY) {
                return FluidStack.EMPTY;
            }
            FluidStack content = getFluidContent(container);
            if (content.isEmpty()) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = content.copyWithAmount(CAPACITY);
            if (action.execute()) {
                container.set(RusticDataComponents.FLUID_CONTENT.get(), FluidContent.empty());
                container.set(DataComponents.RARITY, Rarity.COMMON);
            }
            return drained;
        }
    }
}
