package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rustic.content.blockentity.CabinetBlockEntity;
import rustic.content.blockentity.RusticBlockEntities;

public class CabinetBlock extends BlockRusticChest {

    public static final MapCodec<CabinetBlock> CODEC = simpleCodec(CabinetBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final BooleanProperty MIRROR = BooleanProperty.create("mirror");

    public static final BooleanProperty TOP = BooleanProperty.create("top");

    public CabinetBlock(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(MIRROR, false)
                .setValue(TOP, false));
    }

    @Override
    public MapCodec<CabinetBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(FACING, MIRROR, TOP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        Vec3 hit = context.getClickLocation();

        double relX = hit.x - pos.getX();
        double relZ = hit.z - pos.getZ();
        boolean mirror = switch (facing) {
            case NORTH -> relX >= 0.5;
            case SOUTH -> relX < 0.5;
            case EAST -> relZ >= 0.5;
            default -> relZ < 0.5;

        };

        Level level = context.getLevel();
        BlockState below = level.getBlockState(pos.below());
        boolean top = below.is(this)
                && !below.getValue(TOP)
                && below.getValue(MIRROR) == mirror
                && below.getValue(FACING) == facing;

        return this.defaultBlockState().setValue(FACING, facing).setValue(MIRROR, mirror).setValue(TOP, top);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (hitResult.getDirection() != state.getValue(FACING)) {
            return InteractionResult.PASS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);

        if (!moving && !state.is(newState.getBlock())) {
            BlockPos above = pos.above();
            BlockState aboveState = level.getBlockState(above);
            if (aboveState.is(this) && aboveState.getValue(TOP)) {
                level.setBlock(above, aboveState.setValue(TOP, false), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CabinetBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, RusticBlockEntities.CABINET.get(), CabinetBlockEntity::lidAnimateTick)
                : null;
    }
}
