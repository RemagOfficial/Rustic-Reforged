package rustic.content.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class TipsyEffect extends MobEffect {

    public TipsyEffect() {
        super(MobEffectCategory.HARMFUL, 7900290);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {

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

        return amplifier > 1 && duration % 100 == 0;
    }
}
