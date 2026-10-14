package rustic.content.potions;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rustic.content.items.RusticItems;

/**
 * Ported from 1.12.2 {@code PotionShame}: the splat-of-tomato humiliation the thrown tomato inflicts.
 * Every fifth tick it bursts a handful of tomato item-break particles over the afflicted's hitbox.
 * Colour is legacy's {@code 16409650}, and it is a HARMFUL effect.
 *
 * <p>Legacy did not spawn the particles directly — {@code performEffect} broadcast a
 * {@code MessageShameFX} packet to every client and the client turned it into {@code ITEM_CRACK}
 * particles, because 1.12.2 had no server-to-clients particle helper. 1.21's
 * {@link ServerLevel#sendParticles} does exactly that (the server emits, the engine syncs to tracking
 * clients), so the whole custom packet is gone: this is the recorded deviation from the legacy design,
 * and it removes one message from the network phase's todo list.
 */
public final class ShameEffect extends MobEffect {

    public ShameEffect() {
        super(MobEffectCategory.HARMFUL, 16409650);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // sendParticles lives on ServerLevel; effect ticks only run on the logical server anyway.
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        // MessageShameFX chose rand.nextInt(6) + 6 particles per burst; reproduced here.
        int count = level.getRandom().nextInt(6) + 6;
        ItemStack tomato = new ItemStack(RusticItems.TOMATO.get());
        ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, tomato);

        double width = entity.getBbWidth();
        double height = entity.getBbHeight();
        // Legacy scattered each particle across a random face of the entity box; a spread volume that
        // covers the same box is the 1.21 equivalent and needs no per-particle loop.
        level.sendParticles(particle,
                entity.getX(), entity.getY() + height / 2.0, entity.getZ(),
                count,
                width / 2.0, height / 2.0, width / 2.0,
                0.1);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Legacy isReady: every fifth remaining tick.
        return duration % 5 == 0;
    }
}
