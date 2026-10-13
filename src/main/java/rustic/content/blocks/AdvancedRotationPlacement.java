package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;

/**
 * 1.12.2's {@code IAdvancedRotationPlacement}: pick which way a rotating block should look from
 * <em>where the player clicked</em> instead of only from which side they clicked.
 *
 * <p>Blocks whose model is asymmetric along the vertical axis (the diagonal clay wall today, and the
 * chairs and tables in later phases) cannot tell "which of the four horizontal directions" from the
 * clicked face alone, because clicking the top or bottom face only gives an up/down answer. The
 * legacy code resolved that by comparing how far from the centre of the face the click landed, and
 * that arithmetic is reproduced here unchanged.
 */
public interface AdvancedRotationPlacement {

    /**
     * The 1.12.2 method, verbatim apart from the types: {@code EnumFacing} becomes
     * {@link Direction}, {@code PropertyDirection} becomes {@link DirectionProperty}, and the
     * block-local hit coordinates are doubles because that is what {@link Vec3} stores.
     *
     * @param defaultState the state to start from
     * @param facing       the horizontal facing property to write the answer into
     * @param clickedFace  the face of the block that was clicked
     * @param hitX         click position inside the block, 0..1 along X
     * @param hitY         click position inside the block, 0..1 along Y
     * @param hitZ         click position inside the block, 0..1 along Z
     */
    default BlockState getStateForAdvancedRotationPlacement(BlockState defaultState, DirectionProperty facing,
                                                            Direction clickedFace, double hitX, double hitY, double hitZ) {
        if (clickedFace.getAxis() == Direction.Axis.Y) {
            double fromCenterX = hitX - 0.5D;
            double fromCenterZ = hitZ - 0.5D;

            if (Math.max(Math.abs(fromCenterX), Math.abs(fromCenterZ)) == Math.abs(fromCenterX)) {
                return defaultState.setValue(facing, fromCenterX > 0 ? Direction.EAST : Direction.WEST);
            }
            return defaultState.setValue(facing, fromCenterZ > 0 ? Direction.SOUTH : Direction.NORTH);
        }
        // A side click already says which way the player stood, so the block turns to meet them.
        return defaultState.setValue(facing, clickedFace.getOpposite());
    }

    /**
     * Adapts 1.21's {@link BlockPlaceContext} to the legacy signature: 1.12.2 handed the hit
     * coordinates over already block-local, while 1.21 only gives the absolute click point, so the
     * position of the clicked block has to be subtracted off.
     */
    default BlockState getStateForAdvancedRotationPlacement(BlockState defaultState, DirectionProperty facing,
                                                            BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Vec3 click = context.getClickLocation();
        return getStateForAdvancedRotationPlacement(defaultState, facing, context.getClickedFace(),
                click.x - pos.getX(), click.y - pos.getY(), click.z - pos.getZ());
    }
}
