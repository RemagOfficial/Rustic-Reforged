package rustic.content.potions;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rustic.content.items.RusticItems;

public final class ShameEffect extends MobEffect {

    public ShameEffect() {
        super(MobEffectCategory.HARMFUL, 16409650);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {

        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }

        int count = level.getRandom().nextInt(6) + 6;
        ItemStack tomato = new ItemStack(RusticItems.TOMATO.get());
        ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, tomato);

        double width = entity.getBbWidth();
        double height = entity.getBbHeight();

        level.sendParticles(particle,
                entity.getX(), entity.getY() + height / 2.0, entity.getZ(),
                count,
                width / 2.0, height / 2.0, width / 2.0,
                0.1);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {

        return duration % 5 == 0;
    }
}
