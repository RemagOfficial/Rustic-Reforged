package rustic.content.blocks;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockClayWallDiag extends BlockRotatable implements AdvancedRotationPlacement {

    public BlockClayWallDiag(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return getStateForAdvancedRotationPlacement(this.defaultBlockState(), FACING, context);
    }
}
