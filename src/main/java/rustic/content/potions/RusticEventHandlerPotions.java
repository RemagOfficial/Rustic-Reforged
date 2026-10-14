package rustic.content.potions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;

import java.util.Random;

/**
 * The potion/effect behaviour handler, ported from 1.12.2's {@code rustic.common.potions.EventHandlerPotions}.
 *
 * <p>This is the single home for the logic that the legacy marker effects deferred here (per the Phase 6 plan):
 * fullmetal damage immunity, FULL starvation soak, MAGIC_RESISTANCE / WITHER_WARD halving, UNDYING revival, the
 * fullmetal mount/sleep/jump/fall restrictions and the water-bottle sobering. It is registered as an <em>instance</em>
 * on the NeoForge game bus from {@code Rustic#commonSetup} (the five behavioural effects carry their own logic in
 * their {@code MobEffect} subclasses and are not handled here).
 *
 * <p><b>Event mapping.</b> Legacy {@code LivingHurtEvent -> LivingIncomingDamageEvent}, {@code LivingDeathEvent} and
 * {@code LivingFallEvent} keep their names, {@code EntityMountEvent} keeps its name, {@code
 * PlayerSleepInBedEvent -> CanPlayerSleepEvent} ({@code SleepResult -> BedSleepingProblem}), {@code
 * LivingEvent.LivingJumpEvent} is now {@code LivingEvent.LivingJumpEvent} (a static inner class) and {@code
 * LivingEntityUseItemEvent.Finish} is unchanged.
 *
 * <p><b>Damage-source mapping.</b> The 1.12.2 {@code DamageSource.STARVE}/{@code MAGIC}/{@code WITHER} constants and
 * the {@code OUT_OF_WORLD}/{@code canHarmInCreative()} checks are gone in 1.21. Damage identity is now read through
 * the registry key or a tag: {@link DamageTypes#STARVE}, {@link DamageTypes#MAGIC}, {@link DamageTypes#WITHER} and
 * {@link DamageTypes#FELL_OUT_OF_WORLD}. "This damage even reaches a creative/invulnerable target" - which is what
 * {@code canHarmInCreative()} encoded - is expressed by the {@link DamageTypeTags#BYPASSES_INVULNERABILITY} tag, so
 * fullmetal and undying keep that carve-out faithfully.
 */
public class RusticEventHandlerPotions {

    private final Random rand = new Random();

    // ------------------------------------------------------------- sobering up
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWaterBottleUse(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = entity.getEffect(RusticMobEffects.TIPSY);
        if (effect != null && !entity.level().isClientSide) {
            ItemStack stack = event.getItem();
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (stack.is(Items.POTION) && contents != null && contents.is(Potions.WATER)) {
                int duration = effect.getDuration();
                int amplifier = effect.getAmplifier();
                if (this.rand.nextFloat() < 0.1F) {
                    amplifier--;
                } else {
                    duration -= (this.rand.nextInt(800) + 200);
                }
                entity.removeEffect(RusticMobEffects.TIPSY);
                if (amplifier >= 0 && duration > 0) {
                    entity.addEffect(new MobEffectInstance(RusticMobEffects.TIPSY, duration, amplifier, false, false), null);
                }
            }
        }
    }

    // ------------------------------------------------------------- fullmetal restrictions
    @SubscribeEvent
    public void onJumpEvent(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(RusticMobEffects.FULLMETAL)) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
    }

    @SubscribeEvent
    public void onMountEvent(EntityMountEvent event) {
        if (!event.isMounting()) {
            return;
        }
        if (event.getEntityMounting() instanceof LivingEntity top && top.hasEffect(RusticMobEffects.FULLMETAL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onSleepEvent(CanPlayerSleepEvent event) {
        if (event.getEntity().hasEffect(RusticMobEffects.FULLMETAL)) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @SubscribeEvent
    public void onFallEvent(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        float distance = event.getDistance();
        if (entity.hasEffect(RusticMobEffects.FULLMETAL) && distance >= 0.6F && !entity.isInWater() && !entity.isInLava()) {
            entity.playSound(SoundEvents.ANVIL_LAND, 1.0F, 1.0F);
        }
    }

    // ------------------------------------------------------------- damage modification
    @SubscribeEvent
    public void onEntityDamage(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        LivingEntity entity = event.getEntity();

        if (entity.hasEffect(RusticMobEffects.FULLMETAL)
                && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setAmount(0.0F);
            return;
        }

        if (source.is(DamageTypes.STARVE)) {
            MobEffectInstance effect = entity.getEffect(RusticMobEffects.FULL);
            if (effect != null) {
                event.setAmount(Math.max(event.getAmount() - (effect.getAmplifier() + 1), 0.0F));
            }
        } else if (source.is(DamageTypes.MAGIC)) {
            MobEffectInstance effect = entity.getEffect(RusticMobEffects.MAGIC_RESISTANCE);
            if (effect != null) {
                event.setAmount(event.getAmount() / (2.0F * (effect.getAmplifier() + 1.0F)));
            }
        } else if (source.is(DamageTypes.WITHER)) {
            MobEffectInstance effect = entity.getEffect(RusticMobEffects.WITHER_WARD);
            if (effect != null) {
                event.setAmount(event.getAmount() / (2.0F * (effect.getAmplifier() + 1.0F)));
            }
        }
    }

    // ------------------------------------------------------------- undying
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityDeath(LivingDeathEvent event) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return; // legacy was player-only too: TODO enable for non-players?
        }

        MobEffectInstance effect = player.getEffect(RusticMobEffects.UNDYING);
        if (effect == null) {
            return;
        }
        player.removeEffect(RusticMobEffects.UNDYING);

        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1), null);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1), null);
        // The legacy setEntityState(p, (byte) 35) client animation cue is dropped: it has no 1.21 equivalent and the
        // phase gate parks cosmetic revive VFX (Phase 9), the health/effect restoration above is the load-bearing part.

        if (effect.getAmplifier() > 0) {
            player.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    effect.getDuration(),
                    effect.getAmplifier() - 1,
                    effect.isAmbient(),
                    effect.isVisible()), null);
        }

        event.setCanceled(true);
    }

    // ------------------------------------------------------------- fire power attack
    /**
     * Server-side fire-power "throw a fireball by punching", the body of legacy {@code EventHandlerPotions
     * .doFirePowerAttack}, reached from {@code RusticPayloads}' server handler once the client sends its left-click.
     * The position/motion arithmetic and the amplifier-as-explosion-power mapping are kept faithful; the legacy
     * {@code getLookVec()}/{@code motionX}/{@code onGround} read as {@code getLookAngle()}/{@code getDeltaMovement()}
     * /{@code onGround()}, {@code Material.WATER} as the {@link FluidTags#WATER} fluid state, and {@code
     * spawnEntity} as {@link ServerLevel#addFreshEntity}. The pre-spawn block-collision early-return is omitted: 1.21
     * has no {@code Level#checkBlockCollision} and a fireball fired into a wall simply hits it, a Phase 9 nicety.
     */
    public static void doFirePowerAttack(ServerPlayer player) {
        MobEffectInstance effect = player.getEffect(RusticMobEffects.FIRE_POWER);
        if (effect == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        FluidState head = level.getFluidState(net.minecraft.core.BlockPos.containing(eye.x, eye.y, eye.z));
        if (head.is(FluidTags.WATER)) {
            return;
        }

        double gaussianScale = 0.005D;
        Vec3 direction = new Vec3(
                look.x * 40.0D + level.getRandom().nextGaussian() * gaussianScale,
                look.y * 40.0D,
                look.z * 40.0D + level.getRandom().nextGaussian() * gaussianScale);

        Fireball fireball;
        if (effect.getAmplifier() > 0) {
            fireball = new LargeFireball(level, player, direction, effect.getAmplifier());
        } else {
            fireball = new SmallFireball(level, player, direction);
        }

        fireball.setPos(eye.x + look.x, eye.y + look.y, eye.z + look.z);
        Vec3 playerMotion = player.getDeltaMovement();
        Vec3 motion = fireball.getDeltaMovement().add(playerMotion.x, player.onGround() ? 0.0D : playerMotion.y, playerMotion.z);
        fireball.setDeltaMovement(motion);

        level.playSound(null, eye.x + look.x, eye.y + look.y, eye.z + look.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 1.0F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 1.2F));

        level.addFreshEntity(fireball);
    }
}
