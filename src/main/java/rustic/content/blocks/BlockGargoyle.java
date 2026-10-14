package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rustic.content.blocks.RusticFaceShapes.Declarable;
import rustic.content.blocks.RusticFaceShapes.FaceShape;

/**
 * 1.12.2's {@code BlockGargoyle}: a small stone figure that stands on a surface, faces the player who
 * placed it, and can be hung from underneath.
 *
 * <p>Everything the legacy class got from {@code BlockRotatable} - the horizontal {@code facing}
 * property, its metadata packing, the placement rule and the rotation/mirror handling - is inherited from
 * the ported {@link BlockRotatable}, so the only two things left to say are the shape and the faces.
 *
 * <p>The legacy {@code setLightOpacity(0)} needs no equivalent: with {@code noOcclusion()} and a box
 * smaller than a block the state is not solid-rendered, so light already passes straight through it. The
 * same properties carry {@code isOpaqueCube}/{@code isFullCube} returning false, and the {@code CUTOUT}
 * render layer belongs to the model, which {@link rustic.data.RusticBlockStates} writes.
 */
public class BlockGargoyle extends BlockRotatable implements Declarable {

    /** 1.12.2's {@code DEFAULT_AABB}, in sixteenths: {@code (0.125, 0, 0.125) - (0.875, 0.875, 0.875)}. */
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    public BlockGargoyle(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * 1.12.2's {@code getBlockFaceShape}: only the underside is anything, and it is solid - so a chain,
     * candle or lantern may hang from a gargoyle, while nothing may stand on it.
     */
    @Override
    public FaceShape rusticFaceShape(BlockState state, Direction face) {
        return face == Direction.DOWN ? FaceShape.SOLID : FaceShape.UNDEFINED;
    }
}
