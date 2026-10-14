package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.12.2's {@code BlockRope}: the hanging line of the cloth set, and the one member of the family that
 * an arrow can sever.
 *
 * <p>Everything structural comes from {@link BlockRopeBase} unchanged - the axis, the sag, the survival
 * tests, the right-click that extends a hanging line. What the legacy subclass added is three things, and
 * two of them are one extra clause in a support test: rope is bolted to solid faces rather than hung from
 * thin ones (unlike {@link BlockChain}, which accepts anything attachable), but it will also take hold of
 * the iron lattice, whose centre stub a plain {@code isSideSolid} question would refuse.
 *
 * <p><b>Deliberate deviation, hardness and tool:</b> 1.12.2 built rope from a private
 * {@code MaterialRope extends Material(MapColor.CLOTH)}. The map colour and the {@code SoundType.CLOTH} -
 * {@link net.minecraft.world.level.block.SoundType#WOOL} in 1.21 - port directly. The material itself was
 * the problem: 1.12.2's mining-speed map is keyed on the seven vanilla materials, so a custom subclass
 * matched none of them and no tool was ever faster on rope than a bare hand. It therefore gets no
 * {@code minecraft:mineable/} tag here at all, which is the one family in the mod with a null tool. The
 * legacy {@code setBurning()} becomes the fire lookup pair in {@link RusticBlocks#setFlammables()}, and
 * {@code setNoPushMobility()} has no 1.21 counterpart - mobs path around a block because of its collision
 * shape, which rope already has.
 *
 * <p><b>Deferred:</b> the legacy tests also accepted {@code STAKE_TIED} and axis-matched
 * {@code GRAPE_LEAVES}. Both arrive with the crops (Phase 5), and {@link #attachesTo} is where they get
 * added; the lattice test is the only one of the three that can be written today.
 */
public class BlockRope extends BlockRopeBase implements RusticFaceShapes.Declarable {

    public BlockRope(Properties properties) {
        super(properties);
    }

    /**
     * 1.12.2's {@code isSideSupported}: the base's same-line-or-solid-face test, widened only by the
     * lattice, whose face shape is a centre stub rather than a solid face.
     */
    @Override
    public boolean isSideSupported(LevelReader level, BlockPos pos, BlockState state, Direction facing) {
        if (facing == Direction.DOWN) {
            return false;
        }
        return super.isSideSupported(level, pos, state, facing)
                || attachesTo(level.getBlockState(pos.relative(facing)));
    }

    /** 1.12.2's {@code canPlaceBlockOnSide}, with the same extra clause and the base's ground recursion. */
    @Override
    protected boolean canPlaceOnSide(LevelReader level, BlockPos pos, Direction side) {
        if (side == Direction.UP) {
            // Dispatches back into this method, so the lattice is accepted from above as well.
            return canPlaceOnSide(level, pos, Direction.DOWN);
        }
        return super.canPlaceOnSide(level, pos, side)
                || attachesTo(level.getBlockState(pos.relative(side.getOpposite())));
    }

    /**
     * The blocks a rope will hold on to that the ordinary solid-face question cannot see.
     *
     * <p>Phase 5 adds {@code ModBlocks.STAKE_TIED} and {@code ModBlocks.GRAPE_LEAVES} here - the leaves
     * case matching on the axis, as the legacy pair of tests did.
     */
    private static boolean attachesTo(BlockState anchor) {
        return anchor.getBlock() instanceof BlockLattice;
    }

    /**
     * 1.12.2's {@code onEntityCollidedWithBlock}: an arrow that touches a rope cuts it, which is how the
     * legacy mod let a player sever a hanging line from the ground.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // The legacy guard against the client world; the hook fires on both sides in 1.21.
        if (!level.isClientSide && entity instanceof AbstractArrow arrow && isArrowInAABB(level, pos, state, arrow)) {
            RusticDrops.removeAndDrop(level, pos, state);
        }
    }

    /**
     * 1.12.2's {@code isArrowInAABB}: the rope's own outline, fattened by an eighth of a block on each of
     * the two axes it does not run along, tested against the arrow's bounding box.
     *
     * <p>The fattening is what stops an arrow from having to hit the two-inch-wide centre of the rope
     * dead-on. The legacy method worked from the un-offset box and added the block position afterwards,
     * which is what {@link AABB#move(BlockPos)} does here, so the numbers are the same.
     */
    private boolean isArrowInAABB(BlockGetter level, BlockPos pos, BlockState state, AbstractArrow arrow) {
        Direction.Axis axis = state.getValue(AXIS);
        double xExp = axis == Direction.Axis.X ? 0.0 : 0.125;
        double yExp = axis == Direction.Axis.Y ? 0.0 : 0.125;
        double zExp = axis == Direction.Axis.Z ? 0.0 : 0.125;

        VoxelShape shape = getShape(state, level, pos, CollisionContext.empty());
        AABB aabb = shape.bounds().inflate(xExp, yExp, zExp).move(pos);
        return aabb.intersects(arrow.getBoundingBox());
    }

    /**
     * 1.12.2's {@code getBlockFaceShape}: the same thin centre stub the chain and the lattice present on
     * the two faces along the line, which is what lets another rope, a chain or a lantern hang off the end
     * of one and stops anything clinging to its side.
     */
    @Override
    public RusticFaceShapes.FaceShape rusticFaceShape(BlockState state, Direction face) {
        return state.getValue(AXIS) == face.getAxis()
                ? RusticFaceShapes.FaceShape.CENTER_SMALL
                : RusticFaceShapes.FaceShape.UNDEFINED;
    }
}
