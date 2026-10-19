package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 1.12.2's {@code BlockBerryBush}: the wildberry bush, a six-age ground plant that is harvested by hand rather
 * than broken - right-clicking a ripe bush picks a berry and resets it to age 0 so it can regrow, exactly like
 * vanilla's own sweet-berry bush. The legacy class was abstract with a single {@code getBerries()} subclass; here
 * the produce is a plain {@link Supplier}, the same stand-in {@link HerbBlock} uses, so there is one concrete
 * block and one berry item behind it.
 *
 * <p>Behaviour translated from the legacy class: {@code PropertyInteger AGE 0..5} is
 * {@link BlockStateProperties#AGE_5}; the legacy {@code getGrowthChance 6.0F} with
 * {@code rand.nextInt((int)(25.0F / chance) + 1) == 0} is the {@code nextInt(5)} roll, still gated on light 9;
 * {@code checkAndDropBlock}/{@code canBlockStay} become {@link #canSurvive} (inherited soil test) plus the
 * {@link #updateShape} that drops the bush when its soil disappears; {@code onBlockActivated} harvest becomes
 * {@link #useWithoutItem}/{@link #useItemOn} - with a held bone meal passed through so the legacy "unless holding
 * bone meal" clause still lets bonemeal reach the block; the legacy {@code grow() -> growOutward} spread at full
 * age is {@link #growOutward}. The bush is walk-through (the legacy {@code NULL} collision box).
 *
 * <p>Phase 17 S17.2 restored the two deferred visuals together: the biome foliage tint the legacy {@code IColoredBlock}
 * applied is registered for this block in {@code RusticClient}, and the blockstate now draws the real 1.12.2 base model
 * (log stem over a tintindex-1 leaf cube) for every age. The legacy {@code berries=true} berry overlays stay out on
 * purpose - that variant was never reachable in 1.12.2 either, because the state container here (as there) declares
 * only {@code AGE 0..5}.
 */
public class BerryBushBlock extends BushBlock implements BonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;

    public static final MapCodec<BerryBushBlock> CODEC = simpleCodec(properties -> new BerryBushBlock(properties, null));

    // 1.12.2 BUSH_AABB 0.125..0.875 in x/z (Block.box 2..14) rising the full height.
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    /** The berry this bush yields at full age; {@code null} falls back to the block's own item. */
    private final Supplier<? extends Item> berries;

    public BerryBushBlock(BlockBehaviour.Properties properties, Supplier<? extends Item> berries) {
        super(properties);
        this.berries = berries;
        // 1.12.2: setDefaultState(baseState.withProperty(AGE, 0))
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<BerryBushBlock> codec() {
        return CODEC;
    }

    /** The legacy abstract {@code getBerries()}: the produce, which for the wildberry bush is {@code WILDBERRIES}. */
    public Item getBerries() {
        return berries == null ? asItem() : berries.get();
    }

    public int getMaxAge() {
        // AGE is BlockStateProperties.AGE_5 (0..5); these mappings give IntegerProperty no max accessor, so the
        // bound is written out and checked, exactly as HerbBlock and GrapeStemBlock do.
        assert AGE.getPossibleValues().contains(5) : "berry bush expects AGE_5";
        return 5;
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
        return SHAPE;
    }

    /** The legacy {@code NULL} collision box and {@code isPassable == true}: you walk straight through a bush. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * 1.12.2's {@code updateTick}: ripen one age in place while lit, on the legacy
     * {@code nextInt((int)(25.0F / 6.0F) + 1) == 0} roll, i.e. {@code nextInt(5)}. The outward spread the legacy
     * tick had is commented out there too - only bonemeal spreads a bush.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || level.getRawBrightness(pos, 0) < 9) {
            return;
        }
        int age = getAge(state);
        if (age >= getMaxAge()) {
            return;
        }
        if (CommonHooks.canCropGrow(level, pos, state, random.nextInt(5) == 0)) {
            level.setBlock(pos, getStateForAge(age + 1), Block.UPDATE_CLIENTS);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    /**
     * 1.12.2's {@code neighborChanged -> checkAndDropBlock}: when the soil under the bush goes, the bush drops
     * itself (loot runs, so a ripe bush still yields its berry) and the cell empties.
     */
    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbourState,
            net.minecraft.world.level.LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos
    ) {
        if (!state.canSurvive(level, pos)) {
            RusticDrops.dropAsItem(level, pos, state);
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    // ------------------------------------------------------------------
    // Harvest - 1.12.2's onBlockActivated, split across the two 1.21 empty-hand / with-item hooks.
    // ------------------------------------------------------------------

    /** 1.12.2's {@code onBlockActivated} with an empty hand: pick the berry from a ripe bush. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (isMaxAge(state)) {
            if (!level.isClientSide) {
                harvest(state, level, pos, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * 1.12.2's {@code onBlockActivated} with an item in hand: harvest a ripe bush, except when holding bone meal -
     * the legacy guard {@code !(heldItem == DYE && meta == 15)} passed the click on so bonemeal could spread the
     * bush. Handing back {@link ItemInteractionResult#PASS_TO_DEFAULT_BLOCK_INTERACTION} reproduces that.
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            net.minecraft.world.InteractionHand hand,
            BlockHitResult hit
    ) {
        if (stack.is(Items.BONE_MEAL)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (isMaxAge(state)) {
            if (!level.isClientSide) {
                harvest(state, level, pos, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** The legacy harvest body: reset the bush to age 0 and hand the player one berry (dropping it if full). */
    private void harvest(BlockState state, Level level, BlockPos pos, Player player) {
        level.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_ALL);
        ItemStack stack = new ItemStack(getBerries());
        if (!player.getAbilities().instabuild && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ------------------------------------------------------------------
    // Bonemeal - 1.12.2's IGrowable: grow toward full, or spread a young bush outward once full.
    // ------------------------------------------------------------------

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return !isMaxAge(state) || canSpread(level, pos, state);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = getAge(state);
        if (age < getMaxAge()) {
            int grown = age + 2 + random.nextInt(4); // legacy getBonemealAgeIncrease: two to five
            level.setBlock(pos, getStateForAge(Math.min(grown, getMaxAge())), Block.UPDATE_ALL);
        } else {
            growOutward(level, pos, random);
        }
    }

    /** 1.12.2's {@code growOutward} predicate: is any cell around the bush plantable. */
    public boolean canSpread(LevelReader level, BlockPos pos, BlockState state) {
        return !validOutwardSpots(level, pos).isEmpty();
    }

    /** 1.12.2's {@code growOutward}: place one age-0 bush on a random free, plantable neighbour cell. */
    public void growOutward(ServerLevel level, BlockPos pos, RandomSource random) {
        List<BlockPos> spots = validOutwardSpots(level, pos);
        if (!spots.isEmpty()) {
            level.setBlock(spots.get(random.nextInt(spots.size())), this.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private List<BlockPos> validOutwardSpots(LevelReader level, BlockPos pos) {
        List<BlockPos> spots = new ArrayList<>();
        BlockState plant = this.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos target = pos.offset(dx, 0, dz);
                if (level.isEmptyBlock(target) && plant.canSurvive(level, target)) {
                    spots.add(target);
                }
            }
        }
        return spots;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
