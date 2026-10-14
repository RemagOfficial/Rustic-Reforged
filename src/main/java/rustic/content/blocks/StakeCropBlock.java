package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.TriState;

import java.util.function.Supplier;

/**
 * 1.12.2's {@code BlockStakeCrop}: the tomato and chili vine that climbs a column of
 * {@link CropStakeBlock stakes}. It is <em>not</em> vanilla's {@code CropBlock} - the legacy class predates
 * that comparison by ignoring the horizontal neighbours, growing one block tall per age and instead
 * stacking copies of itself up a stake until {@code getMaxHeight()}, and reverting the broken cell back to
 * a bare stake instead of air.
 *
 * <p>The port keeps every one of those differences, translated onto the 1.21 plant API:
 * <ul>
 *   <li>{@code PropertyInteger AGE 0..3} is {@link BlockStateProperties#AGE_3};</li>
 *   <li>{@code updateTick}'s {@code ForgeHooks.onCropsGrowPre/Post} pair is
 *   {@link CommonHooks#canCropGrow} / {@link CommonHooks#fireCropGrowPost}, the same two hooks vanilla's
 *   {@code CropBlock.randomTick} uses (read from the sources jar, not guessed);</li>
 *   <li>{@code canBlockStay}'s "on soil or on another of myself" rule becomes {@link #canSurvive}, which
 *   is also what {@code BushBlock.updateShape} re-checks, so a vine whose support is removed reverts to a
 *   stake through {@link #updateShape} rather than vanishing;</li>
 *   <li>{@code breakBlock}'s "leave a stake behind" is {@link #playerWillDestroy}, the 1.21 hook whose
 *   non-air return value replaces the broken cell;</li>
 *   <li>{@code getItem}/{@code getSeed} pick-block is {@link #getCloneItemStack}, which hands back the
 *   seed item so the crop is never given in creative form.</li>
 * </ul>
 *
 * <p>The seed, crop and stake references are {@link Supplier}s rather than direct fields: the crop, its
 * seed item and the stake all register in different classes and reading one from another's static
 * initialiser would deadlock. Every supplier is only invoked at world-interaction time, long after the
 * registries are frozen, which is what lets {@code TomatoCropBlock}/{@code ChiliCropBlock} hand in
 * {@code () -> RusticItems.TOMATO.get()} without a construction order.
 *
 * <p>Deferred, in line with the phase boundaries: the per-age cross {@code Model}s are Phase 9 art, and the
 * chicken temptation that 1.12.2 registered against the crop item is read from an item tag in 1.21 and
 * arrives with the recipes.
 */
public abstract class StakeCropBlock extends BushBlock implements BonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    // 1.12.2 CROP_AABB 0.125..0.875 x/z, full height, over a STAKE_AABB collision of the pole's width.
    private static final VoxelShape CROP_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape CROP_COLLISION = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);

    private final Supplier<? extends Block> cropStake;
    private final Supplier<? extends Item> seed;
    private final Supplier<? extends Item> crop;
    /** The chili's one-in-42 ghost pepper; {@code null} for every other stake crop. */
    private final Supplier<? extends Item> rareCrop;
    /** The {@code rand.nextInt(denominator)} divisor behind {@link #rareCrop}; unused when it is null. */
    private final int rareCropDenominator;
    private final int maxHeight;

    protected StakeCropBlock(
            BlockBehaviour.Properties properties,
            Supplier<? extends Block> cropStake,
            Supplier<? extends Item> seed,
            Supplier<? extends Item> crop,
            Supplier<? extends Item> rareCrop,
            int rareCropDenominator,
            int maxHeight
    ) {
        super(properties);
        this.cropStake = cropStake;
        this.seed = seed;
        this.crop = crop;
        this.rareCrop = rareCrop;
        this.rareCropDenominator = rareCropDenominator;
        this.maxHeight = maxHeight;
        // 1.12.2: setDefaultState(baseState.withProperty(AGE, 0))
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    public int getMaxAge() {
        // AGE is BlockStateProperties.AGE_3 (0..3); the mappings do not expose a max accessor, so the
        // bound is written out and asserted against the property's own value list at class load.
        assert AGE.getPossibleValues().contains(3) : "stake crop expects AGE_3";
        return 3;
    }

    public int getMaxHeight() {
        return maxHeight;
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

    protected Item getSeed() {
        return seed.get();
    }

    protected Item getCrop() {
        return crop.get();
    }

    /** 1.12.2 {@code getCrop(Random)}: the base crop, unless this vine has a rare variant to roll. */
    protected Item getCrop(RandomSource random) {
        if (rareCrop != null && random.nextInt(rareCropDenominator) == 0) {
            return rareCrop.get();
        }
        return getCrop();
    }

    private Block stake() {
        return cropStake.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CROP_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 1.12.2 gave the foliage the pole's thin collision box and was not passable.
        return CROP_COLLISION;
    }

    /** The legacy block ticked randomly for its column growth even at full age, so always true here. */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * 1.12.2's {@code updateTick}. Below full age the vine ripens in place when there is light; at full
     * age it climbs, converting the {@link CropStakeBlock} directly above into a new age-0 crop - but only
     * while the column has not yet reached {@code getMaxHeight()} (the legacy guard
     * {@code world.getBlockState(pos.down(maxHeight - 1)) != this}).
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        // 1.12.2 read the light one block up, matching a vine growing toward the sky.
        if (level.getRawBrightness(pos.above(), 0) < 9) {
            return;
        }

        int age = getAge(state);
        float chance = growthChance(state, level, pos);

        if (age < getMaxAge()) {
            if (CommonHooks.canCropGrow(level, pos, state, random.nextInt((int) (30.0F / chance) + 1) == 0)) {
                level.setBlock(pos, getStateForAge(age + 1), Block.UPDATE_CLIENTS);
                CommonHooks.fireCropGrowPost(level, pos, state);
            }
        } else if (level.getBlockState(pos.above()).is(stake())
                && !level.getBlockState(pos.below(maxHeight - 1)).is(this)) {
            if (CommonHooks.canCropGrow(level, pos, state, random.nextInt((int) (20.0F / chance) + 1) == 0)) {
                level.setBlock(pos.above(), getStateForAge(0), Block.UPDATE_ALL);
                CommonHooks.fireCropGrowPost(level, pos, state);
            }
        }
    }

    /**
     * 1.12.2's {@code getGrowthChance}: a light-scaled base that a fertile soil - or climbing off another
     * of the same vine - multiplies by one and a half. Kept on {@code getRawBrightness} and
     * {@code isFertile}, the two 1.21 lookups the legacy {@code getLight}/{@code isFertile} calls became.
     */
    private float growthChance(BlockState state, LevelReader level, BlockPos pos) {
        float growth = 0.125F * (level.getRawBrightness(pos, 0) - 11);
        BlockState soil = level.getBlockState(pos.below());
        if (soil.isFertile(level, pos.below()) || soil.is(this)) {
            growth *= 1.5F;
        }
        return Math.abs(1.5F + growth);
    }

    /**
     * 1.12.2's {@code canBlockStay}: lit enough, and standing either on soil that accepts the plant or on
     * another block of the same vine (the column). The soil answer is the NeoForge
     * {@code canSustainPlant} {@link TriState}, exactly as {@code BushBlock#canSurvive} reads it.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getRawBrightness(pos, 0) < 8) {
            return false;
        }
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if (below.is(this)) {
            return true;
        }
        TriState soil = below.canSustainPlant(level, belowPos, Direction.UP, state);
        return soil.isDefault() ? below.getBlock() instanceof net.minecraft.world.level.block.FarmBlock : soil.isTrue();
    }

    /**
     * 1.12.2's {@code neighborChanged -> checkAndDropBlock}: a vine that lost its support drops and the
     * cell reverts to a bare stake. {@code BushBlock#updateShape} would drop it to AIR; overriding the same
     * check to return the stake instead is what keeps the pole standing where the legacy block did.
     */
    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbourState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos
    ) {
        if (!state.canSurvive(level, pos)) {
            RusticDrops.dropAsItem(level, pos, state);
            return stake().defaultBlockState();
        }
        return state;
    }

    /**
     * 1.12.2's {@code breakBlock}: after a player breaks the vine the cell becomes a stake again, ready to
     * be replanted, rather than air. A non-air return from {@code playerWillDestroy} is the 1.21 form of
     * that exact behaviour.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        return stake().defaultBlockState();
    }

    /** 1.12.2's {@code getItem}/{@code getSeed} pick result: the seed item, never the crop block. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(getSeed());
    }

    // ------------------------------------------------------------------
    // Bonemeal - 1.12.2's IGrowable, translated onto the three-method BonemealableBlock interface.
    // ------------------------------------------------------------------
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return !isMaxAge(state);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int target = getAge(state) + net.minecraft.util.Mth.nextInt(random, 2, 5);
        if (target > getMaxAge()) {
            target = getMaxAge();
        }
        level.setBlock(pos, getStateForAge(target), Block.UPDATE_CLIENTS);
    }

    /** Fluids do not displace a standing vine; the empty override just states that explicitly. */
    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
