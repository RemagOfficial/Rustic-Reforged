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

public class EntityChair extends Entity {

    private static final float MAX_TURN_FROM_SEAT = 105.0F;

    public EntityChair(EntityType<EntityChair> type, Level level) {
        super(type, level);
    }

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

        if (this.level().isClientSide) {
            return;
        }

        if (!(this.level().getBlockState(this.blockPosition()).getBlock() instanceof BlockChair)) {
            this.discard();
            return;
        }

        if (this.getPassengers().isEmpty()) {
            this.discard();
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        callback.accept(passenger, this.getX(), this.getY(), this.getZ());
    }

    @Override
    public void onPassengerTurned(Entity entityToUpdate) {
        entityToUpdate.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(entityToUpdate.getYRot() - this.getYRot());
        float f1 = Mth.clamp(f, -MAX_TURN_FROM_SEAT, MAX_TURN_FROM_SEAT);
        entityToUpdate.yRotO += f1 - f;
        entityToUpdate.setYRot(entityToUpdate.getYRot() + f1 - f);
        entityToUpdate.setYHeadRot(entityToUpdate.getYRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
