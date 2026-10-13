package rustic.content.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.content.blocks.RusticBlocks;

/**
 * Item registration, ported from 1.12.2's {@code ModItems} plus the item half of {@code ModBlocks}.
 *
 * <p>The 1.12.2 codebase never had a block item problem to solve: every {@code BlockBase} subclass
 * registered its own {@code ItemBlock} from inside its constructor, using the block's own registry
 * name. That implicit loop is what this class reproduces explicitly - one {@link BlockItem} per
 * Rustic block, same id - which also means a later phase cannot add a block and forget its item.
 *
 * <p>The non-block items of {@code ModItems} (olives, tomato, seeds, buckets, bottle, book, ...) are
 * not here yet; they belong to the phases that port the items themselves.
 *
 * <p>Ordering note: this class must be touched from the mod constructor, like
 * {@link RusticBlocks}, so that both registers are attached to the bus before the loader fires
 * {@code RegisterEvent}. Reading {@link RusticBlocks} first is what makes the block list complete by
 * the time the items are derived from it.
 */
public final class RusticItems {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Rustic.MODID);

    static {
        // 1.12.2: new ItemBlock(this).setRegistryName(getRegistryName()) inside BlockBase.register()
        for (DeferredHolder<Block, ? extends Block> block : RusticBlocks.entries()) {
            ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
        }
    }

    /** Attaches the item register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private RusticItems() {
    }
}
