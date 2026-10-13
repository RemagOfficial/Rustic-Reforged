package rustic.content.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 1.12.2's {@code BlockRotatable}: a full block that stores which way it looks in a horizontal
 * {@code facing} property.
 *
 * <p>Most of the legacy class is gone. Its hand-rolled {@code PropertyDirection}, its
 * {@code getStateFromMeta}/{@code getMetaFromState} pair and its {@code createBlockState()} override
 * all existed only because 1.12.2 packed block states into block metadata; 1.21 ships the identical
 * property as {@link BlockStateProperties#HORIZONTAL_FACING} and derives the stored value from the
 * state itself, so the two metadata conversions and the state-container declaration simply
 * disappear. {@link #rotate} and {@link #mirror} stay, because a block has to say how its own
 * properties react to structure rotation - exactly what vanilla's abstract
 * {@code HorizontalDirectionalBlock} does, which is not extended here only because it demands a
 * {@code MapCodec} that this port has no use for yet. What is left as genuinely Rustic's own is the
 * placement rule: "the block turns to look at the player who placed it".
 */
public class BlockRotatable extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockRotatable(BlockBehaviour.Properties properties) {
        super(properties);
        // 1.12.2: blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** 1.12.2: {@code placer.getHorizontalFacing().getOpposite()}, i.e. the block faces the player. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    /**
     * Same result as vanilla's own mirror implementations, written against {@link Rotation#rotate}
     * instead of the deprecated {@code BlockStateBase#rotate(Rotation)} so that the ported codebase
     * stays warning-free under {@code -Xlint:deprecation}.
     */
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }
}
