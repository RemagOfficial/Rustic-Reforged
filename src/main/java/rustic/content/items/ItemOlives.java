package rustic.content.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.12.2's {@code ModItems.OLIVES}: the raw tree fruit whose nausea is the joke - "Raw, unprocessed
 * olives should not be consumed".
 *
 * <p>Everything but the eat time is data: the nutrition, saturation and the legacy
 * {@code setPotionEffect(NAUSEA, 200, 1, 0.95F)} fold onto {@code FoodProperties} in {@code RusticItems}
 * (1.20.5 renamed {@code MobEffects.NAUSEA} to {@code MobEffects.CONFUSION}). The one thing the food
 * record cannot carry is the eat duration: 1.21.1's {@code FoodProperties.Builder} exposes only
 * {@code fast()} (0.8 s, the ironberries' 16-tick number) and never a setter for the private
 * {@code eatSeconds}, so the legacy {@code getMaxItemUseDuration} override returning 24 ticks (1.2 s) is
 * ported here as the code-side override of {@link Item#getUseDuration} instead - the same split this mod
 * already uses for the tomato, ghost pepper and ironberries, where the food properties do the data half
 * and a subclass carries whatever {@code FoodProperties} cannot hold.
 *
 * <p>No always-edible flag: the legacy {@code initFood} override set only the potion, so eating olives
 * when full does nothing special - it just makes you ill.
 */
public class ItemOlives extends Item {

    /** 1.12.2's {@code getMaxItemUseDuration} return, in ticks. */
    private static final int EAT_DURATION_TICKS = 24;

    public ItemOlives(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return EAT_DURATION_TICKS;
    }
}
