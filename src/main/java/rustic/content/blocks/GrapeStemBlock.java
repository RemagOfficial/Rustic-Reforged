package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;

public class GrapeStemBlock extends BushBlock implements BonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    public static final MapCodec<GrapeStemBlock> CODEC = simpleCodec(GrapeStemBlock::new);

    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 4.0D, 10.0D),
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 8.0D, 10.0D),
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D),
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D)
    };

    public GrapeStemBlock(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<GrapeStemBlock> codec() {
        return CODEC;
    }

    public int getMaxAge() {

        assert AGE.getPossibleValues().contains(3) : "grape stem expects AGE_3";
        return 3;
    }

    public int getAge(BlockState state) {
        return state.getValue(AGE);
    }

    public boolean isMaxAge(BlockState state) {
        return state.getValue(AGE) >= getMaxAge();
    }

    public BlockState getStateForAge(int age) {
        return this.defaultBlockState().setValue(AGE, age);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[getAge(state)];
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || level.getRawBrightness(pos, 0) < 9) {
            return;
        }
        int age = getAge(state);
        if (age < getMaxAge()) {
            if (CommonHooks.canCropGrow(level, pos, state, random.nextInt(4) == 0)) {
                level.setBlock(pos, getStateForAge(age + 1), Block.UPDATE_CLIENTS);
                CommonHooks.fireCropGrowPost(level, pos, state);
            }
        } else {
            BlockState above = level.getBlockState(pos.above());
            if (above.is(RusticBlocks.ROPE.get())) {
                Direction.Axis axis = above.getValue(BlockRopeBase.AXIS);
                if (axis != Direction.Axis.Y
                        && CommonHooks.canCropGrow(level, pos, state, random.nextInt(4) == 0)) {
                    level.setBlock(pos.above(),
                            RusticFarmBlocks.GRAPE_LEAVES.get().defaultBlockState()
                                    .setValue(BlockRopeBase.AXIS, axis)
                                    .setValue(GrapeLeavesBlock.DIST, 0),
                            Block.UPDATE_ALL);
                    CommonHooks.fireCropGrowPost(level, pos, state);
                }
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (!isMaxAge(state)) {
            return true;
        }
        BlockState above = level.getBlockState(pos.above());
        return above.is(RusticBlocks.ROPE.get()) && above.getValue(BlockRopeBase.AXIS) != Direction.Axis.Y;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = getAge(state);
        if (age < getMaxAge()) {
            int grown = age + 2 + random.nextInt(4);
            level.setBlock(pos, getStateForAge(Math.min(grown, getMaxAge())), Block.UPDATE_CLIENTS);
        } else {
            BlockState above = level.getBlockState(pos.above());
            Direction.Axis axis = above.getValue(BlockRopeBase.AXIS);
            level.setBlock(pos.above(),
                    RusticFarmBlocks.GRAPE_LEAVES.get().defaultBlockState()
                            .setValue(BlockRopeBase.AXIS, axis)
                            .setValue(GrapeLeavesBlock.DIST, 0),
                    Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
