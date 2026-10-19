package rustic.content.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rustic.content.blocks.HerbBlock;
import rustic.content.blocks.RusticDrops;

import java.util.function.Supplier;

public class ItemHerbEdible extends Item {

    private final Supplier<? extends HerbBlock> herbBlock;

    public ItemHerbEdible(Properties properties, Supplier<? extends HerbBlock> herbBlock) {
        super(properties);
        this.herbBlock = herbBlock;
    }

    public HerbBlock getHerbBlock() {
        return herbBlock.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos groundPos = context.getClickedPos();
        BlockPos plantPos = groundPos.above();
        BlockState herbState = herbBlock.get().defaultBlockState();

        if (!level.isEmptyBlock(plantPos) || !herbState.canSurvive(level, plantPos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(plantPos, herbState, Block.UPDATE_ALL);
            RusticDrops.playPlaceSound(level, plantPos, herbState);
            Player player = context.getPlayer();
            ItemStack stack = context.getItemInHand();
            if (player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
