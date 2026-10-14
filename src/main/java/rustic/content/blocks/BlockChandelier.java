package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rustic.content.blocks.RusticFaceShapes.Declarable;
import rustic.content.blocks.RusticFaceShapes.FaceShape;

/**
 * 1.12.2's {@code BlockChandelier}: an empty iron frame that hangs from a chain, a rope or a ceiling and
 * falls - hurting whatever it lands on - when whatever held it goes away.
 *
 * <p>Legacy {@code BlockFallingBase} became 1.21's {@link FallingBlock}, so the falling machinery itself
 * is inherited: {@link #tick} replaces {@code checkFallable}, {@link #falling} replaces
 * {@code onStartFalling}, and {@code onEndFalling}/{@code onBroken} are now
 * {@link net.minecraft.world.level.block.Fallable#onLand} and {@code onBrokenAfterFall}, called by
 * {@link FallingBlockEntity} once it has worked out whether the block could place.
 *
 * <p>Three legacy details are worth the reading:
 * <ul>
 *   <li>the chandelier's extra {@link #suspended} test has no vanilla equivalent at all - sand does not
 *       care what is above it - so it is spliced into {@link #tick} rather than being expressed as a
 *       survival rule. A rope above is checked through {@link BlockRopeBase#AXIS} exactly as the legacy
 *       code did, and the "solid face above" branch keeps the legacy {@code isSideSolid} question, i.e.
 *       a {@code SOLID} down face.</li>
 *   <li>{@code checkFallable}'s "chunk not loaded, so teleport the block straight down" branch is gone.
 *       1.21's own sand has no such branch either: scheduled ticks only run in loaded chunks, so the
 *       block simply waits for one. The legacy {@code pos.getY() >= 0} floor becomes vanilla's
 *       {@code getMinBuildHeight()}, which is the same number for every default dimension and a correct
 *       one for custom height ranges.</li>
 *   <li>the two {@code playEvent} calls are kept as level-event numbers rather than swapped for direct
 *       sound calls, because 1.21.1's {@code LevelRenderer#levelEvent} still maps {@code 1029} to
 *       {@code ANVIL_DESTROY} and {@code 1031} to {@code ANVIL_LAND} - verified against the decompiled
 *       source, so this is the identical sound, volume and pitch the legacy mod played, and it still
 *       reaches every client in range through the level-event packet instead of a block-scoped one. The
 *       legacy name of that hook was {@code playEvent}; 1.21.1 calls it {@code levelEvent}.</li>
 * </ul>
 */
public class BlockChandelier extends FallingBlock implements Declarable {

    public static final MapCodec<BlockChandelier> CODEC = simpleCodec(BlockChandelier::new);

    /** 1.12.2's {@code AABB}: the frame is a quarter block tall and spans the whole cell. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

    /** 1.12.2's {@code fallHurtAmount} and {@code fallHurtMax}, set by {@code onStartFalling}. */
    private static final float FALL_HURT_PER_DISTANCE = 6.0F;
    private static final int FALL_HURT_MAX = 400;

    /** {@code world.playEvent(1031, ...)}: the anvil landing sound, as 1.21.1 still resolves it. */
    private static final int EVENT_ANVIL_LAND = 1031;
    /** {@code world.playEvent(1029, ...)}: the anvil breaking sound. */
    private static final int EVENT_ANVIL_DESTROY = 1029;

    public BlockChandelier(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    /**
     * 1.12.2's {@code updateTick} and {@code checkFallable} together, with the legacy {@code !isRemote}
     * test dropped because this hook only ever runs on a {@link ServerLevel}.
     *
     * <p>{@link FallingBlock} already schedules this two ticks after placement and after every neighbour
     * change, which is the same trigger the legacy class got from {@code BlockFalling}'s
     * {@code onBlockAdded -> scheduleUpdate} pair.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight() && !suspended(level, pos)) {
            FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
            this.falling(entity);
        }
    }

    /**
     * 1.12.2's {@code suspended}: the frame stays up while the block above it either offers a solid down
     * face or is a length of rope hanging straight down.
     */
    private boolean suspended(LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        if (RusticFaceShapes.isSolidFace(above, level, abovePos, Direction.DOWN)) {
            return true;
        }
        return above.getBlock() instanceof BlockRopeBase && above.getValue(BlockRopeBase.AXIS) == Direction.Axis.Y;
    }

    /** 1.12.2's {@code onStartFalling}, which set the three fields {@link FallingBlockEntity} now wraps up. */
    @Override
    protected void falling(FallingBlockEntity entity) {
        entity.setHurtsEntities(FALL_HURT_PER_DISTANCE, FALL_HURT_MAX);
    }

    /** 1.12.2's {@code onEndFalling}: the anvil land sound where the frame came to rest. */
    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState belowState, FallingBlockEntity entity) {
        level.levelEvent(EVENT_ANVIL_LAND, pos, 0);
    }

    /** 1.12.2's {@code onBroken}: the anvil destroy sound when the frame shattered instead of landing. */
    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity entity) {
        level.levelEvent(EVENT_ANVIL_DESTROY, pos, 0);
    }

    /**
     * 1.12.2's {@code randomDisplayTick}, which this block overrode with an empty body. It has to keep
     * that override: {@link FallingBlock}'s own version spawns dust falling off the underside of anything
     * with air below it, which the legacy chandelier deliberately did not do.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 1.12.2 returned the same box for getBoundingBox, and the empty space above it is only visual -
        // the frame is one quarter block tall, so the collision shape is that same box.
        return SHAPE;
    }

    /**
     * 1.12.2's {@code getBlockFaceShape}, which the shape derivation cannot express: the frame reports
     * solid sides while its own collision box is far too thin to be a rigid face, and its hollow top is a
     * bowl rather than something a candle could stand on.
     */
    @Override
    public FaceShape rusticFaceShape(BlockState state, Direction face) {
        if (face == Direction.DOWN) {
            return FaceShape.BOWL;
        }
        if (face == Direction.UP) {
            return FaceShape.CENTER;
        }
        return FaceShape.SOLID;
    }
}
