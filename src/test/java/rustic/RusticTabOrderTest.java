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

public class RusticTabOrderTest {

    private static final List<Fluid> BOTTLE_FLUIDS = List.of(
            RusticFluids.OLIVE_OIL.value(), RusticFluids.IRONBERRY_JUICE.value(),
            RusticFluids.WILDBERRY_JUICE.value(), RusticFluids.GRAPE_JUICE.value(),
            RusticFluids.APPLE_JUICE.value(), RusticFluids.ALE_WORT.value(),
            RusticFluids.HONEY.value(), RusticFluids.GOLDEN_APPLE_JUICE.value(),
            RusticFluids.VANTA_OIL.value(), RusticFluids.ALE.value(),
            RusticFluids.CIDER.value(), RusticFluids.IRON_WINE.value(),
            RusticFluids.MEAD.value(), RusticFluids.WILDBERRY_WINE.value(),
            RusticFluids.WINE.value(), RusticFluids.AMBROSIA.value());

    @Test
    public void theFarmingTabFollowsTheLegacyCreationOrder() {
        List<String> expected = new ArrayList<>(List.of(

                "beehive", "apiary", "fertile_soil",
                "log_olive", "log_ironwood", "leaves_olive", "leaves_ironwood",
                "sapling_olive", "sapling_ironwood",
                "crushing_tub", "evaporating_basin", "crop_stake", "wildberry_bush", "grape_stem",
                "brewing_barrel", "apple_seeds", "sapling_apple",
                "leaves_apple"));

        expected.addAll(List.of("bee", "honeycomb", "beeswax", "tallow", "olives", "ironberries"));
        for (int i = 0; i < BOTTLE_FLUIDS.size(); i++) {
            expected.add("fluid_bottle");
        }
        expected.addAll(List.of("dust_tiny_iron", "dust_gold",
                "tomato", "tomato_seeds", "chili_pepper", "ghost_pepper", "chili_pepper_seeds",
                "wildberries", "grapes"));

        expected.addAll(List.of("olive_oil_bucket", "ironberry_juice_bucket", "wildberry_juice_bucket",
                "grape_juice_bucket", "apple_juice_bucket", "ale_wort_bucket", "honey_bucket",
                "golden_apple_juice_bucket", "vanta_oil_bucket"));

        assertEquals(expected, tabPaths(RusticCreativeTabs.FARMING.get()),
                "the FARMING tab is not the legacy membership in legacy creation order");
    }

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

    @Test
    public void theItemLessLegacyBlocksStayOutOfFarming() {
        List<String> farming = tabPaths(RusticCreativeTabs.FARMING.get());
        for (String itemless : List.of("stake_tied", "tomato_crop", "chili_crop", "grape_leaves")) {
            assertTrue(!farming.contains(itemless),
                    "rustic:" + itemless + " had no legacy item, so it must not be offered in FARMING");
        }

        assertEquals("minecraft",
                BuiltInRegistries.ITEM.getKey(BuiltInRegistries.ITEM.get(ResourceLocation.parse("rustic:stake_tied"))).getNamespace(),
                "stake_tied must have no registered item, matching legacy's block-only registration");
    }

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

    private static List<ItemStack> displayItems(CreativeModeTab tab) {
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));

        return List.copyOf(tab.getDisplayItems());
    }

    private static List<String> tabPaths(CreativeModeTab tab) {
        return displayItems(tab).stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
                .toList();
    }
}
