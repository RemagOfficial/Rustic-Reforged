package rustic.content.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import rustic.content.blocks.StakeCropBlock;

import java.util.function.Supplier;

/**
 * 1.12.2's {@code ItemStakeCropSeed}: a plain seed item that carries the one {@link StakeCropBlock} it
 * plants. The planting itself is not here - it is {@code rustic.content.blocks.CropStakeBlock}'s
 * {@code useItemOn}, which recognises this item and asks it for the crop's default (age-0) state, exactly as
 * the legacy {@code getCropState()} fed {@code world.setBlockState} from the stake's activation.
 *
 * <p>The crop is a {@link Supplier} for the same reason {@link StakeCropBlock} takes its items as suppliers:
 * the seed and the crop block register in different classes, and neither can read the other while its own
 * static initialiser still runs.
 */
public class StakeCropSeedItem extends Item {

    private final Supplier<? extends StakeCropBlock> crop;

    public StakeCropSeedItem(Properties properties, Supplier<? extends StakeCropBlock> crop) {
        super(properties);
        this.crop = crop;
    }

    public StakeCropBlock getCrop() {
        return crop.get();
    }

    /** The age-0 state the stake turns into when this seed is clicked onto it. */
    public BlockState getCropState() {
        return crop.get().defaultBlockState();
    }
}
