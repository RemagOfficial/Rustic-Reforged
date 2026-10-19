package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RetortBlock extends Block {

    public static final MapCodec<RetortBlock> CODEC = simpleCodec(RetortBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Shapes.box(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 0.75D);

    public RetortBlock(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<RetortBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        return openAdjacentCondenser(state, level, pos, player) ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {

        return openAdjacentCondenser(state, level, pos, player) ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private boolean openAdjacentCondenser(BlockState state, Level level, BlockPos pos, Player player) {
        BlockPos condenserPos = pos.relative(state.getValue(FACING).getOpposite());
        BlockState condenserState = level.getBlockState(condenserPos);
        if (player.isSpectator()) {
            return true;
        }
        MenuProvider provider = condenserMenu(level, condenserPos, condenserState);
        if (provider != null) {
            if (!level.isClientSide) {
                player.openMenu(provider);
            }
            return true;
        }
        return false;
    }

    @Nullable
    private static MenuProvider condenserMenu(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        BlockPos bottom;
        Block retort;
        if (block instanceof CondenserAdvancedBlock) {

            bottom = state.getValue(CondenserAdvancedBlock.TOP) ? pos.below() : pos;
            retort = RusticMachineBlocks.RETORT_ADVANCED.get();
        } else if (block instanceof CondenserBlock) {
            bottom = state.getValue(CondenserBlock.TOP) ? pos.below() : pos;
            retort = RusticMachineBlocks.RETORT.get();
        } else {
            return null;
        }
        if (!CondenserFlanking.hasRetorts(level, bottom, level.getBlockState(bottom), retort)) {
            return null;
        }
        return level.getBlockEntity(bottom) instanceof MenuProvider provider ? provider : null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }
}
