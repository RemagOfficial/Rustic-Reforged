package rustic.content.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * 1.12.2's {@code BlockIronTorch}: the torch that hangs from a chain or clips to a wall, which the mod
 * built by copying {@code BlockCandle} and changing the numbers. It is the one torch in the game that
 * cannot be broken by hand - the legacy {@code setHardness(0F)}, which 1.21 still reads as "instantly
 * breakable by any tool".
 *
 * <p>The shape is shorter than a candle's because there is no wax column, only the iron cup and a stub
 * of flame, and the particles sit at the top of that cup rather than above it: hence the wall and
 * standing rises of 0.0, so the flame particle draws at the base offset of
 * {@code pos.getY() + 0.7} with nothing added.
 */
public class BlockIronTorch extends BlockCandleBase {

    protected static final VoxelShape STANDING_SHAPE = Block.box(6.4, 0.0, 6.4, 9.6, 11.2, 9.6);
    protected static final VoxelShape NORTH_SHAPE = Block.box(6.0, 0.0, 9.5, 10.0, 11.2, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 11.2, 6.5);
    protected static final VoxelShape WEST_SHAPE = Block.box(9.5, 0.0, 6.0, 16.0, 11.2, 10.0);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 6.0, 6.5, 11.2, 10.0);

    /**
     * {@code setHardness(0F)}, {@code Material.CIRCUITS}, {@code setSoundType(SoundType.METAL)} and
     * {@code setLightLevel(1.0F)} arrive as {@code lit(RusticBlockProperties.circuits(0.0F))}. The wall
     * offset is the legacy {@code 0.21875D}, a thirty-second of a block, which is how far the torch cup
     * reaches from the wall it is clipped to.
     */
    public BlockIronTorch(BlockBehaviour.Properties properties) {
        super(properties, shapes(), 0.21875, 0.0, 0.0);
    }

    private static Map<Direction, VoxelShape> shapes() {
        return shapesFor(STANDING_SHAPE, NORTH_SHAPE, SOUTH_SHAPE, WEST_SHAPE, EAST_SHAPE);
    }
}
