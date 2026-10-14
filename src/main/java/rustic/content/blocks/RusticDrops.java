package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The two lines almost every 1.12.2 Rustic block repeated when it decided to remove itself: drop the
 * block as an item, turn the position to air, and play the block's own sound.
 *
 * <p>{@code dropBlockAsItem(world, pos, state, 0)} becomes {@link Block#dropResources(BlockState,
 * LevelAccessor, BlockPos, net.minecraft.world.level.block.entity.BlockEntity)}, which routes through
 * the block's loot table like the legacy call did - and, like it, does nothing at all unless the accessor
 * is a server level, so the same call is safe during world generation.
 * <ol>
 *   <li>{@link #dropAsItem} is the half that belongs inside {@code updateShape}, which cannot remove its
 *   own block but simply returns {@code AIR} and lets the caller do that;</li>
 *   <li>{@link #removeAndDrop} is the whole legacy pair, for a block that removes itself from a hook that
 *   has no return value - {@code onPlace}, or a right-click.</li>
 * </ol>
 *
 * <p>The volume and pitch arithmetic is kept here once because every Rustic block that played its own
 * break sound used the same pair: half of {@code volume + 1} and 0.8 of the pitch, at the corner of the
 * block rather than its centre, because that is where 1.12.2 passed the raw block coordinates.
 */
public final class RusticDrops {

    private RusticDrops() {
    }

    /**
     * Drop the block as an item and play its break sound, leaving the position itself to the caller -
     * which in practice is {@code updateShape} returning {@code Blocks.AIR}.
     *
     * <p>Split out from {@link #removeAndDrop} because clearing the position from inside
     * {@code updateShape} would be a re-entrant {@code setBlock} in the middle of another one.
     */
    public static void dropAsItem(LevelAccessor level, BlockPos pos, BlockState state) {
        Block.dropResources(state, level, pos, null);
        if (level instanceof Level realLevel) {
            // updateShape also runs inside a world generation region, which cannot play sounds at all.
            SoundType soundType = state.getSoundType();
            playSound(realLevel, pos, soundType, soundType.getBreakSound());
        }
    }

    /** 1.12.2's {@code dropBlock}: drop, clear the position, then play the break sound. */
    public static void removeAndDrop(LevelAccessor level, BlockPos pos, BlockState state) {
        dropAsItem(level, pos, state);
        if (level instanceof Level realLevel) {
            realLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    public static void playPlaceSound(Level level, BlockPos pos, BlockState state) {
        SoundType soundType = state.getSoundType();
        playSound(level, pos, soundType, soundType.getPlaceSound());
    }

    public static void playSound(Level level, BlockPos pos, SoundType soundType, SoundEvent event) {
        level.playSound(
                null,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                event,
                SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F
        );
    }
}
