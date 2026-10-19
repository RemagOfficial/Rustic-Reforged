package rustic.content.potions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class BlazingTrailEffect extends MobEffect {

    public BlazingTrailEffect() {
        super(MobEffectCategory.BENEFICIAL, 16738816);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        BlockPos feet = entity.blockPosition();
        BlockPos ground = feet.below();
        if (entity.onGround()
                && level.isEmptyBlock(feet)
                && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
            level.setBlockAndUpdate(feet, Blocks.FIRE.defaultBlockState());
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {

        return duration % 10 == 0;
    }
}
