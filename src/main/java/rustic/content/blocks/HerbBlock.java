package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

import java.util.function.Supplier;

/**
 * 1.12.2's {@code BlockHerbBase}: the single-block herb plant - aloe vera, blood orchid, the mushrooms and
 * the rest - that ripens in place rather than climbing anything. Where {@link StakeCropBlock} stacks copies
 * of itself up a pole, this is the vanilla-shaped half of the farm family: a four-age
 * {@link BushBlock implements BonemealableBlock} that grows straight up and drops its own herb.
 *
 * <p>Two kinds of herb share this class, told apart only by the {@code herb} {@link Supplier}:
 * <ul>
 *   <li>a <em>self-herb</em> (aloe vera, blood orchid, chamomile, cohosh, deathstalk mushroom, horsetail,
 *       mooncap mushroom, vanta lily, wind thistle) passes {@code null}, so its herb <em>is</em> its own
 *       block item - {@link #getHerb()} falls back to {@link Block#asItem()}, which is exactly the legacy
 *       {@code hasItemBlock == false} case where the block item was the picked/herbed stack;</li>
 *   <li>a <em>crop-herb</em> (cloudsbluff, core root, ginseng, marsh mallow) has no block item at all; its
 *       produce and its seed are one and the same {@link rustic.content.items.ItemHerbEdible}, which is
 *       passed in here so the block knows what to hand back on a creative pick.</li>
 * </ul>
 *
 * <p>Behaviour translated from the legacy class: {@code PropertyInteger AGE 0..3} is
 * {@link BlockStateProperties#AGE_3}; {@code getGrowthChance() == 3.0F} with
 * {@code rand.nextInt((int)(45.0F / chance) + 1) == 0} is the {@code nextInt(16)} roll below, still gated on
 * the legacy light level of 9; {@code ForgeHooks.onCropsGrowPre/Post} are the same
 * {@link CommonHooks} pair {@link StakeCropBlock} uses; the {@code getPickBlock}/{@code getItemDropped}
 * "give me the herb" answer is {@link #getCloneItemStack}. The natural-worldgen that seeded these plants
 * into biomes is Phase 8 and is deliberately absent, and the herb recipes are Phase 7.
 */
public class HerbBlock extends BushBlock implements BonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    public static final MapCodec<HerbBlock> CODEC = simpleCodec(properties -> new HerbBlock(properties, null));

    // 1.12.2 gave the plant a zero-height box that grew with age; vanilla's crop uses exactly that ramp, so
    // the SHAPE_BY_AGE here mirrors CropBlock's (the legacy block's getBlockFaceShape was UNDEFINED - it
    // never stood on anything). Four entries for the four AGE_3 values.
    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D)
    };

    /** The item this herb yields; {@code null} means the block's own item (a self-herb). */
    private final Supplier<? extends Item> herb;

    public HerbBlock(BlockBehaviour.Properties properties, Supplier<? extends Item> herb) {
        super(properties);
        this.herb = herb;
        // 1.12.2: setDefaultState(baseState.withProperty(AGE, 0))
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<HerbBlock> codec() {
        return CODEC;
    }

    /** The legacy {@code getHerb()}: the produce item, which for a self-herb is the block item itself. */
    public Item getHerb() {
        return herb == null ? asItem() : herb.get();
    }

    public int getMaxAge() {
        // AGE is BlockStateProperties.AGE_3 (0..3). These mappings give IntegerProperty no max accessor,
        // so - exactly as StakeCropBlock does - the bound is written out and checked against the property.
        assert AGE.getPossibleValues().contains(3) : "herb expects AGE_3";
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

    /** The legacy block {@code tickRandomly}, growing in place while lit; always ticking, even at full age. */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * 1.12.2's {@code updateTick}: ripen one age in place when there is light, on the legacy
     * {@code nextInt((int)(45.0F / getGrowthChance()) + 1) == 0} roll. With {@code getGrowthChance() == 3.0F}
     * that divisor is a fixed {@code nextInt(16)}.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (level.getRawBrightness(pos, 0) < 9) {
            return;
        }
        int age = getAge(state);
        if (age >= getMaxAge()) {
            return;
        }
        if (CommonHooks.canCropGrow(level, pos, state, random.nextInt(16) == 0)) {
            level.setBlock(pos, getStateForAge(age + 1), Block.UPDATE_CLIENTS);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    /** 1.12.2's {@code getPickBlock}: picking the plant gives the herb, never the raw block for a crop-herb. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(getHerb());
    }

    // ------------------------------------------------------------------
    // Bonemeal - 1.12.2's IGrowable, on the three-method BonemealableBlock interface.
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
        level.setBlock(pos, getStateForAge(getMaxAge()), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
