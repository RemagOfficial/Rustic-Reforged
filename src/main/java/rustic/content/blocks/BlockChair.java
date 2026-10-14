package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rustic.content.entities.EntityChair;

/**
 * 1.12.2's {@code BlockChair}: a seat you can click to sit in.
 *
 * <p>The block half is small - the legacy class is mostly its nested {@code EntityChair}, which now
 * lives in {@link rustic.content.entities} next to the registered type that describes it. What the
 * block keeps is the horizontal {@code facing} property, the placement rule ("turn to look at the
 * player"), the {@code (2, 0, 2) → (14, 18, 14)} box of the seat and the face shape declaration, all of
 * which the two hooks below and {@link BlockRotatable} express without any metadata conversion.
 *
 * <p>Clicking is where 1.21 forced one real change: the legacy {@code onBlockActivated} was a single
 * entry point, while 1.21 splits it into {@link #useItemOn} (a hand holding something) and
 * {@link #useWithoutItem} (an empty hand). Rustic's chair did not care which, so both call the same
 * {@link #sit} helper. The legacy method then returned {@code true} on <em>every</em> path - even the
 * three that refuse to seat anyone - so the click was swallowed even with a sword in hand. Both hooks
 * reproduce that: {@link ItemInteractionResult#CONSUME} is what stops the held item being used, and
 * {@code sidedSuccess} is the empty-hand answer to the same question. Sneak-clicking a chair with a
 * block in hand therefore cannot place that block, exactly as it could not in 1.12.2.
 *
 * <p>Two lines of the legacy interaction had to be carried over rather than modernised, because 1.21
 * added a rule that silently breaks the chair:
 *
 * <ul>
 *   <li><b>The mount-time snap.</b> {@code setPositionAndUpdate(chair.posX, chair.posY, chair.posZ)}
 *   after a successful {@code startRiding} is {@link net.minecraft.world.entity.Entity#positionRider},
 *   called once here as well as every tick: {@code EntityChair#tick} discards a seat whose passenger is a
 *   block away, and on the spawn tick the rider is still standing next to the chair - see the comment in
 *   {@link #sit}.</li>
 *   <li><b>Forced mounting.</b> 1.21's {@code Entity#removePassenger} sets a 60 tick
 *   {@code boardingCooldown} and {@code Entity#canRide} refuses a rider while it counts down, so an
 *   unforced {@code startRiding} denies a re-sit for three seconds after every dismount - a lockout
 *   1.12.2 did not have. {@code force = true} skips that test; the two guards it also skips are ones this
 *   class already performs itself ({@code sit} refuses sneaking players, and the rider test below refuses
 *   a chair that already has somebody on it, so {@code canAddPassenger} could not have added a second
 *   one either).</li>
 *   <li><b>Re-using a free seat.</b> 1.12.2 refused the click whenever a live seat was in the block, and
 *   that is too strict for 1.21's tick order: a seat is only removed when it next ticks, so the seat the
 *   previous rider just left is still alive, and still alive when the re-sit click is processed a tick
 *   later. Every quick get-up-and-sit-back-down therefore did nothing at all. {@link #sit} now mounts the
 *   existing seat if it has no passenger and refuses only a seat that really has one.</li>
 * </ul>
 *
 * <p>Dropped for real: the one-off teleport's role as the only position sync is gone now that
 * {@link EntityChair#positionRider} runs every tick - vanilla's mounts work the same way - and so is
 * {@code getEntitiesWithinAABB}'s dead seats, since the modern query filters by {@code isAlive}.
 */
public class BlockChair extends BlockRotatable implements RusticFaceShapes.Declarable {

    /** 1.12.2's {@code CHAIR_AABB}: the seat, its four legs and the back bar, 18 sixteenths tall. */
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 18.0, 14.0);

    /**
     * 1.12.2's {@code player.getDistanceSqToCenter(pos) >= 5} cutoff, as a squared distance against the
     * middle of the block - about 2.2 blocks, i.e. well inside the reach of a reach-4 player.
     */
    private static final double MAX_SIT_DISTANCE_SQR = 5.0D;

    public BlockChair(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        this.sit(level, pos, state, player);
        // The legacy return value was true whatever happened, so the held item never got to act.
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        this.sit(level, pos, state, player);
        // SUCCESS on the client, CONSUME on the server: the same swallow the legacy hook handed back,
        // for the one case 1.21 routes anywhere other than useItemOn - an empty hand.
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 1.12.2's {@code onBlockActivated}, server side half: seat nobody who is sneaking, already sitting
     * or too far away, and never two players on one chair.
     */
    private void sit(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isPassenger()
                || player.isShiftKeyDown()
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) >= MAX_SIT_DISTANCE_SQR) {
            return;
        }
        if (level.isClientSide) {
            // The seat is a server-spawned entity; the client learns about it from the spawn packet and
            // the riding packet, which is also why 1.12.2's client->server dismount message is gone.
            return;
        }

        EntityChair chair = null;
        for (EntityChair seat : level.getEntities(EntityTypeTest.forClass(EntityChair.class),
                new AABB(pos), EntityChair::isAlive)) {
            if (!seat.getPassengers().isEmpty()) {
                // Somebody is genuinely sitting there: 1.12.2's refusal, kept.
                return;
            }
            // The seat of the rider who just got up. It is empty and alive for one tick only, because
            // EntityChair#tick removes a seat on its next tick rather than on the tick its passenger
            // leaves, and mounting it now is what makes that tick never happen.
            chair = seat;
            break;
        }

        if (chair == null) {
            chair = EntityChair.create(level, pos, state.getValue(FACING));
            if (chair == null || !level.addFreshEntity(chair)) {
                return;
            }
        }

        if (player.startRiding(chair, true)) {
            // 1.12.2's setPositionAndUpdate(chair.posX, chair.posY, chair.posZ), and it is load-bearing:
            // the vehicle ticks before its riders do, so without the snap the seat's own next tick would
            // measure the player where they were standing, and BlockChair's every-tick pinning through
            // EntityChair#positionRider would only arrive after that. positionRider is the same call the
            // every-tick path makes, so the two cannot disagree.
            chair.positionRider(player);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 1.12.2's {@code getBlockFaceShape}: nothing on any face, so no mount can attach to a chair. */
    @Override
    public RusticFaceShapes.FaceShape rusticFaceShape(BlockState state, Direction face) {
        return RusticFaceShapes.FaceShape.UNDEFINED;
    }
}
