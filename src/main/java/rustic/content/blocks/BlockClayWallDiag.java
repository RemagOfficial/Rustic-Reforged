package rustic.content.blocks;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12.2's {@code BlockClayWallDiag}: a full cube whose four sides are not all the same, so which
 * way it faces has to be stored and worked out at placement time.
 *
 * <p>The legacy class extended {@code BlockBase} and re-implemented the {@code facing} property, its
 * metadata encoding and its rotation/mirroring by hand. All of that is now inherited from
 * {@link BlockRotatable}; only the placement rule is still Rustic's own, because a diagonal wall must
 * use the click position rather than just the clicked face - see {@link AdvancedRotationPlacement}.
 *
 * <p>The block's asymmetry lives entirely in its model (north/south/top/bottom use the plain clay
 * wall texture, east/west use the diagonal one), which is why no shape or collision code is needed
 * here: it occupies a full cube exactly like the plain wall does.
 */
public class BlockClayWallDiag extends BlockRotatable implements AdvancedRotationPlacement {

    public BlockClayWallDiag(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** 1.12.2: {@code getStateForAdvancedRotationPlacement(getDefaultState(), FACING, facing, hitX, hitY, hitZ)} */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return getStateForAdvancedRotationPlacement(this.defaultBlockState(), FACING, context);
    }
}
