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

/**
 * 1.12.2's {@code BlockRetort}, the fixed companion block that flanks a condenser. Both legacy retort ids
 * ({@code retort} beside the basic still, {@code retort_advanced} beside the advanced one) were a single class
 * differing only in texture, so one class backs both here and the datagen blockstate picks the model from the block id.
 *
 * <p>The retort has no inventory and no block entity of its own - it is a decorative column whose one job is to make
 * a condenser's multi-block shape valid, and, when clicked, to open the condenser behind it. The two facts that give
 * it its shape come straight from legacy {@code BlockRetort}:
 * <ul>
 *   <li>a {@code 0.25..0.75} horizontal column AABB (an {@code AxisAlignedBB(0.25,0,0.25,0.75,1,0.75)}), and
 *       {@code isOpaqueCube=false}/{@code isFullCube=false}, which is 1.21's {@code noOcclusion()};</li>
 *   <li>the horizontal {@code FACING} property that {@code getStateForPlacement} sets to the placer's opposite -
 *       the same "turn to look at the player" rule as {@link BlockRotatable}, so the front points away from whoever
 *       placed it and the condenser sits one block behind that front.</li>
 * </ul>
 *
 * <p>Clicking forwards to the block behind the front ({@code pos.relative(facing.getOpposite())}): if that is a
 * condenser (basic or advanced, either tier's retort opens either still, exactly as legacy
 * {@code onBlockActivated} accepted both) its menu is opened through the bottom half's {@link MenuProvider}. The
 * condenser's own {@code hasRetorts} gate is the mirror image of this and lives on the condenser blocks.
 */
public class RetortBlock extends Block {

    public static final MapCodec<RetortBlock> CODEC = simpleCodec(RetortBlock::new);

    /** Legacy {@code BlockRetort.FACING}, the horizontal plane property; the front points away from the placer. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Legacy {@code RETORT_AABB}: a {@code 4..12} pixel column, full height. */
    private static final VoxelShape SHAPE = Shapes.box(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 0.75D);

    public RetortBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Legacy default was FACING=NORTH; placement overrides it to face the placer.
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

    /** 1.12.2 {@code getStateForPlacement}: {@code placer.getHorizontalFacing().getOpposite()}. */
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
        // Legacy accepted a right-click with an item too and still forwarded to the condenser; consume only when a
        // condenser was actually reached, otherwise let the item do its normal thing (place, drink, ...).
        return openAdjacentCondenser(state, level, pos, player) ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Open the condenser one block behind the retort's front. The retort front points away from the condenser, so the
     * still is at {@code facing.getOpposite()}. Both condensers are two-block columns now, so only the bottom half owns
     * the container; resolve to it before asking for the {@link MenuProvider}.
     */
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

    /**
     * The bottom-half condenser's block entity as a {@link MenuProvider}, but only when that still is currently
     * openable - i.e. correctly flanked by its own tier of retorts. Legacy {@code BlockRetort.onBlockActivated}
     * delegated straight to the condenser's own {@code onBlockActivated}, which enforced {@code hasRetorts}, so an
     * unfinished still must not open through its retort either: returning {@code null} here makes the click fall
     * through to normal placement against the retort, exactly as legacy returned {@code false}.
     */
    @Nullable
    private static MenuProvider condenserMenu(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        BlockPos bottom;
        Block retort;
        if (block instanceof CondenserAdvancedBlock) {
            // A click may land on the top half; the container lives one block below.
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
