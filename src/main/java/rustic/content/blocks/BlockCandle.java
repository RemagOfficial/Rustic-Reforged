package rustic.content.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class BlockCandle extends BlockCandleBase {

    protected static final VoxelShape STANDING_SHAPE = Block.box(6.4, 0.0, 6.4, 9.6, 15.0, 9.6);
    protected static final VoxelShape NORTH_SHAPE = Block.box(5.6, 0.0, 11.2, 10.4, 12.8, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(5.6, 0.0, 0.0, 10.4, 12.8, 4.8);
    protected static final VoxelShape WEST_SHAPE = Block.box(11.2, 0.0, 5.6, 16.0, 12.8, 10.4);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 5.6, 4.8, 12.8, 10.4);

    public BlockCandle(BlockBehaviour.Properties properties) {
        super(properties, shapes(), 0.27, 0.25, 0.33);
    }

    private static Map<Direction, VoxelShape> shapes() {
        return shapesFor(STANDING_SHAPE, NORTH_SHAPE, SOUTH_SHAPE, WEST_SHAPE, EAST_SHAPE);
    }
}
