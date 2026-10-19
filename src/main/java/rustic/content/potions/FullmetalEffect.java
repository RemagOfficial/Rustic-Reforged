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

public final class FullmetalEffect extends MobEffect {

    private static final ResourceLocation MOVEMENT_SPEED = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fullmetal_movement_speed");
    private static final ResourceLocation FLYING_SPEED = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fullmetal_flying_speed");
    private static final ResourceLocation ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fullmetal_attack_speed");
    private static final ResourceLocation KNOCKBACK_RESISTANCE = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "fullmetal_knockback_resistance");

    public FullmetalEffect() {

        super(MobEffectCategory.BENEFICIAL, 8220521);

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

        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof LivingEntity) {
            entity.stopRiding();
        }

        if (!entity.onGround() && !entity.isNoGravity()) {
            Vec3 motion = entity.getDeltaMovement();
            double dy = motion.y;

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

        return true;
    }
}
