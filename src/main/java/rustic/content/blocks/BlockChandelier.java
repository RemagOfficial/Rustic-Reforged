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

public class BlockChandelier extends FallingBlock implements Declarable {

    public static final MapCodec<BlockChandelier> CODEC = simpleCodec(BlockChandelier::new);

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

    private static final float FALL_HURT_PER_DISTANCE = 6.0F;
    private static final int FALL_HURT_MAX = 400;

    private static final int EVENT_ANVIL_LAND = 1031;

    private static final int EVENT_ANVIL_DESTROY = 1029;

    public BlockChandelier(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight() && !suspended(level, pos)) {
            FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
            this.falling(entity);
        }
    }

    private boolean suspended(LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        if (RusticFaceShapes.isSolidFace(above, level, abovePos, Direction.DOWN)) {
            return true;
        }
        return above.getBlock() instanceof BlockRopeBase && above.getValue(BlockRopeBase.AXIS) == Direction.Axis.Y;
    }

    @Override
    protected void falling(FallingBlockEntity entity) {
        entity.setHurtsEntities(FALL_HURT_PER_DISTANCE, FALL_HURT_MAX);
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState belowState, FallingBlockEntity entity) {
        level.levelEvent(EVENT_ANVIL_LAND, pos, 0);
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity entity) {
        level.levelEvent(EVENT_ANVIL_DESTROY, pos, 0);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return SHAPE;
    }

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
