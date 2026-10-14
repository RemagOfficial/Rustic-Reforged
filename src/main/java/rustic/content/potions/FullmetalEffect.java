package rustic.content.potions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;
import rustic.Rustic;

/**
 * Ported from 1.12.2 {@code PotionFullmetal}: the "lead" effect. It turns the afflicted into an
 * immovable, attack-speed-slashed, knockback-proof statue that sinks instead of flying or swimming,
 * and it refuses jumps/mounts/sleep/fall elsewhere (those live in the potion event handler).
 *
 * <p>Attribute rework — the deliberate deviations from the legacy class:
 * <ul>
 *   <li>Legacy {@code registerPotionAttributeModifier} used string UUIDs; 1.21 takes a
 *       {@link ResourceLocation}, so each modifier now has a stable {@code rustic:fullmetal_*} id.</li>
 *   <li>Legacy overrode {@code getAttributeModifierAmount} to return the raw amount (never scaled by
 *       amplifier). 1.21's plain {@code addAttributeModifier(attr, id, amount, op)} multiplies by
 *       {@code amplifier + 1}, so the flat values are supplied through NeoForge's curve overload with a
 *       constant function — that reproduces the "always exactly -1 / -0.5 / +9001" behaviour.</li>
 *   <li>Legacy registered a {@code SWIM_SPEED} modifier (a Forge-added attribute 1.12.2 had and 1.21
 *       does not). 1.21 swimming reads {@code MOVEMENT_SPEED}, which is already forced to zero, so the
 *       separate swim modifier is dropped and submersion is handled by the downward push below.</li>
 *   <li>The manual {@code SPacketEntityEffect}/{@code SPacketEntityProperties} tracker broadcasts in
 *       the legacy add/remove overrides are gone: 1.21 syncs effect attribute modifiers automatically
 *       via the entity attribute packet, so {@code MobEffect} needs no hand-written sync.</li>
 * </ul>
 *
 * <p>The per-tick body of legacy {@code performEffect} maps onto {@link #applyEffectTick}: sprint off,
 * dismount a living vehicle, extra downward acceleration while airborne, and force the creative
 * fly ability off. {@link #shouldApplyEffectTickThisTick} returns true every tick exactly as legacy's
 * {@code isReady} did.
 */
public final class FullmetalEffect extends MobEffect {

    private static final ResourceLocation MOVEMENT_SPEED = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fullmetal_movement_speed");
    private static final ResourceLocation FLYING_SPEED = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fullmetal_flying_speed");
    private static final ResourceLocation ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fullmetal_attack_speed");
    private static final ResourceLocation KNOCKBACK_RESISTANCE = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fullmetal_knockback_resistance");

    public FullmetalEffect() {
        // 1.12.2 PotionBase(false, 8220521, "fullmetal") -> beneficial, same colour.
        super(MobEffectCategory.BENEFICIAL, 8220521);
        // Every amount is flat (constant curve), matching the legacy getAttributeModifierAmount override.
        addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> -1.0);
        addAttributeModifier(Attributes.FLYING_SPEED, FLYING_SPEED,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> -1.0);
        addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> -0.5);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE,
                AttributeModifier.Operation.ADD_VALUE, amplifier -> 9001.0);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setSprinting(false);

        // Legacy only dismounted when the vehicle was a living entity; getVehicle() is Entity-typed in 1.21.
        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof LivingEntity) {
            entity.stopRiding();
        }

        if (!entity.onGround() && !entity.isNoGravity()) {
            Vec3 motion = entity.getDeltaMovement();
            double dy = motion.y;
            // Legacy world.isInWaterRainOrBubbleColumn-or-lava reading; 1.21 splits it into the two media.
            if (entity.isInWater() || entity.isInLava()) {
                dy -= 0.27 / 4;
            } else if (entity.isFallFlying()) {
                dy -= 0.32 / 4;
            } else {
                dy -= 0.07;
            }
            entity.setDeltaMovement(motion.x, dy, motion.z);
        }

        if (entity instanceof ServerPlayer player) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Legacy isReady returned true unconditionally: the sink/push runs every tick.
        return true;
    }
}
