package rustic.content.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ported from 1.12.2 {@code PotionTipsy}: the drunkness the booze fluids inflict. Once the amplifier
 * climbs past 1 it re-infects the holder with nausea + slowness (and blindness above amp 2), each for
 * 400 ticks, on a 100-tick cadence so the sub-effects refresh before they lapse. Colour is legacy's
 * {@code 7900290}, and it is a HARMFUL effect ({@code isBadEffect true}).
 *
 * <p>The legacy tail of {@code performEffect} cleared the sub-effects' {@code curativeItems} so milk
 * would not remove them — a Forge 1.12.2 {@code MobEffectInstance} concept that does not exist in 1.21,
 * where milk unconditionally strips every effect. There is no per-effect cure suppression to port, so
 * that whole block is dropped: drinking milk now also clears the tipsy nausea/slowness, a small and
 * acceptable behaviour change recorded here rather than worked around.
 */
public final class TipsyEffect extends MobEffect {

    public TipsyEffect() {
        super(MobEffectCategory.HARMFUL, 7900290);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Legacy gated the whole body on !world.isRemote; applyEffectTick only ever runs on the logical
        // server (the client never ticks effect application), so the side test is unnecessary here.
        if (amplifier > 2) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 1, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 1, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 0, false, false));
        } else {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 0, false, false));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Legacy isReady: only above amp 1, and only every hundredth remaining tick.
        return amplifier > 1 && duration % 100 == 0;
    }
}
