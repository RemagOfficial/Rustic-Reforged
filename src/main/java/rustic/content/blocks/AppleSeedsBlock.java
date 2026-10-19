package rustic.content.blocks;

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
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.TriState;

import java.util.function.Supplier;

public class AppleSeedsBlock extends BushBlock implements BonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_1;

    public static final MapCodec<AppleSeedsBlock> CODEC = simpleCodec(
            properties -> new AppleSeedsBlock(properties, () -> RusticFarmBlocks.APPLE_SAPLING.get()));

    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D)
    };

    private static final float GROWTH_CHANCE = 7.0F;

    private final Supplier<? extends Block> sapling;

    public AppleSeedsBlock(BlockBehaviour.Properties properties, Supplier<? extends Block> sapling) {
        super(properties);
        this.sapling = sapling;

        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<AppleSeedsBlock> codec() {
        return CODEC;
    }

    public int getMaxAge() {

        assert AGE.getPossibleValues().contains(1) : "apple seeds expects AGE_1";
        return 1;
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
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (level.getRawBrightness(pos.above(), 0) < 9) {
            return;
        }
        if (CommonHooks.canCropGrow(level, pos, state,
                random.nextInt((int) (25.0F / GROWTH_CHANCE) + 1) == 0)) {
            int age = getAge(state);
            if (age < getMaxAge()) {
                level.setBlock(pos, getStateForAge(age + 1), Block.UPDATE_CLIENTS);
            } else {
                level.setBlock(pos, sapling.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        TriState soil = below.canSustainPlant(level, belowPos, Direction.UP, state);
        return soil.isDefault() ? below.getBlock() instanceof FarmBlock : soil.isTrue();
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (getAge(state) < getMaxAge()) {
            level.setBlock(pos, getStateForAge(getMaxAge()), Block.UPDATE_CLIENTS);
        } else {
            level.setBlock(pos, sapling.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
