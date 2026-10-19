package rustic.content.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

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

        return true;
    }
}
