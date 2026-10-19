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

public class ItemFluidBottle extends Item {

    public static final int CAPACITY = 1000;

    private static final List<DeferredHolder<Fluid, ? extends Fluid>> VALID_FLUIDS = List.of(
            RusticFluids.OLIVE_OIL, RusticFluids.IRONBERRY_JUICE, RusticFluids.WILDBERRY_JUICE,
            RusticFluids.GRAPE_JUICE, RusticFluids.APPLE_JUICE, RusticFluids.ALE_WORT, RusticFluids.HONEY,
            RusticFluids.GOLDEN_APPLE_JUICE, RusticFluids.VANTA_OIL,
            RusticFluids.ALE, RusticFluids.CIDER, RusticFluids.IRON_WINE, RusticFluids.MEAD,
            RusticFluids.WILDBERRY_WINE, RusticFluids.WINE, RusticFluids.AMBROSIA);

    public ItemFluidBottle(Properties properties) {

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

    public static FluidStack getFluidContent(ItemStack stack) {
        FluidContent content = stack.get(RusticDataComponents.FLUID_CONTENT.get());
        return content == null ? FluidStack.EMPTY : content.stack();
    }

    private static boolean isDrinkable(ItemStack stack) {
        return getFluidContent(stack).getFluid() instanceof DrinkableFluid;
    }

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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

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

    @Override
    public Component getName(ItemStack stack) {
        FluidStack content = getFluidContent(stack);
        if (content.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE).getHoverName();
        }

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

            float quality = booze.getQuality(content);
            int color = RusticBooze.getQualityLabelColor(quality);
            tooltip.add(Component.translatable("tooltip.rustic.quality")
                    .append(Component.literal(String.format(" %.0f%%", quality * 100))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)))));
        }
    }

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
