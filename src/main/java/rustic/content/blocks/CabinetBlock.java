package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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

/**
 * 1.12.2's {@code BlockCabinet}: a two-block-tall wooden chest-like storage block that merges vertically
 * into a 54-slot double when a second cabinet is stacked on it with the same facing and mirror.
 *
 * <p>The legacy class extended {@code BlockRotatable} (so the facing/placer-opposite placement rule is the
 * one {@link BlockRotatable} documents) and layered three extra behaviours on top: a horizontal
 * {@code mirror} flag picked from which half of the face was clicked, a {@code top} flag that turns the
 * lower half into a double, and a custom {@code CabinetRenderer} that swung two animated doors open. This
 * port keeps the first three in full - they are behaviour, and the double inventory the {@code top} flag
 * enables is the whole point of the block - and defers the door animation exactly as the barrel's
 * see-through top was deferred: the block renders a static cube now and the swinging doors arrive with the
 * baked-model/BER rework (see the {@code machines()} note in {@code RusticBlockStates}). Because the model
 * is static and non-directional, the {@code mirror} property carries no rendering meaning yet; it is kept
 * only so the legacy double-merge rule (a top half joins a bottom half that shares facing <em>and</em>
 * mirror) reproduces faithfully.
 *
 * <p>Two 1.12.2 details that vanish rather than translate: the {@code BOTTOM} property existed only for the
 * renderer to know which half drew the lower doors, so a static cube needs neither it nor the
 * {@code getActualState} that computed it; and the {@code onBlockPlacedBy}/{@code addInformation} pair that
 * copied a {@code material} stack into the tile entity belonged to the legacy dynamic cabinet craft. The port
 * builds the cabinet with a plain {@code minecraft:crafting_shaped} recipe that carries no wood material, so
 * neither is read or shown (per-wood appearance stays a Phase 9 nicety).
 */
public class CabinetBlock extends BlockRusticChest {

    public static final MapCodec<CabinetBlock> CODEC = simpleCodec(CabinetBlock::new);

    /** 1.12.2 {@code BlockRotatable.FACING}, kept as the vanilla horizontal property. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 1.12.2 {@code BlockCabinet.MIRROR}: which side the (deferred) door hinges sit on. */
    public static final BooleanProperty MIRROR = BooleanProperty.create("mirror");
    /** 1.12.2 {@code BlockCabinet.TOP}: this cabinet is the upper half of a vertical double. */
    public static final BooleanProperty TOP = BooleanProperty.create("top");

    public CabinetBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // 1.12.2: setDefaultState(... .withProperty(MIRROR, false).withProperty(TOP, false).withProperty(FACING, ...))
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
        // 1.12.2 createBlockState(): FACING, MIRROR, TOP, BOTTOM. BOTTOM was render-only and is gone with the
        // static model, so only the three state properties the behaviour reads are declared.
        builder.add(FACING, MIRROR, TOP);
    }

    /**
     * 1.12.2's {@code getStateForPlacement}: the block turns to look at the placer, the {@code mirror} flag
     * is picked from which half of the face was clicked, and the {@code top} flag lights up only when the
     * block beneath is a cabinet that is not already a top and shares both facing and mirror.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        Vec3 hit = context.getClickLocation();
        // The legacy hitX/hitZ were the click coordinates within the block being placed; getClickLocation is
        // in world space, so subtracting the target cell recovers the same 0..1 fraction.
        double relX = hit.x - pos.getX();
        double relZ = hit.z - pos.getZ();
        boolean mirror = switch (facing) {
            case NORTH -> relX >= 0.5;
            case SOUTH -> relX < 0.5;
            case EAST -> relZ >= 0.5;
            default -> relZ < 0.5; // WEST
        };

        Level level = context.getLevel();
        BlockState below = level.getBlockState(pos.below());
        boolean top = below.is(this)
                && !below.getValue(TOP)
                && below.getValue(MIRROR) == mirror
                && below.getValue(FACING) == facing;

        return this.defaultBlockState().setValue(FACING, facing).setValue(MIRROR, mirror).setValue(TOP, top);
    }

    /**
     * 1.12.2's {@code onBlockActivated} opened the GUI only when the clicked face was the cabinet's front
     * ({@code facing == state.getValue(FACING)}); clicking a side or the top did nothing. {@link
     * BlockRusticChest#useWithoutItem} opens on any face, so the front-face guard is added back in front of it.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (hitResult.getDirection() != state.getValue(FACING)) {
            return InteractionResult.PASS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    /**
     * 1.12.2's {@code breakBlock} cleared the {@code TOP} flag off the partner left stranded above a broken
     * bottom half, so an orphaned upper cabinet stopped advertising a double. The inventory drop itself is
     * handled by {@link BlockRusticChest#onRemove}; this only repairs the neighbour's state.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        // Only when the block actually leaves the world (not when it merely changes state in place).
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
}
