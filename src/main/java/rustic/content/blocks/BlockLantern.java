package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rustic.content.blocks.RusticFaceShapes.Declarable;
import rustic.content.blocks.RusticFaceShapes.FaceShape;

/**
 * 1.12.2's {@code BlockLantern}: the metal box that hangs from a chain or stands on a post, in iron,
 * gold and silver. It is deliberately not {@link net.minecraft.world.level.block.LanternBlock}: the
 * vanilla lantern stores one boolean and refuses to float, while the Rustic one stores the whole
 * direction it was clicked on, keeps no survival test at all, and answers the face-shape question for
 * the blocks that hang below it.
 *
 * <p>That missing survival test is the reason a Rustic lantern stays where it was put even when the
 * block it was mounted to goes away, and it is ported as written - the legacy class simply never
 * implemented {@code neighborChanged} or {@code checkForDrop}, unlike every other mounted block in the
 * mod.
 */
public class BlockLantern extends Block implements Declarable {

    /**
     * 1.12.2's {@code PropertyDirection.create("facing")}, which takes all six directions, down included
     * - a lantern placed on a ceiling hangs, and that is the only way this block gets its DOWN state.
     */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    protected static final VoxelShape DEFAULT_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0);
    protected static final VoxelShape DOWN_SHAPE = Block.box(4.0, 6.0, 4.0, 12.0, 16.0, 12.0);

    /** The flame sits a third of the way up the standing box and a full box-half up the hanging one. */
    private static final double FLAME_HEIGHT = 0.33;
    private static final double HANGING_FLAME_LIFT = 0.375;

    public BlockLantern(BlockBehaviour.Properties properties) {
        // setHardness(2.0F), Material.IRON, setSoundType(METAL) and setLightLevel(1.0F) are
        // lit(RusticBlockProperties.metal(2.0F)); the default is the standing lantern, as in 1.12.2.
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING) == Direction.DOWN ? DOWN_SHAPE : DEFAULT_SHAPE;
    }

    /**
     * 1.12.2's {@code getStateForPlacement}: the clicked face, verbatim, with no test of whether the
     * lantern can stay there. Whatever the player clicked is what the lantern points away from.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    /** 1.12.2's {@code randomDisplayTick}: one flame particle, no smoke - the metal box keeps its fire in. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double y = pos.getY() + FLAME_HEIGHT;
        if (state.getValue(FACING) == Direction.DOWN) {
            y += HANGING_FLAME_LIFT;
        }
        level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5, y, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
    }

    /**
     * 1.12.2's {@code getBlockFaceShape}, which is how a chain, a candle or another lantern learns it may
     * hang from this one. The face the lantern was mounted to is the centre it presents to whatever is
     * above or beside it; the bottom of a standing lantern is a centre stub too, while the bottom of a
     * hanging one is nothing at all.
     */
    @Override
    public FaceShape rusticFaceShape(BlockState state, Direction face) {
        Direction mountFace = state.getValue(FACING).getOpposite();

        if (mountFace == face) {
            return FaceShape.CENTER;
        }
        return face == Direction.DOWN && mountFace != Direction.UP ? FaceShape.CENTER : FaceShape.UNDEFINED;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    /**
     * 1.12.2's {@code withMirror}, which called {@code Mirror#mirror(EnumFacing)} directly. That method
     * has no 1.21 counterpart - {@link Mirror#getRotation(Direction)} hands back a {@link Rotation} that
     * has to be applied - and the two agree: reflecting a direction across the mirror's plane is the
     * quarter, half or no turn the rotation says.
     */
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        return state.setValue(FACING, mirror.getRotation(facing).rotate(facing));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
