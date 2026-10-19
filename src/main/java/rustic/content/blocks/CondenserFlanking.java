package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class CondenserFlanking {

    private CondenserFlanking() {
    }

    static boolean hasRetorts(Level level, BlockPos pos, BlockState state, Block retort) {
        Direction facing = state.getValue(RetortBlock.FACING);
        return switch (facing) {
            case NORTH, SOUTH -> flanksCorrectly(level, pos, retort, Direction.EAST, Direction.WEST);
            case EAST, WEST -> flanksCorrectly(level, pos, retort, Direction.NORTH, Direction.SOUTH);
            default -> false;
        };
    }

    private static boolean flanksCorrectly(Level level, BlockPos pos, Block retort, Direction a, Direction b) {
        return retortFacing(level, pos.relative(a), retort, a)
                && retortFacing(level, pos.relative(b), retort, b);
    }

    private static boolean retortFacing(Level level, BlockPos flank, Block retort, Direction required) {
        BlockState flankState = level.getBlockState(flank);
        return flankState.is(retort) && flankState.getValue(RetortBlock.FACING) == required;
    }
}
