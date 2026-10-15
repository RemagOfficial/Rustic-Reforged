package rustic.content.blocks;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 1.12.2's {@code BlockLeavesApple}: the apple tree's foliage, which is an ordinary leaf block carrying one
 * extra integer - how ripe the apple growing inside it is.
 *
 * <p>The legacy class extended 1.12's {@code BlockLeaves}, whose decay was driven by a {@code check_decay} /
 * {@code decayable} pair. 1.21 rebuilt leaf decay around a {@code distance}/{@code persistent} pair (both
 * inherited from {@link LeavesBlock}), so those two legacy properties have no equivalent to port: the fruit
 * {@code apple_age} property is the only state the legacy block declared on top of vanilla leaves, and it is
 * kept here as {@link #AGE}. The apple tree itself is built from vanilla {@code OAK_LOG} plus this block (see
 * {@link rustic.world.RusticWorldGen#APPLE_TREE}), which is why there is no Rustic apple log.
 *
 * <p><b>Deliberate scope choice for Phase 8.</b> The legacy block also grew fruit on its random ticks, ripened
 * under bonemeal, gave an apple on right-click at max age, and dropped that apple when broken - all of which
 * the plan defers with the fruit/harvest handler (the apple-fruit behaviour belongs to the content phase, not
 * to worldgen). This slice registers the block and its {@link #AGE} property so the tree feature has real
 * foliage to place and the future harvest hook has a state to read; the fruit lifecycle is not wired here.
 */
public class BlockLeavesApple extends LeavesBlock {

    /** 1.12.2's {@code PropertyInteger.create("apple_age", 0, 3)}: the ripeness of the apple inside the leaf. */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    public BlockLeavesApple(BlockBehaviour.Properties properties) {
        super(properties);
        // LeavesBlock has already registered the distance/persistent/waterlogged defaults; add the unripe fruit.
        this.registerDefaultState(this.defaultBlockState().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE);
    }
}
