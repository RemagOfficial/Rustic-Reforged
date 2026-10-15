package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.12.2's {@code BlockBeehive}: the world-generated hive that hangs under a leaf canopy and drops
 * honeycomb and bees when broken.
 *
 * <p>Everything the legacy class did by hand around its horizontal {@code facing} property - the
 * {@code PropertyDirection}, the metadata packing, "the hive turns to look at whoever placed it" and the
 * rotation/mirror handling - is inherited from the ported {@link BlockRotatable}, exactly as it is for the
 * gargoyle, so the only genuinely Rustic-specific thing left here is the shape: a hive is narrower than a
 * block and does not fill its cell.
 *
 * <p><b>Deliberate scope choices for Phase 8.</b> The legacy block carried two behaviours this port does not
 * reproduce on the block itself:
 * <ul>
 *   <li>{@code canPlaceBlockAt} required a {@code BlockLeaves} above, and {@code neighborChanged} dropped
 *       the hive when that leaf went away. In 1.21 the "must sit under leaves" rule belongs to the
 *       worldgen feature that places it ({@code rustic.content.world.RusticBeehiveFeature}, Phase 8.1),
 *       not to a survival check that would also reject a player-set hive; the plan registers this as a plain
 *       block, so the constraint lives in the feature.</li>
 *   <li>The honeycomb/bee drop was hand-rolled in {@code getDrops}. 1.21 expresses the same roll in the
 *       generated loot table (see {@link rustic.data.RusticBlockLoot}), which is where it now lives.</li>
 * </ul>
 * There is no tile entity: the legacy bee simulation ran in the apiary, not in the hive block.
 */
public class BlockBeehive extends BlockRotatable {

    /** 1.12.2's {@code BEEHIVE_AABB}, {@code (0.125, 0, 0.125) - (0.875, 1, 0.875)}, in sixteenths. */
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public BlockBeehive(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
