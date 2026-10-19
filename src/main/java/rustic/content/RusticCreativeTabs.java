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

public final class RusticCreativeTabs {

    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Rustic.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DECOR =
            CREATIVE_MODE_TABS.register("decor", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rustic.decor"))

                    .icon(() -> new ItemStack(Items.FLOWER_POT))
                    .displayItems((parameters, output) -> {

                        accept(output, RusticConfig.ENABLE_PILLARS::get, RusticBlocks.STONE_PILLAR,
                                RusticBlocks.ANDESITE_PILLAR, RusticBlocks.DIORITE_PILLAR, RusticBlocks.GRANITE_PILLAR);

                        accept(output, () -> RusticConfig.ENABLE_PILLARS.get() && RusticConfig.ENABLE_SLATE.get(),
                                RusticBlocks.SLATE_PILLAR);
                        accept(output, RusticBlocks.CHAIN, RusticBlocks.CANDLE, RusticBlocks.CANDLE_DOUBLE,
                                RusticBlocks.CANDLE_LEVER, RusticBlocks.CHANDELIER, RusticBlocks.IRON_TORCH);
                        accept(output, RusticBlocks.CHAIN_GOLD, RusticBlocks.CANDLE_GOLD,
                                RusticBlocks.CANDLE_DOUBLE_GOLD, RusticBlocks.CANDLE_LEVER_GOLD,
                                RusticBlocks.CHANDELIER_GOLD);

                        if (RusticBlocks.SILVER_DECOR != null) {
                            accept(output, RusticBlocks.SILVER_DECOR.chain(), RusticBlocks.SILVER_DECOR.candle(),
                                    RusticBlocks.SILVER_DECOR.candleDouble(), RusticBlocks.SILVER_DECOR.candleLever(),
                                    RusticBlocks.SILVER_DECOR.chandelier());
                        }

                        accept(output, RusticConfig.ENABLE_SLATE::get, RusticBlocks.SLATE, RusticBlocks.SLATE_ROOF,
                                RusticBlocks.SLATE_TILE, RusticBlocks.SLATE_BRICK, RusticBlocks.SLATE_CHISELED,
                                RusticBlocks.SLATE_ROOF_STAIRS, RusticBlocks.SLATE_ROOF_SLAB,
                                RusticBlocks.SLATE_BRICK_STAIRS, RusticBlocks.SLATE_BRICK_SLAB);
                        accept(output, RusticConfig.ENABLE_CLAY_WALLS::get, RusticBlocks.CLAY_WALL,
                                RusticBlocks.CLAY_WALL_CROSS, RusticBlocks.CLAY_WALL_DIAG);

                        if (RusticConfig.ENABLE_CHAIRS.get()) {
                            RusticBlocks.FURNITURE.forEach(furniture -> output.accept(furniture.chair().get()));
                        }
                        if (RusticConfig.ENABLE_TABLES.get()) {
                            RusticBlocks.FURNITURE.forEach(furniture -> output.accept(furniture.table().get()));
                        }

                        accept(output, RusticMachineBlocks.VASE, RusticMachineBlocks.BARREL);
                        accept(output, RusticConfig.ENABLE_LATTICE::get, RusticBlocks.IRON_LATTICE);
                        accept(output, RusticBlocks.IRON_LANTERN, RusticBlocks.GOLDEN_LANTERN);
                        if (RusticBlocks.SILVER_DECOR != null) {
                            accept(output, RusticBlocks.SILVER_DECOR.lantern());
                        }

                        if (RusticConfig.ENABLE_PAINTED_WOOD.get()) {
                            RusticBlocks.PAINTED_WOOD.values().forEach(paintedWood -> output.accept(paintedWood.get()));
                        }
                        accept(output, RusticBlocks.GARGOYLE, RusticMachineBlocks.CABINET,
                                RusticMachineBlocks.LIQUID_BARREL);

                        accept(output, RusticBlocks.olive().planks(), RusticBlocks.ironwood().planks());
                        accept(output, RusticBlocks.olive().fence(), RusticBlocks.ironwood().fence());
                        accept(output, RusticBlocks.olive().fenceGate(), RusticBlocks.ironwood().fenceGate());
                        accept(output, RusticBlocks.olive().slab(), RusticBlocks.ironwood().slab());
                        accept(output, RusticBlocks.olive().stairs(), RusticBlocks.ironwood().stairs());

                        accept(output, RusticBlocks.ROPE);
                        accept(output, RusticBlocks.olive().door(), RusticBlocks.ironwood().door());
                        accept(output, RusticBlocks.LANTERN_WOOD);
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FARMING =
            CREATIVE_MODE_TABS.register("farming", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rustic.farming"))

                    .icon(() -> new ItemStack(RusticItems.OLIVES.get()))
                    .displayItems((parameters, output) -> {

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

                        output.accept(RusticItems.OLIVE_OIL_BUCKET.get());
                        output.accept(RusticItems.IRONBERRY_JUICE_BUCKET.get());
                        output.accept(RusticItems.WILDBERRY_JUICE_BUCKET.get());
                        output.accept(RusticItems.GRAPE_JUICE_BUCKET.get());
                        output.accept(RusticItems.APPLE_JUICE_BUCKET.get());
                        output.accept(RusticItems.ALE_WORT_BUCKET.get());
                        output.accept(RusticItems.HONEY_BUCKET.get());
                        output.accept(RusticItems.GOLDEN_APPLE_JUICE_BUCKET.get());
                        output.accept(RusticItems.VANTA_OIL_BUCKET.get());

                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ALCHEMY =
            CREATIVE_MODE_TABS.register("alchemy", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rustic.alchemy"))

                    .icon(ItemElixir::defaultElixir)
                    .displayItems((parameters, output) -> {

                        output.accept(RusticMachineBlocks.CONDENSER.get());
                        output.accept(RusticMachineBlocks.RETORT.get());
                        output.accept(RusticMachineBlocks.CONDENSER_ADVANCED.get());
                        output.accept(RusticMachineBlocks.RETORT_ADVANCED.get());

                        RusticFarmBlocks.tabSelfHerbs().forEach(herb -> output.accept(herb.get()));

                        output.accept(RusticItems.CLOUDSBLUFF.get());
                        output.accept(RusticItems.CORE_ROOT.get());
                        output.accept(RusticItems.GINSENG.get());
                        output.accept(RusticItems.MARSH_MALLOW.get());

                        output.accept(fluidBottle(RusticFluids.VANTA_OIL.value(), null));

                        ItemElixir.condenserElixirs().forEach(output::accept);
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private static ItemStack fluidBottle(Fluid fluid, Float quality) {
        return RusticItems.FLUID_BOTTLE.get().getFilledBottle(fluid, quality);
    }

    @SafeVarargs
    private static void accept(CreativeModeTab.Output output, DeferredBlock<? extends Block>... blocks) {
        for (var block : blocks) {
            output.accept(new ItemStack(block.get()));
        }
    }

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
