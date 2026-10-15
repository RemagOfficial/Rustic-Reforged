package rustic.content.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import rustic.content.potions.RusticMobEffects;

/**
 * 1.12.2's {@code ModItems.IRONBERRIES}: the ironwood berry whose eating paralyses the consumer with
 * FULLMETAL, stacking its duration instead of restarting it.
 *
 * <p>Everything except that effect is data: the nutrition, saturation, the 16-tick eat time (legacy's
 * {@code getMaxItemUseDuration} override, 1.21's {@code fast()}) and the always-edible flag live on the
 * {@link Item.Properties} the caller passes, folded into {@code FoodProperties} the way Phase 5 folded
 * the herb effects. The one thing {@code FoodProperties} cannot express is the duration arithmetic -
 * legacy read the live effect and added 15 seconds to whatever was left - so that half stays code, a
 * verbatim port of the old {@code onFoodEaten}.
 *
 * <p>The commented-out resistance/fire-resistance/slowness/fatigue/weakness/jump block the legacy
 * override carried is left commented in spirit only: it never ran, so it is not ported.
 */
public class ItemIronberries extends Item {

    /** 1.12.2's {@code int duration = 15 * 20;} */
    private static final int FULLMETAL_DURATION = 15 * 20;

    public ItemIronberries(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        stack = super.finishUsingItem(stack, level, livingEntity);
        if (!level.isClientSide && livingEntity instanceof Player player) {
            MobEffectInstance existing = player.getEffect(RusticMobEffects.FULLMETAL);
            int duration = existing == null ? FULLMETAL_DURATION : existing.getDuration() + FULLMETAL_DURATION;
            // The legacy PotionEffect flags: ambient false, showParticles true -> visible true.
            player.addEffect(new MobEffectInstance(RusticMobEffects.FULLMETAL, duration, 0, false, true));
        }
        return stack;
    }
}
