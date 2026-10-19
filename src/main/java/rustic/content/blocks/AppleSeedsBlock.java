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

/**
 * 1.12.2's {@code BlockAppleSeeds}: the two-age growable crop the crushing tub's apple byproduct turns into.
 * It is <em>not</em> vanilla's {@code CropBlock} - the legacy class grew a single step and then, on the next
 * random tick (or a second bonemeal), replaced itself with an {@link RusticFarmBlocks#APPLE_SAPLING apple
 * sapling} rather than dropping a fruit. That "mature into a sapling" behaviour is the whole point of the
 * block and is what the port reproduces.
 *
 * <p>Every legacy detail, translated onto the 1.21 plant API:
 * <ul>
 *   <li>{@code PropertyInteger AGE 0..1} ({@code getMaxAge() == 1}) is {@link BlockStateProperties#AGE_1};</li>
 *   <li>the two {@code CROPS_AABB}s ({@code 0..0.125} tall, then {@code 0..0.5}) become the age-keyed
 *   {@link #SHAPE_BY_AGE};</li>
 *   <li>{@code updateTick}'s light gate ({@code getLightFromNeighbors(pos.up()) >= 9}) and its
 *   {@code rand.nextInt((int)(25.0F / getGrowthChance()) + 1) == 0} roll - with the legacy
 *   {@code getGrowthChance()} a constant {@code 7F}, that divisor is a fixed {@code nextInt(4)} - are kept
 *   verbatim, wrapped in the same {@link CommonHooks#canCropGrow} / {@link CommonHooks#fireCropGrowPost} pair
 *   the stake crop uses;</li>
 *   <li>at max age the tick (and {@code grow}'s second bonemeal) sets the sapling block with the legacy
 *   {@code 3} update flag, which is {@link Block#UPDATE_ALL};</li>
 *   <li>{@code canPlaceBlockAt}'s "soil below must accept the plant" rule (an {@code IPlantable} of type
 *   Plains, i.e. farmland) becomes {@link #canSurvive}, which the {@code BlockItem}-driven planting path also
 *   consults - so a right-click of the seed on farmland places age 0 exactly as legacy did. Unlike the stake
 *   crop's {@code canSurvive} there is no light requirement here, matching the legacy placement check (the
 *   legacy crop simply floated until its next tick if the ground vanished).</li>
 * </ul>
 *
 * <p>Because the crop has a real block item (the seed itself, registered under the same id in
 * {@link rustic.content.items.RusticItems}), picking a planted crop hands back the seed by default and
 * breaking it loots the seed - both the legacy {@code BlockBase} behaviour, so neither is overridden. The
 * sapling it ripens into is passed as a {@link Supplier} for the same construction-order reason
 * {@link StakeCropBlock} uses them.
 */
public class AppleSeedsBlock extends BushBlock implements BonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_1;

    // BushBlock declares codec() abstract on this NeoForge, exactly as HerbBlock notes; the registered
    // instance is built with the same sapling supplier RusticFarmBlocks passes to the constructor.
    public static final MapCodec<AppleSeedsBlock> CODEC = simpleCodec(
            properties -> new AppleSeedsBlock(properties, () -> RusticFarmBlocks.APPLE_SAPLING.get()));

    // 1.12.2 CROPS_AABB: age 0 stands 0.125 tall, age 1 stands 0.5 tall (0..1 in the legacy array).
    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D)
    };

    /** The legacy {@code getGrowthChance()}: a constant, so the random-tick divisor never changes. */
    private static final float GROWTH_CHANCE = 7.0F;

    private final Supplier<? extends Block> sapling;

    public AppleSeedsBlock(BlockBehaviour.Properties properties, Supplier<? extends Block> sapling) {
        super(properties);
        this.sapling = sapling;
        // 1.12.2: setDefaultState(baseState.withProperty(AGE, 0))
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<AppleSeedsBlock> codec() {
        return CODEC;
    }

    public int getMaxAge() {
        // AGE is BlockStateProperties.AGE_1 (0..1); as elsewhere the bound is written out and checked.
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

    /** The legacy block {@code tickRandomly}, growing then converting; it always ticks. */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * 1.12.2's {@code updateTick}: ripen one age when the cell above is lit, then - at full age - replace the
     * crop with an apple sapling. The sapling write uses the legacy {@code 3} flag ({@link Block#UPDATE_ALL},
     * neighbours included) while the age step used {@code 2} ({@link Block#UPDATE_CLIENTS}).
     */
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

    /**
     * 1.12.2's {@code canPlaceBlockAt}: the soil directly under the crop must accept the plant. There is no
     * light term, and the seed's {@code BlockItem} placement consults this, so farmland (the legacy Plains
     * answer, expressed here through the NeoForge {@code canSustainPlant} {@link TriState}) is what a right-
     * click plants onto.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        TriState soil = below.canSustainPlant(level, belowPos, Direction.UP, state);
        return soil.isDefault() ? below.getBlock() instanceof FarmBlock : soil.isTrue();
    }

    // ------------------------------------------------------------------
    // Bonemeal - 1.12.2's IGrowable: canGrow / canUseBonemeal were both always true (a full-age crop is still
    // a valid target because bonemeal then turns it into the sapling), and grow() jumped to max age or, at max
    // age, set the sapling. No step-by-step increment here - that is the legacy {@code grow}, kept verbatim.
    // ------------------------------------------------------------------
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
