package rustic.content.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from 1.12.2 {@code PotionFeather}: the feather-fall charm. Every tick, if the holder is
 * airborne and already falling faster than 0.4 blocks/tick, it nudges the downward velocity up by 0.1
 * and zeroes the accumulated fall distance — so a long drop never reaches fall-damage speed.
 *
 * <p>Legacy {@code entity.motionY += 0.1; entity.velocityChanged = true} becomes a
 * {@link Vec3}-based {@code setDeltaMovement}; 1.21 propagates the new velocity to riders/viewers on
 * its own, so there is no {@code velocityChanged} flag to set. Colour is legacy's {@code 14474460}.
 */
public final class FeatherEffect extends MobEffect {

    public FeatherEffect() {
        super(MobEffectCategory.BENEFICIAL, 14474460);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Vec3 motion = entity.getDeltaMovement();
        if (!entity.onGround() && motion.y < -0.4) {
            entity.setDeltaMovement(motion.x, motion.y + 0.1, motion.z);
            entity.resetFallDistance();
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Legacy isReady returned true: the slow-fall check runs every tick.
        return true;
    }
}
