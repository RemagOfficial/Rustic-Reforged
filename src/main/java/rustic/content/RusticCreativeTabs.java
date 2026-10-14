package rustic.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.items.RusticItems;

/**
 * The three Rustic creative tabs from 1.12.2 ({@code Rustic.decorTab} / {@code farmingTab} /
 * {@code alchemyTab}), rebuilt as registered {@link CreativeModeTab}s.
 *
 * <p>1.12.2 gave the tabs labels like {@code "rustic.decor"} and pulled their icons straight from
 * mod objects ({@code ModBlocks.VASE}, {@code ModItems.OLIVES}, {@code ModItems.ELIXIR}). Those
 * blocks/items do not exist yet, so each icon is a vanilla stand-in marked with a TODO that gets
 * replaced when the owning phase registers the real content.
 */
public final class RusticCreativeTabs {

    /** Content classes that own a registry keep their own DeferredRegister; the shared ones live in RusticRegistries. */
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Rustic.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DECOR =
            CREATIVE_MODE_TABS.register("decor", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rustic.decor"))
                    // TODO Phase 3: replace with the rustic vase item (1.12.2: new ItemStack(ModBlocks.VASE))
                    .icon(() -> new ItemStack(Items.FLOWER_POT))
                    .displayItems((parameters, output) -> {
                        // 1.12.2 filled this tab by calling setCreativeTab(decorTab) inside BlockBase,
                        // BlockPillar and BlockStairsBase, so every decor block the config enabled was
                        // in it. The families in RusticBlocks are that grouping, and the flag that used
                        // to gate registration now only gates visibility here - turning a family off
                        // leaves already-placed blocks intact.
                        for (RusticBlocks.DecorFamily family : RusticBlocks.DECOR_FAMILIES) {
                            if (family.gate().get()) {
                                family.blocks().forEach(block -> output.accept(block.get()));
                            }
                        }
                        // Phase 4: 1.12.2 put the storage machines (barrel, cabinet, vase) in the same
                        // decorTab through setCreativeTab, so the machine register is appended here.
                        RusticMachineBlocks.entries().forEach(block -> output.accept(block.get()));
                        // TODO Phase 3: the remaining decor families (candles, chains, chairs, tables,
                        //   lattice, lanterns, gargoyle, vase) append themselves to DECOR_FAMILIES.
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FARMING =
            CREATIVE_MODE_TABS.register("farming", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rustic.farming"))
                    // TODO Phase 5: replace with the rustic olives item (1.12.2: new ItemStack(ModItems.OLIVES))
                    .icon(() -> new ItemStack(Items.WHEAT))
                    .displayItems((parameters, output) -> {
                        // 1.12.2 filled this tab the same way, through setCreativeTab(farmingTab) in the
                        // log, leaves and sapling constructors. The wood family's agriculture half is that
                        // grouping; the crops, herbs and fruit items of Phase 5 append to FARMING_FAMILIES.
                        for (RusticBlocks.DecorFamily family : RusticBlocks.FARMING_FAMILIES) {
                            if (family.gate().get()) {
                                family.blocks().forEach(block -> output.accept(block.get()));
                            }
                        }
                        // Phase 5: the two placeable poles (crop stake, tied stake) and the produce + seeds
                        // of the stake crops. The crops themselves have no item - they are picked as seeds -
                        // so only RusticFarmBlocks.blockItemEntries() (not every farm block) is shown here.
                        RusticFarmBlocks.blockItemEntries().forEach(holder -> output.accept(holder.get()));
                        output.accept(RusticItems.TOMATO_SEEDS.get());
                        output.accept(RusticItems.CHILI_PEPPER_SEEDS.get());
                        output.accept(RusticItems.TOMATO.get());
                        output.accept(RusticItems.CHILI_PEPPER.get());
                        output.accept(RusticItems.GHOST_PEPPER.get());
                        // Phase 5 herbs: the nine self-herbs already came through blockItemEntries(); the four
                        // crop-herbs show here as their ItemHerbEdible, which is both their seed and their produce
                        // (the crop-herb blocks have no block item of their own).
                        output.accept(RusticItems.CLOUDSBLUFF.get());
                        output.accept(RusticItems.CORE_ROOT.get());
                        output.accept(RusticItems.GINSENG.get());
                        output.accept(RusticItems.MARSH_MALLOW.get());
                        // Phase 5 vinery: the grapes produce. The placeable stem (shown as its legacy "Grape Seeds"
                        // block item) already came through blockItemEntries(); the leaves have no item and are not shown.
                        output.accept(RusticItems.GRAPES.get());
                        // Phase 5 berry bush: the placeable bush came through blockItemEntries(); its produce here.
                        output.accept(RusticItems.WILDBERRIES.get());
                        // TODO Phase 5: remaining berry/food items append here as those slices land.
                        // TODO Phase 6: 1.12.2 overrode displayAllRelevantItems to also add a filled
                        //   universal bucket for every Rustic fluid; that belongs here once the
                        //   fluids and their bucket items are registered.
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ALCHEMY =
            CREATIVE_MODE_TABS.register("alchemy", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rustic.alchemy"))
                    // TODO Phase 6: replace with the rustic elixir item (1.12.2: ModItems.ELIXIR.getDefaultInstance())
                    .icon(() -> new ItemStack(Items.POTION))
                    .displayItems((parameters, output) -> {
                        // TODO Phase 6: elixirs, booze bottles, potions and alchemy machines land here
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private RusticCreativeTabs() {
    }
}
