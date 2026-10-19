package rustic.data;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
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

public class RusticBlockLoot extends BlockLootSubProvider {

    public RusticBlockLoot(HolderLookup.Provider registries) {

        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected void generate() {
        for (RusticBlocks.WoodFamily wood : RusticBlocks.WOOD_FAMILIES) {
            add(wood.leaves().get(), leavesDrops(wood.leaves().get(), wood.sapling().get()));
        }

        add(RusticFarmBlocks.APPLE_LEAVES.get(),
                leavesDrops(RusticFarmBlocks.APPLE_LEAVES.get(), RusticFarmBlocks.APPLE_SAPLING.get()));
        add(RusticFarmBlocks.BEEHIVE.get(),
                beehiveDrops(RusticItems.HONEYCOMB.get(), RusticItems.BEE.get()));

        add(RusticFarmBlocks.TOMATO_CROP.get(),
                stakeCropTable(RusticFarmBlocks.TOMATO_CROP.get(), RusticItems.TOMATO_SEEDS.get(), RusticItems.TOMATO.get()));
        add(RusticFarmBlocks.CHILI_CROP.get(),
                stakeCropTable(RusticFarmBlocks.CHILI_CROP.get(), RusticItems.CHILI_PEPPER_SEEDS.get(),
                        RusticItems.CHILI_PEPPER.get()));

        add(RusticFarmBlocks.STAKE_TIED.get(),
                createSingleItemTable(RusticBlocks.ROPE.get().asItem()));

        for (var holder : RusticFarmBlocks.herbs()) {
            HerbBlock herb = holder.get();
            add(herb, herbTable(herb, herb.getHerb()));
        }

        add(RusticFarmBlocks.GRAPE_LEAVES.get(),
                grapesTable(RusticFarmBlocks.GRAPE_LEAVES.get(), RusticItems.GRAPES.get()));

        add(RusticFarmBlocks.WILDBERRY_BUSH.get(),
                berryTable(RusticFarmBlocks.WILDBERRY_BUSH.get(), RusticItems.WILDBERRIES.get()));

        for (Block block : knownBlocks()) {
            if (block instanceof StakeCropBlock || block instanceof StakeTiedBlock || block instanceof HerbBlock
                    || block instanceof GrapeLeavesBlock || block instanceof BerryBushBlock
                    || block instanceof BlockBeehive) {

                continue;
            }
            if (block instanceof SlabBlock) {
                add(block, createSlabItemTable(block));
            } else if (block instanceof DoorBlock) {

                add(block, createDoorTable(block));
            } else if (!(block instanceof LeavesBlock)) {

                dropSelf(block);
            }
        }
    }

    private LootTable.Builder stakeCropTable(
            Block cropBlock,
            Item seed,
            Item crop
    ) {
        return LootTable.lootTable()

                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(seed)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))))

                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(crop))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(cropBlock)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(StakeCropBlock.AGE, 3))));
    }

    private LootTable.Builder herbTable(Block herbBlock, Item herb) {
        return LootTable.lootTable()

                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(herb)))

                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(herb)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(herbBlock)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(HerbBlock.AGE, 3))));
    }

    private LootTable.Builder grapesTable(Block leaves, Item grapes) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(grapes))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(leaves)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(GrapeLeavesBlock.GRAPES, true))));
    }

    private LootTable.Builder berryTable(Block bush, Item berries) {
        return LootTable.lootTable()

                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(bush)))

                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(berries))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(bush)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(BerryBushBlock.AGE, 5))));
    }

    private LootTable.Builder leavesDrops(Block leaves, Block sapling) {
        return createSilkTouchOrShearsDispatchTable(leaves,
                applyExplosionCondition(leaves, LootItem.lootTableItem(sapling))
                        .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
                                NORMAL_LEAVES_SAPLING_CHANCES)));
    }

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

    private static List<Block> knownBlocks() {

        List<Block> blocks = new java.util.ArrayList<>(
                RusticBlocks.entries().stream().<Block>map(DeferredHolder::get).toList());
        RusticMachineBlocks.entries().forEach(holder -> blocks.add(holder.get()));
        RusticFarmBlocks.entries().forEach(holder -> blocks.add(holder.get()));
        return blocks;
    }
}
