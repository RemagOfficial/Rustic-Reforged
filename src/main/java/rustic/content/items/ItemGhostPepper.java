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

public class ItemGhostPepper extends Item {

    private static final int FIRE_POWER_DURATION = 1200;
    private static final int FIRE_POWER_AMPLIFIER = 0;

    public ItemGhostPepper(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        stack = super.finishUsingItem(stack, level, livingEntity);
        if (!level.isClientSide && livingEntity instanceof Player player) {

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
