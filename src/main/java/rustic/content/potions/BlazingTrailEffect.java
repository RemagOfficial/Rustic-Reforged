package rustic.content.potions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Ported from 1.12.2 {@code PotionBlazingTrail}: the walking-into-fire charm. Every tenth tick, if the
 * holder is standing on the ground with air at its own position and solid ground beneath, it ignites
 * that air block. Colour is legacy's {@code 16738816}.
 *
 * <p>Legacy {@code getBlockState(pos.down()).isNormalCube()} (a Forge 1.12 helper) becomes the 1.21
 * {@code isFaceSturdy(..., Direction.UP)} check on the block below — the nearest faithful reading of
 * "there is something solid enough to carry fire here". {@code setBlockState(pos, fire, 3)} maps to
 * {@code setBlockAndUpdate}, which uses the same update flags. Only ever ticks on the server, so no
 * side guard is needed.
 */
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
        // Legacy isReady: only every tenth remaining tick.
        return duration % 10 == 0;
    }
}
