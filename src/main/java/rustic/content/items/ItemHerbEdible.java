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

/**
 * 1.12.2's {@code ItemHerbEdible}: the four crop-herb produce items - cloudsbluff, core root, ginseng and
 * marsh mallow - which are food <em>and</em> their own seed. Unlike the self-herbs, whose item is just the
 * block's {@code BlockItem}, these blocks are registered with no block item at all: this stands in for it
 * under the very same id (the legacy class set its registry name to the herb block's), and its
 * {@code onItemUse} planted the herb, while the food values came from the legacy {@code ItemFoodBase}
 * arguments.
 *
 * <p>The food itself is declared entirely through {@link Item.Properties#food}, so the eating/energy/levitation
 * behaviour is data the caller passes in the properties - this class adds nothing to it. The only code here
 * is planting: clicking the edible onto soil that will take a {@link HerbBlock} raises an age-0 plant, which
 * is the 1.21 form of the legacy {@code IPlantable}/{@code onItemUse} pair.
 *
 * <p>The herb block is a {@link Supplier} because the block and this item register in different classes and
 * neither can read the other while its own static initialiser runs; it is only resolved at click time.
 */
public class ItemHerbEdible extends Item {

    private final Supplier<? extends HerbBlock> herbBlock;

    public ItemHerbEdible(Properties properties, Supplier<? extends HerbBlock> herbBlock) {
        super(properties);
        this.herbBlock = herbBlock;
    }

    public HerbBlock getHerbBlock() {
        return herbBlock.get();
    }

    /**
     * 1.12.2's {@code onItemUse}: plant an age-0 herb on the clicked ground when that ground will take it and
     * the cell above is clear. The soil test is the block's own {@link BlockState#canSurvive}, the same one
     * {@code rustic.content.blocks.CropStakeBlock}'s seed-clicking reuses, so a herb only takes root where it
     * would also survive a random tick. Consumes one stack outside creative, exactly as the legacy
     * {@code stack.shrink} guarded on the creative check did.
     */
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
