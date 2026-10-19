package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;

public interface AdvancedRotationPlacement {

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

        return defaultState.setValue(facing, clickedFace.getOpposite());
    }

    default BlockState getStateForAdvancedRotationPlacement(BlockState defaultState, DirectionProperty facing,
                                                            BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Vec3 click = context.getClickLocation();
        return getStateForAdvancedRotationPlacement(defaultState, facing, context.getClickedFace(),
                click.x - pos.getX(), click.y - pos.getY(), click.z - pos.getZ());
    }
}
