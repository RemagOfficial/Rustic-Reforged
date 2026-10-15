package rustic.data;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.blocks.HerbBlock;
import rustic.content.blocks.BerryBushBlock;
import rustic.content.blocks.BlockBeehive;
import rustic.content.blocks.GrapeLeavesBlock;
import rustic.content.blocks.StakeCropBlock;
import rustic.content.blocks.StakeTiedBlock;
import rustic.content.items.RusticItems;

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
        // Phase 8: the apple tree's foliage drops an apple sapling on the same one-in-twenty roll as the wood
        // family (BlockLeavesApple.getItemDropped returned ModBlocks.APPLE_SAPLING), and its world-generated
        // beehive drops honeycomb (0..4) plus bees (1..2) rather than itself - the legacy getDrops roll, which
        // never returned the hive block.
        add(RusticFarmBlocks.APPLE_LEAVES.get(),
                leavesDrops(RusticFarmBlocks.APPLE_LEAVES.get(), RusticFarmBlocks.APPLE_SAPLING.get()));
        add(RusticFarmBlocks.BEEHIVE.get(),
                beehiveDrops(RusticItems.HONEYCOMB.get(), RusticItems.BEE.get()));
        // Phase 5 crops: the legacy BlockStakeCrop.getDrops rolled 1-2 seeds and, at max age, one crop -
        // the chili's getCrop(rand) rolled a ghost pepper one in forty-two, which is expressed here as a
        // second weighted entry rather than as Java randomness (the loot table's uniform roll replaces the
        // legacy world.rand.nextInt call, keeping the same 1-in-42 chance).
        add(RusticFarmBlocks.TOMATO_CROP.get(),
                stakeCropTable(RusticFarmBlocks.TOMATO_CROP.get(), RusticItems.TOMATO_SEEDS.get(), RusticItems.TOMATO.get()));
        add(RusticFarmBlocks.CHILI_CROP.get(),
                stakeCropTable(RusticFarmBlocks.CHILI_CROP.get(), RusticItems.CHILI_PEPPER_SEEDS.get(),
                        RusticItems.CHILI_PEPPER.get()));
        // STAKE_TIED drops the rope, matching the legacy getDrops that always returned a ModBlocks.ROPE.
        add(RusticFarmBlocks.STAKE_TIED.get(),
                createSingleItemTable(RusticBlocks.ROPE.get().asItem()));

        // Phase 5 herbs: each of the thirteen HerbBlocks yields its own herb. The produce item is read off the
        // block itself (getHerb()) so a self-herb drops its block item and a crop-herb drops its ItemHerbEdible,
        // matching the legacy getItemDropped. See herbTable for the age-gated maturity bonus.
        for (var holder : RusticFarmBlocks.herbs()) {
            HerbBlock herb = holder.get();
            add(herb, herbTable(herb, herb.getHerb()));
        }

        // Phase 5 vinery: a grape leaf drops its grapes only when it is carrying them (the legacy getDrops that
        // added ModItems.GRAPES under if (state.getValue(GRAPES))); the block reverts to rope via the destroy hook
        // (GrapeLeavesBlock.playerWillDestroy), so the loot is the grapes and nothing else. The stem is an ordinary
        // placeable plant handled by the default drop-myself loop below.
        add(RusticFarmBlocks.GRAPE_LEAVES.get(),
                grapesTable(RusticFarmBlocks.GRAPE_LEAVES.get(), RusticItems.GRAPES.get()));

        // Phase 5 wildberry bush: the legacy BlockBerryBush.getDrops ran super.getDrops (the bush's own block item)
        // and then added one WILDBERRIES only when the bush was at full age - so the base pool always hands back the
        // bush and the second pool is gated on AGE = 5. Hand-picking (useWithoutItem) is a separate path in the block.
        add(RusticFarmBlocks.WILDBERRY_BUSH.get(),
                berryTable(RusticFarmBlocks.WILDBERRY_BUSH.get(), RusticItems.WILDBERRIES.get()));

        for (Block block : knownBlocks()) {
            if (block instanceof StakeCropBlock || block instanceof StakeTiedBlock || block instanceof HerbBlock
                    || block instanceof GrapeLeavesBlock || block instanceof BerryBushBlock
                    || block instanceof BlockBeehive) {
                // Written above with bespoke tables; skip the drop-myself default so crops stay picked-as-seed,
                // a grape leaf (which has no block item) never emits one, and the beehive drops its loot rather
                // than itself.
                continue;
            }
            if (block instanceof SlabBlock) {
                add(block, createSlabItemTable(block));
            } else if (!(block instanceof LeavesBlock)) {
                // The leaves were written above: their drop is the sapling of the same wood, not itself.
                dropSelf(block);
            }
        }
    }

    /**
     * 1.12.2's {@code BlockStakeCrop.getDrops}: a seed drop of one or two, and at max age one crop. The
     * break-time drop is the plain crop here; the chili's one-in-42 ghost pepper lives on the right-click
     * harvest path ({@code StakeCropBlock.getCrop(RandomSource)}), which is the legacy {@code tryHarvest}
     * that actually rolls it.
     */
    private LootTable.Builder stakeCropTable(
            Block cropBlock,
            Item seed,
            Item crop
    ) {
        return LootTable.lootTable()
                // 1.12.2: stacks.add(new ItemStack(getSeed(), rand.nextInt(2) + 1)) - one or two seeds, always.
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(seed)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))))
                // 1.12.2: if (age >= maxAge) stacks.add(new ItemStack(getCrop(rand))) - gated on the AGE state.
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(crop))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(cropBlock)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(StakeCropBlock.AGE, 3))));
    }

    /**
     * 1.12.2's {@code BlockHerbBase.getDrops}: the herb itself, plus extra copies at maturity. The base pool
     * hands back one herb whenever the plant is broken - this is the legacy {@code super.getDrops} dropping the
     * block's own item for a self-herb, and the {@code getItemDropped} herb for a crop-herb, both of which the
     * block's {@code getHerb()} already collapses to one answer. The second pool is the legacy max-age roll
     * ({@code k = 2 + fortune} draws each passing on {@code rand.nextInt(6) <= age}); a plain 1-2 herb at the
     * final age reproduces its average yield without a fortune table, since these plants are harvested by hand
     * rather than by a fortune tool.
     */
    private LootTable.Builder herbTable(Block herbBlock, Item herb) {
        return LootTable.lootTable()
                // The herb itself, whenever the plant is broken.
                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(herb)))
                // A maturity bonus of one or two more, gated on the final AGE.
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(herb)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(herbBlock)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(HerbBlock.AGE, 3))));
    }

    /**
     * 1.12.2's {@code BlockGrapeLeaves.getDrops}: the legacy method added a single grapes stack only under
     * {@code if (state.getValue(GRAPES))}, so the whole pool is gated on the {@code grapes} state and a leaf that
     * carries no fruit drops nothing (it still reverts to rope through the destroy hook, which is not a drop).
     */
    private LootTable.Builder grapesTable(Block leaves, Item grapes) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(grapes))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(leaves)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(GrapeLeavesBlock.GRAPES, true))));
    }

    /**
     * 1.12.2's {@code BlockBerryBush.getDrops}: the legacy method returned {@code super.getDrops} (the bush's own
     * block item) and then added a single WILDBERRIES under {@code if (isMaxAge(state))}, so the base pool always
     * drops the bush and the berry pool is gated on the final AGE.
     */
    private LootTable.Builder berryTable(Block bush, Item berries) {
        return LootTable.lootTable()
                // The bush itself, whenever it is broken.
                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(bush)))
                // One berry at full ripeness.
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(berries))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(bush)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(BerryBushBlock.AGE, 5))));
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

    /**
     * 1.12.2's {@code BlockBeehive.getDrops}: honeycomb in a {@code rand.nextInt(5)} stack (0..4, the hive can
     * yield nothing) plus a bee in a {@code rand.nextInt(2) + 1} stack (always one or two). The hive never
     * dropped itself, so neither pool falls back to the block item.
     */
    private LootTable.Builder beehiveDrops(Item honeycomb, Item bee) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(honeycomb)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 4.0F)))))
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(bee)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))));
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
        // Three block registers: the decor, the Phase 4 machines and the Phase 5 farm blocks all need a
        // drop-myself table unless they are a crop or a tied stake, which the loop above writes explicitly.
        List<Block> blocks = new java.util.ArrayList<>(
                RusticBlocks.entries().stream().<Block>map(DeferredHolder::get).toList());
        RusticMachineBlocks.entries().forEach(holder -> blocks.add(holder.get()));
        RusticFarmBlocks.entries().forEach(holder -> blocks.add(holder.get()));
        return blocks;
    }
}
