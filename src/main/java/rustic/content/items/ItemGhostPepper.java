package rustic.content.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import rustic.content.potions.RusticMobEffects;

import java.util.List;

/**
 * 1.12.2's {@code ModItems.GHOST_PEPPER}: the hottest food in the mod, and the only one whose bite
 * actually hurts before it buffs.
 *
 * <p>The FOOD half of the legacy {@code onFoodEaten} - the two hearts of fire damage - cannot be data:
 * {@code FoodProperties} effects only add mob effects, never deal damage, so the hurt call stays code
 * exactly where legacy had it, server-side, players only. The FIRE_POWER effect could have been a
 * {@code FoodProperties.Builder#effect} line, and is deliberately not: keeping both halves of the bite
 * in one method preserves the legacy order (damage first, then the effect) that decides what a
 * death-by-ghost-pepper looks like.
 *
 * <p>Faithful legacy numbers: {@code attackEntityFrom(DamageSource.ON_FIRE, 2.0F)} becomes the
 * {@code inFire} damage source at 2.0F, and the effect is
 * {@code PotionEffect(FIRE_POWER, 1200, 0)} - sixty seconds at amplifier 0, default flags. The yellow
 * italic tooltip line is the legacy {@code addInformation} override; its lang key
 * {@code tooltip.rustic.ghost_pepper} existed in the 1.12.2 code but never in its lang file, so the
 * English text added with this port fills a gap rather than moving a value.
 */
public class ItemGhostPepper extends Item {

    /** 1.12.2's {@code player.addPotionEffect(new PotionEffect(FIRE_POWER_POTION, 1200, 0))}. */
    private static final int FIRE_POWER_DURATION = 1200;
    private static final int FIRE_POWER_AMPLIFIER = 0;

    public ItemGhostPepper(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        stack = super.finishUsingItem(stack, level, livingEntity);
        if (!level.isClientSide && livingEntity instanceof Player player) {
            // Legacy DamageSource.ON_FIRE, the 1.21 name for the same "burning" source.
            player.hurt(level.damageSources().inFire(), 2.0F);
            player.addEffect(new MobEffectInstance(RusticMobEffects.FIRE_POWER,
                    FIRE_POWER_DURATION, FIRE_POWER_AMPLIFIER));
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rustic.ghost_pepper").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }
}
