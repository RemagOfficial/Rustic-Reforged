package rustic.content.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import rustic.content.potions.RusticMobEffects;

public class ItemIronberries extends Item {

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

            player.addEffect(new MobEffectInstance(RusticMobEffects.FULLMETAL, duration, 0, false, true));
        }
        return stack;
    }
}
