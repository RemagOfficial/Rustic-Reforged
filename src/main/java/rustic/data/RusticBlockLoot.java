package rustic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticMachineBlocks;

import java.util.List;
import java.util.Set;

/**
 * Block loot tables, replacing 1.12.2's implicit behaviour.
 *
 * <p>The legacy mod never wrote a loot table: 1.12.2 dropped a block's own item unless the block
 * declared a harvest level, and none of Rustic's decorative blocks did. Under 1.21 every block needs
 * an explicit table, and "drop myself" is exactly what the old code did, so that is all this
 * generates - with the two exceptions the block itself cannot express.
 *
 * <p>The first is a double slab, which 1.21 stores as a state of the slab block rather than as the
 * separate {@code *_doubleslab} block 1.12.2 registered and therefore has to say "two slabs" out loud.
 * The second is leaves: {@code BlockLeavesRustic.getDrops} rolled a one-in-twenty sapling, or the block
 * itself under silk touch or shears, which is vanilla's own leaf table except for one detail - it never
 * dropped the sticks vanilla adds, so {@link #leavesDrops} leaves them out too.
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
        for (RusticBlocks.WoodFamily wood : RusticBlocks.WOOD_FAMILIES) {
            add(wood.leaves().get(), leavesDrops(wood.leaves().get(), wood.sapling().get()));
        }
        for (Block block : knownBlocks()) {
            if (block instanceof SlabBlock) {
                add(block, createSlabItemTable(block));
            } else if (!(block instanceof LeavesBlock)) {
                // The leaves were written above: their drop is the sapling of the same wood, not itself.
                dropSelf(block);
            }
        }
    }

    /**
     * One wood's leaves table. {@code BlockLeavesRustic.getDrops} rolled {@code rand.nextInt(20)} for the
     * sapling and shortened that divisor by {@code 2 << fortune} down to a floor of 10 - 1/20, 1/16,
     * 1/12, 1/10 - which is exactly {@link #NORMAL_LEAVES_SAPLING_CHANCES}, and why the vanilla constant
     * is reused instead of a list of Rustic numbers. The dispatch on silk touch or shears is what the
     * legacy {@code getSilkTouchDrop} and {@code onSheared} overrides did: both handed back the leaf block.
     *
     * <p>The olives and ironberries the same method rolled at 1/6 and 1/16 arrive with the fruit items
     * they drop, in the phase that ports them.
     */
    private LootTable.Builder leavesDrops(Block leaves, Block sapling) {
        return createSilkTouchOrShearsDispatchTable(leaves,
                applyExplosionCondition(leaves, LootItem.lootTableItem(sapling))
                        .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
                                NORMAL_LEAVES_SAPLING_CHANCES)));
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
        // Both block registers: the decor and the Phase 4 machines need a drop-myself table alike.
        List<Block> blocks = new java.util.ArrayList<>(
                RusticBlocks.entries().stream().<Block>map(DeferredHolder::get).toList());
        RusticMachineBlocks.entries().forEach(holder -> blocks.add(holder.get()));
        return blocks;
    }
}
