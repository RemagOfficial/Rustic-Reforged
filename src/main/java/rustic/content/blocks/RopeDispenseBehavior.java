package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

/**
 * 1.12.2's {@code rustic.common.util.DispenseRope}: a dispenser firing rope at rope extends the hanging
 * line instead of dropping the item.
 *
 * <p>The legacy class overrode Forge's {@code BehaviorDefaultDispenseItem#dispenseStack}, and 1.21's
 * {@link DefaultDispenseItemBehavior#execute(BlockSource, ItemStack)} is the same slot in the same
 * template method - {@code dispense} still plays the dispenser sound and arm animation around it, and is
 * final, so {@code execute} is the only thing to override. The two shape changes are mechanical: the
 * source is now a record read as {@code level()}/{@code pos()}/{@code state()}, and 1.12.2's
 * {@code return stack.copy()} with the copy shrunk is the modern contract of mutating and returning the
 * incoming stack, which is what the dispenser stores back into the slot either way.
 *
 * <p>Registered from {@link rustic.Rustic#commonSetup} against the rope's block item, replacing the
 * legacy {@code BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(...)} call in
 * {@code CommonProxy.init()}.
 */
public class RopeDispenseBehavior extends DefaultDispenseItemBehavior {

    /**
     * 1.12.2's {@code addRope} walked at most 64 blocks down an existing line; the same cap
     * {@link BlockRopeBase} puts on a player extending one by hand.
     */
    private static final int MAX_HANG_LENGTH = BlockRopeBase.MAX_HANG_LENGTH;

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        ServerLevel level = source.level();
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        BlockPos pos = source.pos().relative(facing);
        BlockState front = level.getBlockState(pos);

        // Either the block straight out of the dispenser is rope, or the one beyond it is and the space in
        // between is air - the two ways a dispenser can be pointed along an existing line.
        if (front.is(RusticBlocks.ROPE.get())) {
            if (addRope(level, pos)) {
                stack.shrink(1);
                return stack;
            }
        } else if (front.isAir() && level.getBlockState(pos.relative(facing)).is(RusticBlocks.ROPE.get())) {
            pos = pos.relative(facing);
            if (addRope(level, pos)) {
                stack.shrink(1);
                return stack;
            }
        }

        // 1.12.2's super.dispenseStack: an ordinary drop of the item in front of the dispenser.
        return super.execute(source, stack);
    }

    /**
     * 1.12.2's {@code addRope}: follow the line down until it stops being rope, refuse if any of it was
     * lying sideways, then put one more at the end and play the place sound there.
     */
    private static boolean addRope(ServerLevel level, BlockPos pos) {
        Block rope = RusticBlocks.ROPE.get();

        int yOffset = 1;
        while (yOffset < MAX_HANG_LENGTH && level.getBlockState(pos.below(yOffset)).is(rope)) {
            if (level.getBlockState(pos.below(yOffset)).getValue(BlockRopeBase.AXIS) != Direction.Axis.Y) {
                return false;
            }
            yOffset++;
        }

        BlockPos target = pos.below(yOffset);
        // canPlaceBlockAt(pos) meant "is what is there replaceable".
        if (level.getBlockState(target).canBeReplaced()) {
            BlockState state = rope.defaultBlockState()
                    .setValue(BlockRopeBase.AXIS, Direction.Axis.Y)
                    .setValue(BlockRopeBase.DANGLE, false);
            level.setBlock(target, state, Block.UPDATE_ALL);
            // The legacy sound went to (x, y - yOffset, z): the corner of the block that was just added.
            RusticDrops.playPlaceSound(level, target, state);
            return true;
        }
        return false;
    }
}
