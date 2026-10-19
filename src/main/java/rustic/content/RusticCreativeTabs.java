package rustic.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.config.RusticConfig;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.items.RusticItems;
import rustic.content.items.ItemElixir;
import rustic.content.fluids.RusticFluids;

import java.util.function.Supplier;

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
                        // Bug fix 2026-10-19: this list is the legacy Item.REGISTRY walk in creation
                        // order, not the DECOR_FAMILIES grouping. 1.12.2's decorTab had no getSubItems
                        // override, so its displayAllRelevantItems walked Item.REGISTRY and asked each
                        // decorTab item for ONE stack - except the meta items whose getSubBlocks fired on
                        // the null-tab pass (ItemBlockWithMetadata): the planks pair is the only decor
                        // block with two entries. The sequence below mirrors ModBlocks.java creation order
                        // line by line (oracle lines 171-400): pillars, then the metal mounts at their own
                        // creation spots - the silver candle set sits between the gold set and the slate
                        // blocks, while silver_lantern stays with the lanterns, so silver is split exactly
                        // as legacy registered it. The almanac book is NOT listed: Patchouli appends its
                        // guide_book to the book.json creative_tab after this list runs, which lands it
                        // last - where legacy's ItemBook registration put it.
                        accept(output, RusticConfig.ENABLE_PILLARS::get, RusticBlocks.STONE_PILLAR,
                                RusticBlocks.ANDESITE_PILLAR, RusticBlocks.DIORITE_PILLAR, RusticBlocks.GRANITE_PILLAR);
                        // Slate pillar is nested inside the pillars block (ModBlocks lines 171-179 -
                        // if (ENABLE_PILLARS) { four stone pillars; if (ENABLE_SLATE) SLATE_PILLAR }), so it
                        // was created at registry index 5, BEFORE the chain. The rest of the SLATE family
                        // lands later at its own if (ENABLE_SLATE) block (ModBlocks lines 200-215), which is
                        // why the pillar sits here and the block set sits lower down.
                        accept(output, () -> RusticConfig.ENABLE_PILLARS.get() && RusticConfig.ENABLE_SLATE.get(),
                                RusticBlocks.SLATE_PILLAR);
                        accept(output, RusticBlocks.CHAIN, RusticBlocks.CANDLE, RusticBlocks.CANDLE_DOUBLE,
                                RusticBlocks.CANDLE_LEVER, RusticBlocks.CHANDELIER, RusticBlocks.IRON_TORCH);
                        accept(output, RusticBlocks.CHAIN_GOLD, RusticBlocks.CANDLE_GOLD,
                                RusticBlocks.CANDLE_DOUBLE_GOLD, RusticBlocks.CANDLE_LEVER_GOLD,
                                RusticBlocks.CHANDELIER_GOLD);
                        // The silver candle set, created right after the gold one (ModBlocks lines 191-197);
                        // these six only exist when the compat flag registered them at all.
                        if (RusticBlocks.SILVER_DECOR != null) {
                            accept(output, RusticBlocks.SILVER_DECOR.chain(), RusticBlocks.SILVER_DECOR.candle(),
                                    RusticBlocks.SILVER_DECOR.candleDouble(), RusticBlocks.SILVER_DECOR.candleLever(),
                                    RusticBlocks.SILVER_DECOR.chandelier());
                        }
                        // The rest of the slate family, its own if (ENABLE_SLATE) block after beehive/apiary
                        // (ModBlocks lines 200-215). SLATE_PILLAR itself is listed above, at its legacy index.
                        accept(output, RusticConfig.ENABLE_SLATE::get, RusticBlocks.SLATE, RusticBlocks.SLATE_ROOF,
                                RusticBlocks.SLATE_TILE, RusticBlocks.SLATE_BRICK, RusticBlocks.SLATE_CHISELED,
                                RusticBlocks.SLATE_ROOF_STAIRS, RusticBlocks.SLATE_ROOF_SLAB,
                                RusticBlocks.SLATE_BRICK_STAIRS, RusticBlocks.SLATE_BRICK_SLAB);
                        accept(output, RusticConfig.ENABLE_CLAY_WALLS::get, RusticBlocks.CLAY_WALL,
                                RusticBlocks.CLAY_WALL_CROSS, RusticBlocks.CLAY_WALL_DIAG);
                        // All eight chairs before any table - the legacy file created the two groups in
                        // that order (ModBlocks lines 224-243), in FURNITURE_WOODS' own sequence.
                        if (RusticConfig.ENABLE_CHAIRS.get()) {
                            RusticBlocks.FURNITURE.forEach(furniture -> output.accept(furniture.chair().get()));
                        }
                        if (RusticConfig.ENABLE_TABLES.get()) {
                            RusticBlocks.FURNITURE.forEach(furniture -> output.accept(furniture.table().get()));
                        }
                        // Vase and barrel at their creation spot (line 244-245), not appended at the end.
                        accept(output, RusticMachineBlocks.VASE, RusticMachineBlocks.BARREL);
                        accept(output, RusticConfig.ENABLE_LATTICE::get, RusticBlocks.IRON_LATTICE);
                        accept(output, RusticBlocks.IRON_LANTERN, RusticBlocks.GOLDEN_LANTERN);
                        if (RusticBlocks.SILVER_DECOR != null) {
                            accept(output, RusticBlocks.SILVER_DECOR.lantern());
                        }
                        // The sixteen painted woods in EnumMap order, which is the legacy white-to-black
                        // creation sequence (ModBlocks lines 254-286).
                        if (RusticConfig.ENABLE_PAINTED_WOOD.get()) {
                            RusticBlocks.PAINTED_WOOD.values().forEach(paintedWood -> output.accept(paintedWood.get()));
                        }
                        accept(output, RusticBlocks.GARGOYLE, RusticMachineBlocks.CABINET,
                                RusticMachineBlocks.LIQUID_BARREL);
                        // The wood decor, interleaved BY PART the way legacy created it (lines 308-345,
                        // 398-399): both planks as the one meta item's two variants (the legacy
                        // getSubBlocks listed olive before ironwood), then both fences, both gates, both
                        // slabs, both stairs, and the doors only after the rope.
                        accept(output, RusticBlocks.olive().planks(), RusticBlocks.ironwood().planks());
                        accept(output, RusticBlocks.olive().fence(), RusticBlocks.ironwood().fence());
                        accept(output, RusticBlocks.olive().fenceGate(), RusticBlocks.ironwood().fenceGate());
                        accept(output, RusticBlocks.olive().slab(), RusticBlocks.ironwood().slab());
                        accept(output, RusticBlocks.olive().stairs(), RusticBlocks.ironwood().stairs());
                        // The rope is BlockBase with register=true (legacy BlockRope line 33), so it is a
                        // decorTab member at its own creation spot between the stairs and the doors.
                        accept(output, RusticBlocks.ROPE);
                        accept(output, RusticBlocks.olive().door(), RusticBlocks.ironwood().door());
                        accept(output, RusticBlocks.LANTERN_WOOD);
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FARMING =
            CREATIVE_MODE_TABS.register("farming", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rustic.farming"))
                    // 1.12.2 icon: new ItemStack(ModItems.OLIVES) - the farming tab leads with olives.
                    .icon(() -> new ItemStack(RusticItems.OLIVES.get()))
                    .displayItems((parameters, output) -> {
                        // Bug fix 2026-10-19: this list is the legacy Item.REGISTRY walk in creation
                        // order, not a grouping by family. 1.12.2's CommonProxy.preInit ran ModBlocks.init
                        // before ModItems.init, so every farmingTab BLOCK item comes first, in the exact
                        // sequence ModBlocks.java created them (oracle lines 198-397), and the farmingTab
                        // ITEMS (ModItems.init order) follow. stake_tied, tomato_crop and chili_crop are
                        // absent because those legacy classes registered no item at all
                        // (BlockBase register=false + block-only registration), and grape_leaves has no
                        // item either. The legacy tab had no config gate on any of these members.
                        output.accept(RusticFarmBlocks.BEEHIVE.get());
                        output.accept(RusticMachineBlocks.APIARY.get());
                        output.accept(RusticBlocks.FERTILE_SOIL.get());
                        output.accept(RusticBlocks.olive().log().get());
                        output.accept(RusticBlocks.ironwood().log().get());
                        output.accept(RusticBlocks.olive().leaves().get());
                        output.accept(RusticBlocks.ironwood().leaves().get());
                        output.accept(RusticBlocks.olive().sapling().get());
                        output.accept(RusticBlocks.ironwood().sapling().get());
                        output.accept(RusticMachineBlocks.CRUSHING_TUB.get());
                        output.accept(RusticMachineBlocks.EVAPORATING_BASIN.get());
                        output.accept(RusticFarmBlocks.CROP_STAKE.get());
                        output.accept(RusticFarmBlocks.WILDBERRY_BUSH.get());
                        output.accept(RusticFarmBlocks.GRAPE_STEM.get());
                        output.accept(RusticMachineBlocks.BREWING_BARREL.get());
                        output.accept(RusticItems.APPLE_SEEDS.get());
                        output.accept(RusticFarmBlocks.APPLE_SAPLING.get());
                        output.accept(RusticFarmBlocks.APPLE_LEAVES.get());
                        // Then the ModItems.init half, in register order: the bee chain and the tree fruits
                        // (BEESWAX/TALLOW ride the legacy ItemBase default farmingTab), the sixteen fluid
                        // bottles the legacy ItemFluidBottle.getSubItems listed - the seven boozes pre- seeded
                        // at quality 0.75F - the two dusts, and the stake-crop produce and seeds.
                        output.accept(RusticItems.BEE.get());
                        output.accept(RusticItems.HONEYCOMB.get());
                        output.accept(RusticItems.BEESWAX.get());
                        output.accept(RusticItems.TALLOW.get());
                        output.accept(RusticItems.OLIVES.get());
                        output.accept(RusticItems.IRONBERRIES.get());
                        output.accept(fluidBottle(RusticFluids.OLIVE_OIL.value(), null));
                        output.accept(fluidBottle(RusticFluids.IRONBERRY_JUICE.value(), null));
                        output.accept(fluidBottle(RusticFluids.WILDBERRY_JUICE.value(), null));
                        output.accept(fluidBottle(RusticFluids.GRAPE_JUICE.value(), null));
                        output.accept(fluidBottle(RusticFluids.APPLE_JUICE.value(), null));
                        output.accept(fluidBottle(RusticFluids.ALE_WORT.value(), null));
                        output.accept(fluidBottle(RusticFluids.HONEY.value(), null));
                        output.accept(fluidBottle(RusticFluids.GOLDEN_APPLE_JUICE.value(), null));
                        // Legacy getSubItems(farmingTab) looped every VALID_FLUIDS entry, so the vanta oil bottle
                        // appears here too - initFluidBottle adds it ninth, a plain drinkable (not a booze). The
                        // item was a member of BOTH tabs, so it is also the alchemy branch's only bottle (see ALCHEMY).
                        output.accept(fluidBottle(RusticFluids.VANTA_OIL.value(), null));
                        output.accept(fluidBottle(RusticFluids.ALE.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.CIDER.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.IRON_WINE.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.MEAD.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.WILDBERRY_WINE.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.WINE.value(), 0.75F));
                        output.accept(fluidBottle(RusticFluids.AMBROSIA.value(), 0.75F));
                        output.accept(RusticItems.IRON_DUST_TINY.get());
                        output.accept(RusticItems.GOLD_DUST.get());
                        output.accept(RusticItems.TOMATO.get());
                        output.accept(RusticItems.TOMATO_SEEDS.get());
                        output.accept(RusticItems.CHILI_PEPPER.get());
                        output.accept(RusticItems.GHOST_PEPPER.get());
                        output.accept(RusticItems.CHILI_PEPPER_SEEDS.get());
                        output.accept(RusticItems.WILDBERRIES.get());
                        output.accept(RusticItems.GRAPES.get());
                        // Last, and only here: 1.12.2's farmingTab override of displayAllRelevantItems ran
                        // the registry walk above and THEN appended a filled universal bucket for every
                        // ModFluids.getFluids() entry, so the buckets trail the whole tab. This port
                        // registers nine per-fluid buckets (the seven boozes stay bucket-less, preserving
                        // the observable 9/7 split) in that same fluid order.
                        output.accept(RusticItems.OLIVE_OIL_BUCKET.get());
                        output.accept(RusticItems.IRONBERRY_JUICE_BUCKET.get());
                        output.accept(RusticItems.WILDBERRY_JUICE_BUCKET.get());
                        output.accept(RusticItems.GRAPE_JUICE_BUCKET.get());
                        output.accept(RusticItems.APPLE_JUICE_BUCKET.get());
                        output.accept(RusticItems.ALE_WORT_BUCKET.get());
                        output.accept(RusticItems.HONEY_BUCKET.get());
                        output.accept(RusticItems.GOLDEN_APPLE_JUICE_BUCKET.get());
                        output.accept(RusticItems.VANTA_OIL_BUCKET.get());
                        // The nine self-herbs are filtered out: 1.12.2's BlockHerbBase set
                        // setCreativeTab(alchemyTab), so the herbs belong to ALCHEMY, not FARMING, and the
                        // four crop-herb edibles (ItemHerbEdible) are alchemyTab items too.
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ALCHEMY =
            CREATIVE_MODE_TABS.register("alchemy", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rustic.alchemy"))
                    // 1.12.2 icon: ModItems.ELIXIR.getDefaultInstance() - the instant-health default elixir.
                    .icon(ItemElixir::defaultElixir)
                    .displayItems((parameters, output) -> {
                        // 1.12.2 filled this tab through its setCreativeTab(alchemyTab) members, which the
                        // Item.REGISTRY walk rendered in registration order. That order reproduces here:
                        // the still blocks first, in ModBlocks creation order (condenser, retort, then the
                        // advanced pair - not the port's register order, which groups by tier).
                        output.accept(RusticMachineBlocks.CONDENSER.get());
                        output.accept(RusticMachineBlocks.RETORT.get());
                        output.accept(RusticMachineBlocks.CONDENSER_ADVANCED.get());
                        output.accept(RusticMachineBlocks.RETORT_ADVANCED.get());
                        // The nine self-herbs as block items, in legacy Herbs.init creation order.
                        RusticFarmBlocks.tabSelfHerbs().forEach(herb -> output.accept(herb.get()));
                        // The four crop-herbs as their ItemHerbEdible (both seed and produce), in the order
                        // the edibles were registered at the end of Herbs.init.
                        output.accept(RusticItems.CLOUDSBLUFF.get());
                        output.accept(RusticItems.CORE_ROOT.get());
                        output.accept(RusticItems.GINSENG.get());
                        output.accept(RusticItems.MARSH_MALLOW.get());
                        // The fluid bottle sat in both tabs; its alchemy-branch getSubItems listed exactly the
                        // vanta oil bottle, and the bottle registered (ModItems line 104) before the elixir.
                        output.accept(fluidBottle(RusticFluids.VANTA_OIL.value(), null));
                        // And every condenser recipe result, the legacy getSubItems walk order (its first
                        // entry is the default instance the tab icon shows).
                        ItemElixir.condenserElixirs().forEach(output::accept);
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    /** A full drinkable bottle of {@code fluid} for a tab entry; {@code quality} is the legacy booze seed. */
    private static ItemStack fluidBottle(Fluid fluid, Float quality) {
        return RusticItems.FLUID_BOTTLE.get().getFilledBottle(fluid, quality);
    }

    /** Accepts each of {@code blocks} as one tab entry, in the order they are given. */
    @SafeVarargs
    private static void accept(CreativeModeTab.Output output, DeferredBlock<? extends Block>... blocks) {
        for (var block : blocks) {
            output.accept(new ItemStack(block.get()));
        }
    }

    /** The same behind one config gate - the 1.21 form of the legacy {@code if (Config.ENABLE_*)} block. */
    @SafeVarargs
    private static void accept(CreativeModeTab.Output output, Supplier<Boolean> gate,
                              DeferredBlock<? extends Block>... blocks) {
        if (gate.get()) {
            accept(output, blocks);
        }
    }

    private RusticCreativeTabs() {
    }
}
