package rustic.content.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class BlockIronTorch extends BlockCandleBase {

    protected static final VoxelShape STANDING_SHAPE = Block.box(6.4, 0.0, 6.4, 9.6, 11.2, 9.6);
    protected static final VoxelShape NORTH_SHAPE = Block.box(6.0, 0.0, 9.5, 10.0, 11.2, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 11.2, 6.5);
    protected static final VoxelShape WEST_SHAPE = Block.box(9.5, 0.0, 6.0, 16.0, 11.2, 10.0);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 6.0, 6.5, 11.2, 10.0);

    public BlockIronTorch(BlockBehaviour.Properties properties) {
        super(properties, shapes(), 0.21875, 0.0, 0.0);
    }

    private static Map<Direction, VoxelShape> shapes() {
        return shapesFor(STANDING_SHAPE, NORTH_SHAPE, SOUTH_SHAPE, WEST_SHAPE, EAST_SHAPE);
    }
}
