package rustic.content.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import rustic.content.blocks.StakeCropBlock;

import java.util.function.Supplier;

public class StakeCropSeedItem extends Item {

    private final Supplier<? extends StakeCropBlock> crop;

    public StakeCropSeedItem(Properties properties, Supplier<? extends StakeCropBlock> crop) {
        super(properties);
        this.crop = crop;
    }

    public StakeCropBlock getCrop() {
        return crop.get();
    }

    public BlockState getCropState() {
        return crop.get().defaultBlockState();
    }
}
