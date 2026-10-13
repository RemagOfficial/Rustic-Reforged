package rustic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import rustic.content.blocks.RusticBlocks;

import java.util.List;
import java.util.Set;

/**
 * Block loot tables, replacing 1.12.2's implicit behaviour.
 *
 * <p>The legacy mod never wrote a loot table: 1.12.2 dropped a block's own item unless the block
 * declared a harvest level, and none of Rustic's decorative blocks did. Under 1.21 every block needs
 * an explicit table, and "drop myself" is exactly what the old code did, so that is all this
 * generates - with the one exception the block itself cannot express, a double slab, which 1.21
 * stores as a state of the slab block rather than as the separate {@code *_doubleslab} block 1.12.2
 * registered and therefore has to say "two slabs" out loud.
 */
public class RusticBlockLoot extends BlockLootSubProvider {

    public RusticBlockLoot(HolderLookup.Provider registries) {
        // The first argument lists the blocks whose drops survive an explosion, which none of these
        // are. The second is the set of enabled feature flags, and every registered block requires
        // FeatureFlags.VANILLA, so it has to be the default set: an empty one makes the generator skip
        // all of them and then complain that the tables it was handed belong to "non-blocks".
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected void generate() {
        for (Block block : knownBlocks()) {
            if (block instanceof SlabBlock) {
                add(block, createSlabItemTable(block));
            } else {
                dropSelf(block);
            }
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return knownBlocks();
    }

    /**
     * The default implementation returns every block in the game, which would make the generator
     * demand a table for each vanilla block as well, so the list is narrowed to Rustic's own.
     */
    private static List<Block> knownBlocks() {
        return RusticBlocks.entries().stream().<Block>map(DeferredHolder::get).toList();
    }
}
