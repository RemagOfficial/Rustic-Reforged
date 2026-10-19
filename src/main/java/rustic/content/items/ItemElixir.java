package rustic.content.items;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import rustic.content.potions.ElixirUtils;
import rustic.content.potions.RusticMobEffects;

import java.util.ArrayList;
import java.util.List;

public class ItemElixir extends Item {

    public ItemElixir(Properties properties) {

        super(properties.stacksTo(16));
    }

    public static ItemStack defaultElixir() {
        ItemStack stack = new ItemStack(RusticItems.ELIXIR.get());
        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 0), stack);
        return stack;
    }

    public static List<ItemStack> condenserElixirs() {
        List<MobEffectInstance> effects = List.of(

                new MobEffectInstance(MobEffects.HEAL, 1, 0),
                new MobEffectInstance(MobEffects.HEAL, 1, 1),

                new MobEffectInstance(MobEffects.REGENERATION, 900, 0),
                new MobEffectInstance(MobEffects.REGENERATION, 1800, 0),
                new MobEffectInstance(MobEffects.REGENERATION, 450, 1),

                new MobEffectInstance(MobEffects.WITHER, 900, 0),
                new MobEffectInstance(MobEffects.WITHER, 1800, 0),
                new MobEffectInstance(MobEffects.WITHER, 450, 1),

                new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0),
                new MobEffectInstance(MobEffects.NIGHT_VISION, 9600, 0),

                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 9600, 0),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 1),

                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0),
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 9600, 0),

                new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 0),
                new MobEffectInstance(MobEffects.HEALTH_BOOST, 9600, 0),
                new MobEffectInstance(MobEffects.HEALTH_BOOST, 1800, 1),

                new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0),
                new MobEffectInstance(MobEffects.DIG_SPEED, 9600, 0),
                new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 1),

                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0),
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9600, 0),
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800, 1),

                new MobEffectInstance(RusticMobEffects.IRON_SKIN, 3600, 0),
                new MobEffectInstance(RusticMobEffects.IRON_SKIN, 9600, 0),
                new MobEffectInstance(RusticMobEffects.IRON_SKIN, 1800, 1),

                new MobEffectInstance(RusticMobEffects.FEATHER, 3600, 0),
                new MobEffectInstance(RusticMobEffects.FEATHER, 9600, 0),

                new MobEffectInstance(RusticMobEffects.BLAZING_TRAIL, 3600, 0),
                new MobEffectInstance(RusticMobEffects.BLAZING_TRAIL, 9600, 0));

        List<ItemStack> elixirs = new ArrayList<>(effects.size());
        for (MobEffectInstance effect : effects) {
            ItemStack stack = new ItemStack(RusticItems.ELIXIR.get());
            ElixirUtils.addEffect(effect, stack);
            elixirs.add(stack);
        }
        return elixirs;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {

        return 24;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        Player player = livingEntity instanceof Player p ? p : null;
        if (!level.isClientSide) {
            for (MobEffectInstance effect : ElixirUtils.getEffects(stack)) {
                MobEffect mobEffect = effect.getEffect().value();
                if (mobEffect.isInstantenous()) {

                    mobEffect.applyInstantenousEffect(player, player, livingEntity, effect.getAmplifier(), 1.0D);
                } else {
                    livingEntity.addEffect(new MobEffectInstance(effect));
                }
            }
        }

        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
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

        MutableComponent name = Component.translatable("rustic.elixir.prefix");
        List<MobEffectInstance> effects = ElixirUtils.getEffects(stack);
        for (int i = 0; i < effects.size(); i++) {
            name = name.append(Component.translatable(effects.get(i).getEffect().value().getDescriptionId()));
            if (i < effects.size() - 1) {
                name = name.append(Component.translatable("rustic.elixir.separator"));
            }
        }
        return name;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<MobEffectInstance> effects = ElixirUtils.getEffects(stack);
        if (effects.isEmpty()) {

            tooltip.add(Component.translatable("effect.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (MobEffectInstance effect : effects) {
            MobEffect mobEffect = effect.getEffect().value();
            MutableComponent text = Component.translatable(mobEffect.getDescriptionId());
            if (effect.getAmplifier() > 0) {
                text = text.append(" ").append(Component.translatable("potion.potency." + effect.getAmplifier()));
            }
            if (!effect.endsWithin(20)) {

                text = Component.translatable("potion.withDuration", text,
                        MobEffectUtil.formatDuration(effect, 1.0F, 20.0F));
            }

            tooltip.add(text.withStyle(mobEffect.getCategory() == MobEffectCategory.HARMFUL
                    ? ChatFormatting.RED : ChatFormatting.BLUE));
        }
    }
}
