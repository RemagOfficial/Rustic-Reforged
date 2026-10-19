package rustic.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.items.RusticItems;
import rustic.content.items.ItemElixir;
import rustic.content.fluids.RusticFluids;

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
                        // Phase 15 exception: the apiary was setCreativeTab(farmingTab), not decorTab, so it is
                        // filtered out of this blanket append and listed under FARMING instead.
                        RusticMachineBlocks.entries().stream()
                                .filter(block -> block != RusticMachineBlocks.APIARY)
                                .forEach(block -> output.accept(block.get()));
                        // TODO Phase 3: the remaining decor families (candles, chains, chairs, tables,
                        //   lattice, lanterns, gargoyle, vase) append themselves to DECOR_FAMILIES.
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FARMING =
            CREATIVE_MODE_TABS.register("farming", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rustic.farming"))
                    // 1.12.2 icon: new ItemStack(ModItems.OLIVES) - the farming tab leads with olives.
                    .icon(() -> new ItemStack(RusticItems.OLIVES.get()))
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
                        // Phase 14 (§A3): the apple-seeds crop's seed. It is not in blockItemEntries() (its
                        // item is registered directly in RusticItems as the crop's BlockItem), so it is shown
                        // here - the legacy BlockAppleSeeds sat in farmingTab through setCreativeTab.
                        output.accept(RusticItems.APPLE_SEEDS.get());
                        // Phase 6 tree fruits: the two potion-foods the olives/ironberries leaf drop yields.
                        // OLIVES is also the tab icon (legacy ModItems.OLIVES); IRONBERRIES carries the stacking
                        // FULLMETAL effect. Both were in legacy's farmingTab through setCreativeTab.
                        output.accept(RusticItems.OLIVES.get());
                        output.accept(RusticItems.IRONBERRIES.get());
                        // Phase 6 fluid buckets. 1.12.2 overrode displayAllRelevantItems to add a filled universal
                        // bucket for every Rustic fluid; this port instead registers nine per-fluid buckets (the seven
                        // boozes stay bucket-less, preserving the observable 9/7 split), and they are shown directly.
                        output.accept(RusticItems.OLIVE_OIL_BUCKET.get());
                        output.accept(RusticItems.IRONBERRY_JUICE_BUCKET.get());
                        output.accept(RusticItems.WILDBERRY_JUICE_BUCKET.get());
                        output.accept(RusticItems.GRAPE_JUICE_BUCKET.get());
                        output.accept(RusticItems.APPLE_JUICE_BUCKET.get());
                        output.accept(RusticItems.ALE_WORT_BUCKET.get());
                        output.accept(RusticItems.HONEY_BUCKET.get());
                        output.accept(RusticItems.GOLDEN_APPLE_JUICE_BUCKET.get());
                        output.accept(RusticItems.VANTA_OIL_BUCKET.get());
                        // Phase 6 filled bottles. Legacy ItemFluidBottle.getSubItems seeded the tab with a full
                        // bottle of every Rustic fluid; the seven boozes came pre-filled at quality 0.75F, the rest
                        // plain (null). Faithful here - sixteen bottles, booze quality seeded on the fluid stack.
                        output.accept(fluidBottle(RusticFluids.OLIVE_OIL.value(), null));
                        output.accept(fluidBottle(RusticFluids.IRONBERRY_JUICE.value(), null));
                        output.accept(fluidBottle(RusticFluids.WILDBERRY_JUICE.value(), null));
                        output.accept(fluidBottle(RusticFluids.GRAPE_JUICE.value(), null));
                        output.accept(fluidBottle(RusticFluids.APPLE_JUICE.value(), null));
                        output.accept(fluidBottle(RusticFluids.ALE_WORT.value(), null));
                        output.accept(fluidBottle(RusticFluids.HONEY.value(), null));
                        output.accept(fluidBottle(RusticFluids.GOLDEN_APPLE_JUICE.value(), null));
                        output.accept(fluidBottle(RusticFluids.ALE.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.CIDER.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.IRON_WINE.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.MEAD.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.WILDBERRY_WINE.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.WINE.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.AMBROSIA.value(), 0.75F));
                        // Phase 15 (A1): the apiary. 1.12.2 registered it with setCreativeTab(farmingTab), so it
                        // appears here rather than riding the DECOR machine append (see the filter on that loop).
                        output.accept(RusticMachineBlocks.APIARY.get());
                        // Phase 15 follow-up: the six plain ItemBase materials. Every 1.12.2 ItemBase set
                        // setCreativeTab(Rustic.farmingTab) in its constructor (bee/honeycomb even repeated it
                        // explicitly in ModItems.init), so the bee chain and the basin/tallow materials belong
                        // in FARMING - without this the apiary's bee slot was unreachable in creative.
                        // (grape_seeds is a port-only extra with no legacy tab counterpart, so it stays out.)
                        output.accept(RusticItems.BEE.get());
                        output.accept(RusticItems.HONEYCOMB.get());
                        output.accept(RusticItems.BEESWAX.get());
                        output.accept(RusticItems.TALLOW.get());
                        output.accept(RusticItems.IRON_DUST_TINY.get());
                        output.accept(RusticItems.GOLD_DUST.get());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ALCHEMY =
            CREATIVE_MODE_TABS.register("alchemy", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rustic.alchemy"))
                    // 1.12.2 icon: ModItems.ELIXIR.getDefaultInstance() - the instant-health default elixir.
                    .icon(ItemElixir::defaultElixir)
                    .displayItems((parameters, output) -> {
                        // The legacy default instance first (getDefaultInstance seeded one tick of instant
                        // health); TODO Phase 7: the condenser-recipe elixirs getSubItems listed then.
                        output.accept(ItemElixir.defaultElixir());
                        // Vanta oil is shown as a filled bottle, the alchemy-side entry it had in legacy.
                        output.accept(fluidBottle(RusticFluids.VANTA_OIL.value(), null));
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    /** A full drinkable bottle of {@code fluid} for a tab entry; {@code quality} is the legacy booze seed. */
    private static ItemStack fluidBottle(Fluid fluid, Float quality) {
        return RusticItems.FLUID_BOTTLE.get().getFilledBottle(fluid, quality);
    }

    private RusticCreativeTabs() {
    }
}
