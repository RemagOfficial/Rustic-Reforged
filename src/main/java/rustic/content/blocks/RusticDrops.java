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

public final class RusticDrops {

    private RusticDrops() {
    }

    public static void dropAsItem(LevelAccessor level, BlockPos pos, BlockState state) {
        Block.dropResources(state, level, pos, null);
        if (level instanceof Level realLevel) {

            SoundType soundType = state.getSoundType();
            playSound(realLevel, pos, soundType, soundType.getBreakSound());
        }
    }

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
