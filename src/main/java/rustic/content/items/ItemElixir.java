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

/**
 * The elixir, ported from 1.12.2's {@code rustic.common.items.ItemElixir}: a stackable (16) drinkable that
 * carries an arbitrary, ordered list of effects in the {@code rustic:elixir_effects} data component (see
 * {@link ElixirUtils}) instead of the legacy {@code "ElixirEffects"} NBT list.
 *
 * <p><b>Tab entries.</b> Legacy overrode {@code getSubItems} to list every condenser recipe result; 1.21 has no
 * {@code getSubItems} hook and creative tabs cannot read the recipe map at display time, so
 * {@link #condenserElixirs()} mirrors those results in the legacy {@code Recipes.addCondenserRecipes} order - the
 * same rows {@code RusticRecipeProvider.addCondenser} generates, pinned against that data by
 * {@code RusticAlchemyTabTest} so the two cannot drift. The item tint (legacy {@code IColoredItem} via
 * {@code ElixirUtils.getColor}) landed in Phase 17 S17.4 as an {@code ItemColors} handler in {@code RusticClient};
 * the effect-name, potency and duration lines below
 * mirror both legacy {@code ElixirUtils.addPotionTooltip} and vanilla {@code PotionContents.addPotionTooltip};
 * the {@code potion.whenDrank} attribute block, which legacy also printed for attribute-granting effects such
 * as health boost, stays deferred - no condenser elixir effect carries an attribute on 1.21, so it would
 * print nothing.
 */
public class ItemElixir extends Item {

    public ItemElixir(Properties properties) {
        // The legacy setMaxStackSize(16) - elixirs stack, potions do not.
        super(properties.stacksTo(16));
    }

    /** The alchemy-tab icon, the 1.21 form of the legacy {@code getDefaultInstance} (Instant Health, 1 tick). */
    public static ItemStack defaultElixir() {
        ItemStack stack = new ItemStack(RusticItems.ELIXIR.get());
        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 0), stack);
        return stack;
    }

    /**
     * Every condenser elixir, in the legacy {@code Recipes.addCondenserRecipes} order - the 1.21 form of the
     * legacy {@code ItemElixir.getSubItems} walk over {@code Recipes.condenserRecipes}, which fed the alchemy
     * tab its thirty-one elixir entries (the first, Instant Health I, is also {@link #defaultElixir()}). The
     * triples below mirror the {@code condense} rows of {@code RusticRecipeProvider.addCondenser} one-for-one;
     * {@code RusticAlchemyTabTest} decodes the generated {@code rustic:condenser} recipe data and fails if
     * either side drifts. Fresh stacks per call: a creative-tab entry must not be a shared mutable instance.
     */
    public static List<ItemStack> condenserElixirs() {
        List<MobEffectInstance> effects = List.of(
                // Healing (instant health).
                new MobEffectInstance(MobEffects.HEAL, 1, 0),
                new MobEffectInstance(MobEffects.HEAL, 1, 1),
                // Regeneration.
                new MobEffectInstance(MobEffects.REGENERATION, 900, 0),
                new MobEffectInstance(MobEffects.REGENERATION, 1800, 0),
                new MobEffectInstance(MobEffects.REGENERATION, 450, 1),
                // Wither.
                new MobEffectInstance(MobEffects.WITHER, 900, 0),
                new MobEffectInstance(MobEffects.WITHER, 1800, 0),
                new MobEffectInstance(MobEffects.WITHER, 450, 1),
                // Night vision.
                new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0),
                new MobEffectInstance(MobEffects.NIGHT_VISION, 9600, 0),
                // Speed.
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 9600, 0),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 1),
                // Fire resistance.
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0),
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 9600, 0),
                // Health boost.
                new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 0),
                new MobEffectInstance(MobEffects.HEALTH_BOOST, 9600, 0),
                new MobEffectInstance(MobEffects.HEALTH_BOOST, 1800, 1),
                // Haste.
                new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0),
                new MobEffectInstance(MobEffects.DIG_SPEED, 9600, 0),
                new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 1),
                // Strength.
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0),
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9600, 0),
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800, 1),
                // Iron skin (custom effect).
                new MobEffectInstance(RusticMobEffects.IRON_SKIN, 3600, 0),
                new MobEffectInstance(RusticMobEffects.IRON_SKIN, 9600, 0),
                new MobEffectInstance(RusticMobEffects.IRON_SKIN, 1800, 1),
                // Feather (custom effect).
                new MobEffectInstance(RusticMobEffects.FEATHER, 3600, 0),
                new MobEffectInstance(RusticMobEffects.FEATHER, 9600, 0),
                // Blazing trail (custom effect).
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

    // ------------------------------------------------------------------ drinking

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Always drinkable, even with no effects stored - the legacy onItemRightClick had no guard either
        // (the "water bottle" elixir), and only starts the use, like ItemFluidBottle's drinkable check.
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        // Legacy getMaxItemUseDuration: 24 ticks, a quarter slower than the 32-tick fluid bottle.
        return 24;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        Player player = livingEntity instanceof Player p ? p : null;
        if (!level.isClientSide) {
            for (MobEffectInstance effect : ElixirUtils.getEffects(stack)) {
                MobEffect mobEffect = effect.getEffect().value();
                if (mobEffect.isInstantenous()) {
                    // The legacy affectEntity(attacker, source, target, amplifier, 1.0D) pair.
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

        // Creative keeps the elixir; otherwise the glass-bottle container comes back, dropped whole when the
        // last one was drunk - the same branch structure as ItemFluidBottle and the legacy shrink/givePlayerItem.
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
        // "Elixir of <A>, <B>" - legacy getItemStackDisplayName: prefix, then every stored effect name
        // joined by the lang separator, straight off the pinned rustic.elixir.* keys.
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
            // Legacy addPotionTooltip's empty branch: the vanilla "effect.none" line in gray.
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
                // Legacy: s1 += " (" + Potion.getPotionDurationString(effect, 1.0F) + ")" - instant effects
                // (HEAL, 1 tick) stay bare, timed ones get the vanilla "potion.withDuration" "(M:SS)" tail.
                // formatDuration's third argument is ticks-per-second: 20.0F reproduces the legacy division.
                text = Component.translatable("potion.withDuration", text,
                        MobEffectUtil.formatDuration(effect, 1.0F, 20.0F));
            }
            // The legacy bad-effect red / good-effect blue split.
            tooltip.add(text.withStyle(mobEffect.getCategory() == MobEffectCategory.HARMFUL
                    ? ChatFormatting.RED : ChatFormatting.BLUE));
        }
    }
}
