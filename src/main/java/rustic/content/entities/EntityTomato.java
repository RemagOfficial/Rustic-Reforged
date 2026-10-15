package rustic.content.entities;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import rustic.content.items.RusticItems;
import rustic.content.potions.RusticMobEffects;

/**
 * 1.12.2's {@code EntityTomato}: the thrown tomato, a splat of {@code SHAME} on any living thing it hits.
 *
 * <p>The legacy base was {@code EntityThrowable}, whose 1.21 descendant is {@link ThrowableItemProjectile}:
 * the plan named plain {@code ThrowableProjectile}, but the item-carrying subclass is what vanilla's own
 * snowball and egg extend, and it is what {@code ThrownItemRenderer} can render without a bespoke
 * {@code ItemSupplier} implementation - the 1.12.2 renderer drew {@code ModItems.TOMATO} as a fixed item,
 * which is exactly the synced default {@code Item} stack this base carries. Gravity (0.03), water drag
 * (0.8) and air drag (0.99) already match legacy {@code EntityThrowable}'s numbers in
 * {@code ThrowableProjectile.tick}, so no motion arithmetic is ported.
 *
 * <p>Faithful legacy details:
 * <ul>
 * <li>the hit is 0 damage plus {@code PotionEffect(SHAME, 400, 0, false, false)} on any
 * {@code LivingEntity} that is not the thrower - the {@code attackEntityFrom} with the {@code 0} the
 * legacy code left in after the damage constant was zeroed;
 * <li>impact plays the slime-hurt sound (volume 0.5, pitch 0.4/(rand*0.4+0.8)) and the splat particle
 * event even when the victim IS the thrower - legacy skipped the damage there but not the sound, and
 * the event only ever broadcast to tracking clients, never back to the thrower as a self-hit;
 * <li>the burst is {@code rand.nextInt(8) + 4} ITEM_CRACK particles at ±0.03 velocity, the legacy
 * handleStatusUpdate line verbatim (vanilla's egg uses a flat 8 and ±0.04; the legacy numbers win).
 * </ul>
 *
 * <p>Deviations, all cosmetic engine swaps: the 1.12.2 particle call passed the tomato item id as the
 * extra parameter; 1.21 needs the {@link ItemParticleOption} wrapper. {@code setDead()} is
 * {@code discard()}. The impact sound plays on both sides through {@code playLocalSound} because
 * {@code Level.playSound(null, ...)} has no client-side equivalent in 1.21 - the legacy call did the
 * same thing, reaching everyone in earshot including the thrower.
 */
public class EntityTomato extends ThrowableItemProjectile {

    /** 1.12.2's {@code new PotionEffect(PotionsRustic.SHAME_POTION, 400, 0, false, false)}. */
    private static final int SHAME_DURATION = 400;
    private static final int SHAME_AMPLIFIER = 0;

    public EntityTomato(EntityType<? extends EntityTomato> entityType, Level level) {
        super(entityType, level);
    }

    public EntityTomato(Level level, LivingEntity shooter) {
        super(RusticEntities.TOMATO.get(), shooter, level);
    }

    public EntityTomato(Level level, double x, double y, double z) {
        super(RusticEntities.TOMATO.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        // RenderTomato.Factory handed the renderer ModItems.TOMATO outright; here the same item arrives
        // as the projectile's synced default stack.
        return RusticItems.TOMATO.get();
    }

    /**
     * The legacy {@code handleStatusUpdate(3)}: the client-side splat of tomato-item crack particles.
     * {@code broadcastEntityEvent} on the server reaches every tracking client except the thrower for a
     * self-hit, matching the legacy {@code world.setEntityState} semantics.
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(getDefaultItem()));
            for (int i = 0; i < this.random.nextInt(8) + 4; i++) {
                this.level().addParticle(particle,
                        this.getX(), this.getY(), this.getZ(),
                        this.random.nextDouble() * 0.06 - 0.03,
                        this.random.nextDouble() * 0.06 - 0.03,
                        this.random.nextDouble() * 0.06 - 0.03);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        // Legacy: damage + shame only when the victim is not the thrower; 0 damage, amplifier 0.
        if (result.getEntity() == this.getOwner()) {
            return;
        }
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity living) {
            // The legacy PotionEffect's two flags are ambient=false, showParticles(false -> visible true).
            living.addEffect(new MobEffectInstance(
                    RusticMobEffects.SHAME, SHAME_DURATION, SHAME_AMPLIFIER, false, true));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        // Projectile.onHit already routes entity hits to onHitEntity (the legacy onImpact did both in
        // one method); only the shared impact sound and the splat/discard half are left to port. The
        // sound fires even when the victim is the thrower - legacy skipped the damage there, not this.
        super.onHit(result);

        // The legacy playSound(null, ...) - volume 0.5, the snowball-throw pitch formula.
        this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_ATTACK,
                this.getSoundSource(), 0.5F,
                0.4F / (this.random.nextFloat() * 0.4F + 0.8F), false);

        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }
}
