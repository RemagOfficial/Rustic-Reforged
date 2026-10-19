package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import rustic.content.RusticCreativeTabs;
import rustic.content.blocks.RusticBlocks;
import rustic.content.fluids.RusticFluids;
import rustic.content.items.ItemFluidBottle;
import rustic.content.items.RusticItems;

/**
 * Bug-fix gate (2026-10-19) for the FARMING and DECOR creative tabs: 1.12.2 rendered them through the
 * plain {@code Item.REGISTRY} walk inherited from {@code CreativeTabs.displayAllRelevantItems}, so their
 * entries came out in CREATION order - {@code ModBlocks.init} first (including the nested
 * {@code Herbs.init}), {@code ModItems.init} after - with one twist: the farmingTab override appended a
 * filled bucket for every {@code ModFluids.getFluids()} entry LAST, after the walk. The port's tabs are
 * explicit hand-lists, so this pins their order against the oracle
 * ({@code ModBlocks.java} lines 171-400, {@code ModItems.java} lines 54-190, {@code Rustic.java}
 * lines 81-106), the same way {@code RusticAlchemyTabTest} pins ALCHEMY.
 *
 * <p>Membership deltas the order exposes: stake_tied, tomato_crop, chili_crop and grape_leaves registered NO
 * item in legacy (block-only registration), so they must not appear in FARMING - and the port now leaves them
 * item-less too (stake_tied is not in {@code RusticFarmBlocks.blockItemEntries}); the almanac book is appended
 * by Patchouli after the DECOR list runs, which is why it is absent from the golden below and must nevertheless
 * be last in-game.
 */
public class RusticTabOrderTest {

    /** The fluid order the legacy initFluidBottle loop - and therefore the port's bottle entries - uses. */
    private static final List<Fluid> BOTTLE_FLUIDS = List.of(
            RusticFluids.OLIVE_OIL.value(), RusticFluids.IRONBERRY_JUICE.value(),
            RusticFluids.WILDBERRY_JUICE.value(), RusticFluids.GRAPE_JUICE.value(),
            RusticFluids.APPLE_JUICE.value(), RusticFluids.ALE_WORT.value(),
            RusticFluids.HONEY.value(), RusticFluids.GOLDEN_APPLE_JUICE.value(),
            RusticFluids.VANTA_OIL.value(), RusticFluids.ALE.value(),
            RusticFluids.CIDER.value(), RusticFluids.IRON_WINE.value(),
            RusticFluids.MEAD.value(), RusticFluids.WILDBERRY_WINE.value(),
            RusticFluids.WINE.value(), RusticFluids.AMBROSIA.value());

    // ------------------------------------------------------------------ FARMING

    /** Every farmingTab member in the legacy walk order: ModBlocks creation order, then ModItems, then buckets. */
    @Test
    public void theFarmingTabFollowsTheLegacyCreationOrder() {
        List<String> expected = new ArrayList<>(List.of(
                // ModBlocks.init, line by line (198-397): the farmingTab blocks in creation order.
                "beehive", "apiary", "fertile_soil",
                "log_olive", "log_ironwood", "leaves_olive", "leaves_ironwood",
                "sapling_olive", "sapling_ironwood",
                "crushing_tub", "evaporating_basin", "crop_stake", "wildberry_bush", "grape_stem",
                "brewing_barrel", "apple_seeds", "sapling_apple",
                "leaves_apple"));
        // ModItems.init (57-189): the bee chain, the fruits, the sixteen bottles (one fluid_bottle path
        // per getSubBlocks entry), the dusts, then the stake-crop produce in field order.
        expected.addAll(List.of("bee", "honeycomb", "beeswax", "tallow", "olives", "ironberries"));
        for (int i = 0; i < BOTTLE_FLUIDS.size(); i++) {
            expected.add("fluid_bottle");
        }
        expected.addAll(List.of("dust_tiny_iron", "dust_gold",
                "tomato", "tomato_seeds", "chili_pepper", "ghost_pepper", "chili_pepper_seeds",
                "wildberries", "grapes"));
        // And last, from the farmingTab displayAllRelevantItems override: one bucket per fluid, in
        // ModFluids order - the nine the port registers (the boozes stayed bucket-less).
        expected.addAll(List.of("olive_oil_bucket", "ironberry_juice_bucket", "wildberry_juice_bucket",
                "grape_juice_bucket", "apple_juice_bucket", "ale_wort_bucket", "honey_bucket",
                "golden_apple_juice_bucket", "vanta_oil_bucket"));

        assertEquals(expected, tabPaths(RusticCreativeTabs.FARMING.get()),
                "the FARMING tab is not the legacy membership in legacy creation order");
    }

    /** The sixteen bottle entries name the legacy fluids in the legacy order, not just the same item. */
    @Test
    public void theFarmingBottlesCarryTheLegacyFluidOrder() {
        List<ItemStack> shown = displayItems(RusticCreativeTabs.FARMING.get());
        List<ItemStack> bottles = shown.stream()
                .filter(stack -> stack.is(RusticItems.FLUID_BOTTLE.get()))
                .toList();
        assertEquals(BOTTLE_FLUIDS.size(), bottles.size(), "sixteen filled bottles, like initFluidBottle");
        for (int i = 0; i < BOTTLE_FLUIDS.size(); i++) {
            assertSame(BOTTLE_FLUIDS.get(i), ItemFluidBottle.getFluidContent(bottles.get(i)).getFluid(),
                    "bottle entry " + i + " must hold " + BOTTLE_FLUIDS.get(i));
        }
    }

    /** The four item-less legacy farm blocks stay out of the tab and have no registered item at all. */
    @Test
    public void theItemLessLegacyBlocksStayOutOfFarming() {
        List<String> farming = tabPaths(RusticCreativeTabs.FARMING.get());
        for (String itemless : List.of("stake_tied", "tomato_crop", "chili_crop", "grape_leaves")) {
            assertTrue(!farming.contains(itemless),
                    "rustic:" + itemless + " had no legacy item, so it must not be offered in FARMING");
        }
        // stake_tied keeps its BLOCK (so existing worlds load), but - exactly as legacy's block-only
        // BlockStakeTied - it has no item form: it is not in RusticFarmBlocks.blockItemEntries, so the id is
        // absent from the item registry and resolves to the minecraft:air default rather than a BlockItem.
        assertEquals("minecraft",
                BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.get(ResourceLocation.parse("rustic:stake_tied"))).getNamespace(),
                "stake_tied must have no registered item, matching legacy's block-only registration");
    }

    // ------------------------------------------------------------------ DECOR

    /**
     * Every decorTab member in the legacy walk order, read straight off ModBlocks.java creation lines
     * 171-400: the four stone pillars then slate_pillar (all inside if (ENABLE_PILLARS), with slate
     * nested one level deeper at line 177), then the metal mounts (silver split between the gold set
     * and the slate blocks, its lantern with the lanterns), the rest of the slate family, clay walls,
     * chairs before tables, vase and barrel mid-list, the painted woods, gargoyle/cabinet/liquid barrel,
     * the wood decor interleaved by part, rope between the stairs and the doors, the wooden lantern last.
     */
    @Test
    public void theDecorTabFollowsTheLegacyCreationOrder() {
        List<String> expected = new ArrayList<>(List.of(
                "stone_pillar", "andesite_pillar", "diorite_pillar", "granite_pillar", "slate_pillar",
                "chain", "candle", "candle_double", "candle_lever", "chandelier", "iron_torch",
                "chain_gold", "candle_gold", "candle_double_gold", "candle_lever_gold", "chandelier_gold"));
        if (RusticBlocks.silverDecorEnabled()) {
            expected.addAll(List.of("chain_silver", "candle_silver", "candle_double_silver",
                    "candle_lever_silver", "chandelier_silver"));
        }
        expected.addAll(List.of(
                "slate", "slate_roof", "slate_tile", "slate_brick", "slate_chiseled",
                "stairs_slate_roof", "slate_roof_slab", "stairs_slate_brick", "slate_brick_slab",
                "clay_wall", "clay_wall_cross", "clay_wall_diag"));
        for (String wood : RusticBlocks.FURNITURE_WOODS) {
            expected.add("chair_" + wood);
        }
        for (String wood : RusticBlocks.FURNITURE_WOODS) {
            expected.add("table_" + wood);
        }
        expected.addAll(List.of("vase", "barrel", "iron_lattice", "iron_lantern", "golden_lantern"));
        if (RusticBlocks.silverDecorEnabled()) {
            expected.add("silver_lantern");
        }
        // The legacy white -> black sequence, which is EnumMap/DyeColor order and the PAINTED_WOOD key order.
        expected.addAll(List.of(
                "painted_wood_white", "painted_wood_orange", "painted_wood_magenta", "painted_wood_light_blue",
                "painted_wood_yellow", "painted_wood_lime", "painted_wood_pink", "painted_wood_gray",
                "painted_wood_silver", "painted_wood_cyan", "painted_wood_purple", "painted_wood_blue",
                "painted_wood_brown", "painted_wood_green", "painted_wood_red", "painted_wood_black"));
        expected.addAll(List.of(
                "gargoyle", "cabinet", "liquid_barrel",
                "planks_olive", "planks_ironwood",
                "fence_olive", "fence_ironwood", "fence_gate_olive", "fence_gate_ironwood",
                "olive_slab", "ironwood_slab", "stairs_olive", "stairs_ironwood",
                "rope", "olive_door", "ironwood_door", "lantern_wood"));

        assertEquals(expected, tabPaths(RusticCreativeTabs.DECOR.get()),
                "the DECOR tab is not the legacy membership in legacy creation order");
    }

    // ------------------------------------------------------------------ helpers

    private static List<ItemStack> displayItems(CreativeModeTab tab) {
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        // getDisplayItems hands back the tab's live collection; copy it so index assertions read a stable list.
        return List.copyOf(tab.getDisplayItems());
    }

    private static List<String> tabPaths(CreativeModeTab tab) {
        return displayItems(tab).stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
                .toList();
    }
}
