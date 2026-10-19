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

public abstract class StakeCropBlock extends BushBlock implements BonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    private static final VoxelShape CROP_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape CROP_COLLISION = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);

    private final Supplier<? extends Block> cropStake;
    private final Supplier<? extends Item> seed;
    private final Supplier<? extends Item> crop;

    private final Supplier<? extends Item> rareCrop;

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

        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    public int getMaxAge() {

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

        return CROP_COLLISION;
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

    private float growthChance(BlockState state, LevelReader level, BlockPos pos) {
        float growth = 0.125F * (level.getRawBrightness(pos, 0) - 11);
        BlockState soil = level.getBlockState(pos.below());
        if (soil.isFertile(level, pos.below()) || soil.is(this)) {
            growth *= 1.5F;
        }
        return Math.abs(1.5F + growth);
    }

    public boolean canBePlanted(LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        TriState soil = below.canSustainPlant(level, belowPos, Direction.UP, this.defaultBlockState());
        return soil.isDefault() ? below.getBlock() instanceof net.minecraft.world.level.block.FarmBlock : soil.isTrue();
    }

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

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            boolean willHarvest, FluidState fluid) {
        boolean removed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        if (removed) {
            level.setBlock(pos, stake().defaultBlockState(), Block.UPDATE_ALL);
        }
        return removed;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(getSeed());
    }

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

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {

        return hasUnripeCrop(level, pos) || climbTarget(level, pos) != null;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (hasUnripeCrop(level, pos)) {

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

        BlockPos source = climbTarget(level, pos);
        if (source == null) {
            return;
        }
        level.setBlock(source.above(), getStateForAge(0), Block.UPDATE_ALL);
    }

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

    private static boolean isHoldingBonemeal(Player player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BONE_MEAL);
    }

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

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
