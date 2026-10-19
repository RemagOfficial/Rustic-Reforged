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

public class EntityTomato extends ThrowableItemProjectile {

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

        return RusticItems.TOMATO.get();
    }

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

        if (result.getEntity() == this.getOwner()) {
            return;
        }
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity living) {

            living.addEffect(new MobEffectInstance(
                    RusticMobEffects.SHAME, SHAME_DURATION, SHAME_AMPLIFIER, false, true));
        }
    }

    @Override
    protected void onHit(HitResult result) {

        super.onHit(result);

        this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_ATTACK,
                this.getSoundSource(), 0.5F,
                0.4F / (this.random.nextFloat() * 0.4F + 0.8F), false);

        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }
}
