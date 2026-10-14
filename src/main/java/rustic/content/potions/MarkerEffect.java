package rustic.content.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The 1.21 form of 1.12.2's {@code PotionBase} for the five effects that carry no per-tick logic of
 * their own — {@code fire_power}, {@code full}, {@code magic_resistance}, {@code wither_ward} and
 * {@code undying}. In 1.12.2 those were built inline as {@code new PotionBase(false, color, name)} and
 * every bit of behaviour lived in {@code EventHandlerPotions}; the split is identical here, so this
 * class only exists to make those five one-liners readable and to record the colour the legacy
 * {@code setPotionName}/{@code Potion(isBadEffect, color)} pair carried.
 *
 * <p>The legacy status-icon index ({@code setIconIndex}) is not part of the effect any more: 1.21 draws
 * a {@code MobEffect} icon through the client-extensions sprite API, so the icon mapping is deferred to
 * Phase 9 alongside the potion-icon texture sheet, exactly as {@code RusticMobEffects} records.
 */
public final class MarkerEffect extends MobEffect {

    public MarkerEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
