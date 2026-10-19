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

public class RopeDispenseBehavior extends DefaultDispenseItemBehavior {

    private static final int MAX_HANG_LENGTH = BlockRopeBase.MAX_HANG_LENGTH;

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        ServerLevel level = source.level();
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        BlockPos pos = source.pos().relative(facing);
        BlockState front = level.getBlockState(pos);

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

        return super.execute(source, stack);
    }

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

        if (level.getBlockState(target).canBeReplaced()) {
            BlockState state = rope.defaultBlockState()
                    .setValue(BlockRopeBase.AXIS, Direction.Axis.Y)
                    .setValue(BlockRopeBase.DANGLE, false);
            level.setBlock(target, state, Block.UPDATE_ALL);

            RusticDrops.playPlaceSound(level, target, state);
            return true;
        }
        return false;
    }
}
