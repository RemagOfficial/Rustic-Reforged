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

public class BlockChair extends BlockRotatable implements RusticFaceShapes.Declarable {

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 18.0, 14.0);

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

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void sit(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isPassenger()
                || player.isShiftKeyDown()
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) >= MAX_SIT_DISTANCE_SQR) {
            return;
        }
        if (level.isClientSide) {

            return;
        }

        EntityChair chair = null;
        for (EntityChair seat : level.getEntities(EntityTypeTest.forClass(EntityChair.class),
                new AABB(pos), EntityChair::isAlive)) {
            if (!seat.getPassengers().isEmpty()) {

                return;
            }

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

            chair.positionRider(player);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RusticFaceShapes.FaceShape rusticFaceShape(BlockState state, Direction face) {
        return RusticFaceShapes.FaceShape.UNDEFINED;
    }
}
