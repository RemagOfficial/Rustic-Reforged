package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The condenser multi-block's retort-flank rule, shared by {@link CondenserBlock} and
 * {@link CondenserAdvancedBlock} so the gate lives once instead of being mirrored into both classes.
 *
 * <p>Reproduced verbatim from 1.12.2 {@code BlockCondenser.hasRetorts}/{@code BlockCondenserAdvanced.hasRetorts}: a
 * condenser is only operational when its two side flanks - the blocks perpendicular to the way it faces - are retorts
 * of the matching tier, each turned so its front points <em>away</em> from the condenser (the still sits behind every
 * retort front, which is also why clicking a retort opens the machine at {@code facing.getOpposite()}).
 */
final class CondenserFlanking {

    private CondenserFlanking() {
    }

    /**
     * Whether {@code pos} (a condenser bottom half in {@code state}) is correctly flanked by two {@code retort}-tier
     * blocks facing outward. The condenser's {@code FACING} picks the flank axis: a still facing north/south is flanked
     * east/west, one facing east/west is flanked north/south.
     */
    static boolean hasRetorts(Level level, BlockPos pos, BlockState state, Block retort) {
        Direction facing = state.getValue(RetortBlock.FACING);
        return switch (facing) {
            case NORTH, SOUTH -> flanksCorrectly(level, pos, retort, Direction.EAST, Direction.WEST);
            case EAST, WEST -> flanksCorrectly(level, pos, retort, Direction.NORTH, Direction.SOUTH);
            default -> false;
        };
    }

    /**
     * Both flank positions along {@code a}/{@code b} must hold a {@code retort} whose front points outward along that
     * same direction: the block to the east faces east, the one to the north faces north, and so on.
     */
    private static boolean flanksCorrectly(Level level, BlockPos pos, Block retort, Direction a, Direction b) {
        return retortFacing(level, pos.relative(a), retort, a)
                && retortFacing(level, pos.relative(b), retort, b);
    }

    private static boolean retortFacing(Level level, BlockPos flank, Block retort, Direction required) {
        BlockState flankState = level.getBlockState(flank);
        return flankState.is(retort) && flankState.getValue(RetortBlock.FACING) == required;
    }
}
