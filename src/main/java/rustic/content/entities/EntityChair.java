package rustic.content.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rustic.content.blocks.BlockChair;

/**
 * 1.12.2's {@code BlockChair.EntityChair}: the invisible seat a clicked chair spawns, and what the
 * player rides while the chair block is under them.
 *
 * <p>The entity is a vehicle and nothing else: no hitbox ({@code sized(0F, 0F)} on the type, which is
 * the legacy {@code setSize(0F, 0F)}), nothing drawn ({@code NoopRenderer}, which 1.21.1 insists on
 * having registered for every type that reaches the client - see
 * {@link rustic.content.client.RusticClient}), no synced data, no saved data of its own. Its whole
 * job is the three things the legacy class did per tick, and 1.21 keeps two of them while modernising
 * the third:
 *
 * <ul>
 *   <li><b>Life cycle.</b> The legacy {@code onUpdate} removed the seat when the block beneath it was no
 *   longer a chair, when it had lost its passenger, or - on the server - when the passenger was sneaking
 *   or had somehow got more than one block away. The first two are kept and are now server-only; the
 *   other two are gone, because 1.21 does them better elsewhere: {@code Player#rideTick} turns a sneak
 *   into {@code stopRiding()} on its own, and {@link #positionRider} pins the rider to the seat every
 *   tick, so a gap of a whole block can only ever be a measurement artefact, never a passenger who left.
 *   <br>Server-only is the part that mattered: a discard on the client is a silent desync, because
 *   {@code Entity#remove} only flags the entity removed - it does not eject passengers - so
 *   {@code Entity#baseTick} stands the local player up and nothing tells the server. The server then keeps
 *   its seat, keeps that player riding it, and refuses every later click as "already occupied" until the
 *   world is reloaded. 1.12.2 ran the same tests on both sides and survived only because
 *   {@code BlockChair.EntityChair#setDead} sent {@code MessageDismountChair} whenever
 *   {@code world.isRemote} was true; there is no modern equivalent of that report, so the client's copy
 *   now exists, carries the rider and waits for packets like every other vanilla mount does.</li>
 *   <li><b>Position.</b> {@code setPositionAndUpdate(chair.posX, chair.posY, chair.posZ)} becomes the
 *   {@link #positionRider(Entity, MoveFunction)} override below, which 1.21 calls from the passenger's own
 *   {@code rideTick} every tick - the modern replacement for the one-off teleport the legacy code
 *   needed, and the reason a sitting player does not rubber-band. It is <em>also</em> still called once at
 *   mount time by {@code BlockChair#sit}: the vehicle ticks before its riders do, so the first tick of a
 *   new seat would otherwise measure the player where they were standing and delete itself. See that
 *   method.</li>
 *   <li><b>Turn.</b> {@code applyOrientationToEntity} - the hook 1.12.2 used to keep the sitter's body
 *   pointed along the chair and their view clamped to it - is now
 *   {@link #onPassengerTurned(Entity)}, and vanilla's {@code Boat} clamps to the very same
 *   -105..105 degrees, which is the number copied here. There is no vanilla seat entity to compare with:
 *   1.21.1 has none at all (no {@code SeatEntity}, no {@code EntityType.SEAT}, and campfires do not seat
 *   anybody), so a boat is the closest mounted precedent.</li>
 * </ul>
 *
 * <p>Dropped because 1.12.2's {@code canBeAttackedWithItem} no longer exists and its two modern cousins
 * already answer the way Rustic wanted: {@code isPickable()} and {@code canBeCollidedWith()} both
 * default to false, so the seat cannot be targeted by crosshair raytracing or shoved around.
 */
public class EntityChair extends Entity {

    /**
     * 1.12.2's {@code applyOrientationToEntity} clamp: how far the sitter may turn away from the
     * direction the chair faces before their body snaps back to the chair's.
     */
    private static final float MAX_TURN_FROM_SEAT = 105.0F;

    public EntityChair(EntityType<EntityChair> type, Level level) {
        super(type, level);
    }

    /**
     * 1.12.2's {@code EntityChair(World, BlockPos, EnumFacing)}. The seat sits a quarter of a block
     * against the back of the chair and four tenths above its bottom face, and the yaw is the direction
     * the block faces - the legacy {@code facing.getHorizontalAngle()} is {@link Direction#toYRot()}.
     *
     * <p>The zero size is not set here: it belongs to the registered type, as it does for every 1.21
     * entity.
     */
    @Nullable
    public static EntityChair create(Level level, BlockPos pos, Direction facing) {
        EntityChair chair = RusticEntities.CHAIR.get().create(level);
        if (chair == null) {
            return null;
        }
        double xOffset = facing.getStepX() * -0.125D;
        double zOffset = facing.getStepZ() * -0.125D;
        chair.setPos(pos.getX() + 0.5 + xOffset, pos.getY() + 0.4, pos.getZ() + 0.5 + zOffset);
        chair.setYRot(facing.toYRot());
        return chair;
    }

    @Override
    public void tick() {
        super.tick();

        // The client's copy decides nothing at all - see the life-cycle note in the class header.
        if (this.level().isClientSide) {
            return;
        }

        // 1.12.2: !(world.getBlockState(getPosition()).getBlock() instanceof BlockChair) -> setDead.
        // blockPosition() is the legacy getPosition(), i.e. the block the seat is standing in.
        if (!(this.level().getBlockState(this.blockPosition()).getBlock() instanceof BlockChair)) {
            this.discard();
            return;
        }

        // The legacy sneak and distance tests are not here: a sneaking rider is dropped by
        // Player#rideTick and a rider that somehow ended up elsewhere is dropped by
        // ServerLevel#tickPassenger, either way the seat loses its passenger and this test removes it one
        // tick later, which is the only removal this class needs.
        if (this.getPassengers().isEmpty()) {
            this.discard();
        }
    }

    /**
     * The one-off {@code setPositionAndUpdate(chair.posX, chair.posY, chair.posZ)} of 1.12.2, run every
     * tick instead: the passenger goes exactly where the seat is, with none of the attachment offsets
     * the default implementation folds in for a mount that has a size of its own.
     */
    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        callback.accept(passenger, this.getX(), this.getY(), this.getZ());
    }

    /** 1.12.2's {@code applyOrientationToEntity}, whose first line is {@code setYBodyRot} now. */
    @Override
    public void onPassengerTurned(Entity entityToUpdate) {
        entityToUpdate.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(entityToUpdate.getYRot() - this.getYRot());
        float f1 = Mth.clamp(f, -MAX_TURN_FROM_SEAT, MAX_TURN_FROM_SEAT);
        entityToUpdate.yRotO += f1 - f;
        entityToUpdate.setYRot(entityToUpdate.getYRot() + f1 - f);
        entityToUpdate.setYHeadRot(entityToUpdate.getYRot());
    }

    /** 1.12.2's empty {@code entityInit}: the seat syncs nothing. */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** 1.12.2's empty {@code readEntityFromNBT}: everything the seat knows comes from the block. */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    /** 1.12.2's empty {@code writeEntityToNBT}. */
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
