package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.phys.BlockHitResult;
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
 *   <li>{@code breakBlock}'s "leave a stake behind" is {@link #onDestroyedByPlayer}, the hook that is
 *   actually responsible for the removal ({@code ServerPlayerGameMode#destroyBlock} ignores whatever
 *   {@code playerWillDestroy} returns - read from the sources jar), so the override re-places the stake
 *   once the super call has cleared the cell;</li>
 *   <li>{@code getItem}/{@code getSeed} pick-block is {@link #getCloneItemStack}, which hands back the
 *   seed item so the crop is never given in creative form.</li>
 *   <li>Divergence, at the user's request rather than legacy: bone meal never harvests the column and
 *   treats it as one plant - a click advances <em>every</em> unripe cell of the column with one shared
 *   growth roll, and once the whole column is ripe the same click force-climbs one stake (skipping the
 *   climb's dice roll), while every other held item still harvests as the legacy {@code tryHarvest} did.
 *   See {@link #useItemOn} and {@link #performBonemeal}.</li>
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
        // 1.12.2 gave the foliage the pole's thin collision box and was not passable. The box alone would
        // read as isSolid() (full height hits calculateSolid's ysize bar) and revert the farmland under the
        // bottom vine, so the family registers on crop().forceSolidOff() - stakeCrop() - instead.
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
     * 1.12.2's plant gate in {@code BlockCropStake.onBlockActivated}: clicking a seed onto a stake only needed
     * the soil <em>under the stake</em> to accept the vine ({@code pos.down().canSustainPlant(crop)}) - there
     * was no light term at placement, so a vine could be planted at night or in a dark greenhouse. Kept apart
     * from {@link #canSurvive} (which adds the legacy {@code canBlockStay} light rule the ongoing vine must
     * meet), because folding the light check into planting is what stopped a seed clicking onto a stake in
     * dim light from taking - a divergence from the legacy block.
     */
    public boolean canBePlanted(LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        TriState soil = below.canSustainPlant(level, belowPos, Direction.UP, this.defaultBlockState());
        return soil.isDefault() ? below.getBlock() instanceof net.minecraft.world.level.block.FarmBlock : soil.isTrue();
    }

    /**
     * 1.12.2's {@code canBlockStay}: lit enough - or open to the sky, the legacy {@code || canSeeSky}
     * branch that kept an unlit night column alive - and standing either on soil that accepts the plant
     * or on another block of the same vine (the column). The soil answer is the NeoForge
     * {@code canSustainPlant} {@link TriState}, exactly as {@code BushBlock#canSurvive} reads it.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getRawBrightness(pos, 0) < 8 && !level.canSeeSky(pos)) {
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
     * be replanted, rather than air. NeoForge's {@code onDestroyedByPlayer} is the hook actually
     * responsible for the removal - {@code ServerPlayerGameMode#destroyBlock} calls it after
     * {@code playerWillDestroy}, and nothing in the destruction path consumes any state that hook returns
     * (verified against the sources jar, both overloads) - so the stake is re-placed here once the super
     * call has cleared the cell, on both sides because the client predicts the break.
     */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            boolean willHarvest, FluidState fluid) {
        boolean removed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        if (removed) {
            level.setBlock(pos, stake().defaultBlockState(), Block.UPDATE_ALL);
        }
        return removed;
    }

    /** 1.12.2's {@code getItem}/{@code getSeed} pick result: the seed item, never the crop block. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(getSeed());
    }

    // ------------------------------------------------------------------
    // Harvest - 1.12.2's onBlockActivated/tryHarvest, split across the two 1.21 empty-hand / with-item
    // hooks exactly as BerryBushBlock does. A right-click on any vine of a ripe column picks every ripe
    // vine in that column (the legacy walk up and down the same-crop stack), not just the clicked cell.
    // ------------------------------------------------------------------

    /**
     * 1.12.2's {@code onBlockActivated} with an empty hand: harvest a ripe vine in the clicked column.
     * Bone meal in the main hand stands the harvest down (the divergence lives in {@link #useItemOn} /
     * {@link #performBonemeal}), so the click reaches the bone meal path instead of being eaten here.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (isHoldingBonemeal(player)) {
            return InteractionResult.PASS;
        }
        if (!hasRipeCrop(level, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            harvestColumn(level, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 1.12.2's {@code onBlockActivated} ran {@code tryHarvest} with <em>any</em> held item, bone meal
     * included. The port deliberately diverges on the bone meal: it never harvests - the click passes to
     * the {@link BonemealableBlock} path, where it ripens every unripe cell of the column at once (one
     * plant, one roll) and, once the whole column is ripe, force-climbs one stake per click
     * ({@link #performBonemeal}). Every other item still harvests the ripe column exactly like the
     * legacy {@code tryHarvest} did.
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (stack.is(Items.BONE_MEAL)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!hasRipeCrop(level, pos)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            harvestColumn(level, pos, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 1.12.2's ripe guard for a click, widened to the whole column: the clicked cell or any same-vine block
     * directly above or below it counts, so one click on any block of a ripe column harvests the column.
     */
    private boolean hasRipeCrop(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        for (int y = pos.getY(); y < level.getHeight(); y++) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (!state.is(this)) {
                break;
            }
            if (getAge(state) >= getMaxAge()) {
                return true;
            }
        }
        for (int y = pos.getY() - 1; y >= level.getMinBuildHeight(); y--) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (!state.is(this)) {
                break;
            }
            if (getAge(state) >= getMaxAge()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 1.12.2's {@code onBlockActivated} column walk - up to the world ceiling and back down to the floor,
     * stopping at the first block that is not this vine, picking every ripe cell on the way.
     */
    private void harvestColumn(Level level, BlockPos pos, Player player) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        for (int y = pos.getY(); y < level.getHeight(); y++) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (!state.is(this)) {
                break;
            }
            tryHarvest(level, cursor, state, player);
        }
        for (int y = pos.getY() - 1; y >= level.getMinBuildHeight(); y--) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (!state.is(this)) {
                break;
            }
            tryHarvest(level, cursor, state, player);
        }
    }

    /**
     * 1.12.2's {@code tryHarvest}: a ripe cell resets to {@code getMaxAge() - 1} - not to a seed - so the vine
     * has to ripen once more before it can be picked again (the almanac's "broken vines will have to grow
     * back"), and hands the player {@link #getCrop(RandomSource)}, dropping it on the ground when the
     * inventory is full; creative players pick without filling their inventory, like {@code BerryBushBlock}.
     */
    private void tryHarvest(Level level, BlockPos pos, BlockState state, Player player) {
        if (getAge(state) < getMaxAge()) {
            return;
        }
        level.setBlock(pos, getStateForAge(getMaxAge() - 1), Block.UPDATE_ALL);
        ItemStack stack = new ItemStack(getCrop(level.getRandom()));
        if (!player.getAbilities().instabuild && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ------------------------------------------------------------------
    // Bonemeal - 1.12.2's IGrowable, translated onto the three-method BonemealableBlock interface, plus
    // the user-requested divergence that lets a ripe vine be force-climbed instead of harvested.
    // ------------------------------------------------------------------
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        // The column is one plant: the target is valid while ANY cell still grows, not just the clicked
        // one, and at full ripeness only while there is a stake to climb.
        return hasUnripeCrop(level, pos) || climbTarget(level, pos) != null;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (hasUnripeCrop(level, pos)) {
            // User-requested "one plant" feel (diverges from 1.12.2, which grew only the clicked cell):
            // one shared 2..5 roll - the legacy grow() jump - applied to every unripe cell of the column.
            int jump = net.minecraft.util.Mth.nextInt(random, 2, 5);
            BlockPos.MutableBlockPos cursor =
                    new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
            for (int y = pos.getY(); y < level.getHeight(); y++) {
                cursor.setY(y);
                BlockState cell = level.getBlockState(cursor);
                if (!cell.is(this)) {
                    break;
                }
                if (!isMaxAge(cell)) {
                    level.setBlock(cursor, getStateForAge(Math.min(getAge(cell) + jump, getMaxAge())),
                            Block.UPDATE_CLIENTS);
                }
            }
            for (int y = pos.getY() - 1; y >= level.getMinBuildHeight(); y--) {
                cursor.setY(y);
                BlockState cell = level.getBlockState(cursor);
                if (!cell.is(this)) {
                    break;
                }
                if (!isMaxAge(cell)) {
                    level.setBlock(cursor, getStateForAge(Math.min(getAge(cell) + jump, getMaxAge())),
                            Block.UPDATE_CLIENTS);
                }
            }
            return;
        }
        // Divergence from 1.12.2 (where bone meal simply harvested a ripe vine): a click on the column
        // whose ripe top can climb converts the stake above right now, skipping the dice roll that the
        // random-tick climb rides. One guaranteed block of climb per bone meal - the item consumption and
        // the growth particles come from {@code BoneMealItem.applyBonemeal} itself, and no sound is
        // played, matching BerryBushBlock/HerbBlock.
        BlockPos source = climbTarget(level, pos);
        if (source == null) {
            return;
        }
        level.setBlock(source.above(), getStateForAge(0), Block.UPDATE_ALL);
    }

    /**
     * True while any cell of the clicked vine column is still growing. The column is the contiguous run
     * of this vine above and below the click - the same walk {@link #hasRipeCrop} makes - so a bone meal
     * click on one cell sees the whole plant.
     */
    private boolean hasUnripeCrop(LevelReader level, BlockPos clicked) {
        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos(clicked.getX(), clicked.getY(), clicked.getZ());
        for (int y = clicked.getY(); y < level.getHeight(); y++) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (!state.is(this)) {
                break;
            }
            if (!isMaxAge(state)) {
                return true;
            }
        }
        for (int y = clicked.getY() - 1; y >= level.getMinBuildHeight(); y--) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (!state.is(this)) {
                break;
            }
            if (!isMaxAge(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the main hand holds bone meal; the empty-hand harvest hook stands down for it (the
     * {@code ServerPlayerGameMode} cascade only reaches that hook from a main-hand click, so an
     * off-hand stack of bone meal never blocks the ordinary empty-hand harvest). See {@link #useItemOn}.
     */
    private static boolean isHoldingBonemeal(Player player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BONE_MEAL);
    }

    /**
     * The cell a bone meal climb would grow from: the top of the clicked vine column (a column is
     * contiguous, so only its top cell can have anything but another vine above it), and only when that
     * cell is fully ripe, a bare {@link CropStakeBlock} sits directly above, and the column has not yet
     * reached {@code getMaxHeight()} - the same gates as the random-tick climb, minus the dice. {@code
     * null} when there is nothing to climb, which is also what keeps bone meal on a maxed or stake-less
     * ripe vine from doing anything at all (and never harvesting it).
     */
    private BlockPos climbTarget(LevelReader level, BlockPos clicked) {
        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos(clicked.getX(), clicked.getY(), clicked.getZ());
        BlockPos top = clicked;
        for (int y = clicked.getY() + 1; y < level.getHeight(); y++) {
            cursor.setY(y);
            if (!level.getBlockState(cursor).is(this)) {
                break;
            }
            top = cursor.immutable();
        }
        if (!isMaxAge(level.getBlockState(top))
                || !level.getBlockState(top.above()).is(stake())
                || level.getBlockState(top.below(maxHeight - 1)).is(this)) {
            return null;
        }
        return top;
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
