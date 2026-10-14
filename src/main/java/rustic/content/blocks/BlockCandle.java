package rustic.content.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * 1.12.2's {@code BlockCandle}: a thin candle on a metal plate, standing on a surface or clipped to
 * four different walls. Gold- and silver-tinted copies were separate registry ids built from the same
 * class, which is why the block takes no colour itself - the textures differ, the behaviour does not.
 *
 * <p>Everything except these five boxes and the three particle offsets is in {@link BlockCandleBase}.
 * The boxes are the legacy {@code AxisAlignedBB} constants multiplied by 16, because 1.21's
 * {@link Block#box} works in sixteenths: the standing candle's {@code 0.4, 0.0, 0.4, 0.6, 0.9375, 0.6}
 * becomes {@code 6.4, 0.0, 6.4, 9.6, 15.0, 9.6}.
 */
public class BlockCandle extends BlockCandleBase {

    protected static final VoxelShape STANDING_SHAPE = Block.box(6.4, 0.0, 6.4, 9.6, 15.0, 9.6);
    protected static final VoxelShape NORTH_SHAPE = Block.box(5.6, 0.0, 11.2, 10.4, 12.8, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(5.6, 0.0, 0.0, 10.4, 12.8, 4.8);
    protected static final VoxelShape WEST_SHAPE = Block.box(11.2, 0.0, 5.6, 16.0, 12.8, 10.4);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 5.6, 4.8, 12.8, 10.4);

    /**
     * The boxes and the particle offsets are the whole of what separates this class from its 1.12.2
     * twin. {@code setHardness(1F)}, {@code Material.CIRCUITS}, {@code setSoundType(SoundType.METAL)}
     * and {@code setLightLevel(1.0F)} all arrive as
     * {@code lit(RusticBlockProperties.circuits(1.0F))} from the registration site.
     */
    public BlockCandle(BlockBehaviour.Properties properties) {
        super(properties, shapes(), 0.27, 0.25, 0.33);
    }

    /** The legacy switch in {@code getBoundingBox}, in the order {@link #shapesFor} takes them. */
    private static Map<Direction, VoxelShape> shapes() {
        return shapesFor(STANDING_SHAPE, NORTH_SHAPE, SOUTH_SHAPE, WEST_SHAPE, EAST_SHAPE);
    }
}
