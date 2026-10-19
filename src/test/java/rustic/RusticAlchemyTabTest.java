package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import rustic.content.RusticCreativeTabs;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.fluids.RusticFluids;
import rustic.content.items.ItemElixir;
import rustic.content.items.ItemFluidBottle;
import rustic.content.items.RusticItems;
import rustic.content.potions.ElixirUtils;
import rustic.content.potions.RusticMobEffects;
import rustic.registry.RusticRegistries;

/**
 * Bug-fix gate for the ALCHEMY creative tab: 1.12.2 filled it from every
 * {@code setCreativeTab(Rustic.alchemyTab)} member - the condenser/retort pair twice, the thirteen herbs
 * and the condenser-recipe elixirs plus the bottle of vanta oil - and the Item.REGISTRY walk rendered them
 * in registration order. The port's tabs are explicit hand-lists, so this is where that membership and
 * order are pinned, against the legacy oracle ({@code ModBlocks} lines 346-349, {@code Herbs.init},
 * {@code ItemFluidBottle}'s alchemy branch and {@code Recipes.addCondenserRecipes}).
 *
 * <p>Runs in ModDevGradle's FML unit-test JVM like the registration tests, so the registries are live and
 * the generated condenser recipe data is on the classpath - which the second test uses to pin
 * {@link ItemElixir#condenserElixirs()} against the actual recipe JSON, the 1.21 stand-in for legacy's
 * {@code getSubItems}-over-the-recipe-list behaviour.
 */
public class RusticAlchemyTabTest {

    /**
     * The tab's ids in legacy order: the four still blocks (condenser, retort, then the advanced pair, per
     * ModBlocks lines 346-349), the nine self-herb block items in Herbs.init creation order (wind_thistle
     * before vanta_lily), the four crop-herb edibles, the vanta oil bottle (fluid_bottle registered before
     * elixir in ModItems), then the thirty-one elixirs.
     */
    private static final List<String> EXPECTED_ORDER = List.of(
            "condenser", "retort", "condenser_advanced", "retort_advanced",
            "aloe_vera", "blood_orchid", "chamomile", "cohosh", "deathstalk_mushroom", "horsetail",
            "mooncap_mushroom", "wind_thistle", "vanta_lily",
            "cloudsbluff", "core_root", "ginseng", "marsh_mallow",
            "fluid_bottle");

    /** The golden effect triples, in Recipes.addCondenserRecipes order - the tab's entries 19..49. */
    private static final List<MobEffectInstance> EXPECTED_ELIXIRS = List.of(
            new MobEffectInstance(MobEffects.HEAL, 1, 0),
            new MobEffectInstance(MobEffects.HEAL, 1, 1),
            new MobEffectInstance(MobEffects.REGENERATION, 900, 0),
            new MobEffectInstance(MobEffects.REGENERATION, 1800, 0),
            new MobEffectInstance(MobEffects.REGENERATION, 450, 1),
            new MobEffectInstance(MobEffects.WITHER, 900, 0),
            new MobEffectInstance(MobEffects.WITHER, 1800, 0),
            new MobEffectInstance(MobEffects.WITHER, 450, 1),
            new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0),
            new MobEffectInstance(MobEffects.NIGHT_VISION, 9600, 0),
            new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0),
            new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 9600, 0),
            new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 1),
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0),
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 9600, 0),
            new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 0),
            new MobEffectInstance(MobEffects.HEALTH_BOOST, 9600, 0),
            new MobEffectInstance(MobEffects.HEALTH_BOOST, 1800, 1),
            new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0),
            new MobEffectInstance(MobEffects.DIG_SPEED, 9600, 0),
            new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 1),
            new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0),
            new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9600, 0),
            new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800, 1),
            new MobEffectInstance(RusticMobEffects.IRON_SKIN, 3600, 0),
            new MobEffectInstance(RusticMobEffects.IRON_SKIN, 9600, 0),
            new MobEffectInstance(RusticMobEffects.IRON_SKIN, 1800, 1),
            new MobEffectInstance(RusticMobEffects.FEATHER, 3600, 0),
            new MobEffectInstance(RusticMobEffects.FEATHER, 9600, 0),
            new MobEffectInstance(RusticMobEffects.BLAZING_TRAIL, 3600, 0),
            new MobEffectInstance(RusticMobEffects.BLAZING_TRAIL, 9600, 0));

    /** The condenser recipe ids in {@code RusticRecipeProvider.addCondenser} emission order. */
    private static final List<String> RECIPE_IDS = List.of(
            "heal_basic", "heal_advanced",
            "regeneration_basic", "regeneration_extended", "regeneration_strong",
            "wither_basic", "wither_extended", "wither_strong",
            "night_vision_basic", "night_vision_extended",
            "speed_basic", "speed_extended", "speed_strong",
            "fire_resistance_basic", "fire_resistance_extended",
            "health_boost_basic", "health_boost_extended", "health_boost_strong",
            "haste_basic", "haste_extended", "haste_strong",
            "strength_basic", "strength_extended", "strength_strong",
            "iron_skin_basic", "iron_skin_extended", "iron_skin_strong",
            "feather_basic", "feather_extended",
            "blazing_trail_basic", "blazing_trail_extended");

    // ------------------------------------------------------------------ membership and order

    /** The ALCHEMY tab is exactly the legacy membership, in the legacy Item.REGISTRY order. */
    @Test
    public void theAlchemyTabHoldsTheLegacyMembershipInLegacyOrder() {
        List<ItemStack> shown = alchemyDisplayItems();

        List<String> paths = shown.stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
                .toList();
        List<String> expected = new ArrayList<>(EXPECTED_ORDER);
        for (int i = 0; i < EXPECTED_ELIXIRS.size(); i++) {
            expected.add("elixir");
        }
        assertEquals(expected, paths, "the ALCHEMY tab is not the legacy membership in legacy order");

        // Entry 18 is not just "a fluid bottle": the alchemy branch listed exactly the vanta oil bottle.
        ItemStack bottle = shown.get(EXPECTED_ORDER.size() - 1);
        assertSame(RusticItems.FLUID_BOTTLE.get(), bottle.getItem(), "the bottle entry is a fluid bottle");
        assertSame(RusticFluids.VANTA_OIL.value(), ItemFluidBottle.getFluidContent(bottle).getFluid(),
                "the alchemy tab's bottle is vanta oil, the legacy branch's only entry");

        // Entries 19.. are the elixirs, each carrying its one golden effect.
        for (int i = 0; i < EXPECTED_ELIXIRS.size(); i++) {
            ItemStack stack = shown.get(EXPECTED_ORDER.size() + i);
            assertSame(RusticItems.ELIXIR.get(), stack.getItem(), "tab entry " + i + " is an elixir");
            assertSingleEffect(stack, EXPECTED_ELIXIRS.get(i), "tab elixir " + i);
        }
    }

    /** The first elixir entry is the tab icon's default instance, exactly as legacy's walk began. */
    @Test
    public void theElixirListOpensWithTheDefaultInstance() {
        List<ItemStack> shown = alchemyDisplayItems();
        ItemStack first = shown.get(EXPECTED_ORDER.size());
        assertSingleEffect(ItemElixir.defaultElixir(), EXPECTED_ELIXIRS.get(0), "the default elixir");
        assertSingleEffect(first, EXPECTED_ELIXIRS.get(0), "the tab's first elixir");
    }

    // ------------------------------------------------------------------ recipe-data sync

    /**
     * The single-source guard: {@link ItemElixir#condenserElixirs()} mirrors the generated
     * {@code rustic:condenser} recipe data, so the tab can never drift from the recipes the condensers
     * actually craft. Each generated JSON is decoded through the registered serializer - the same path
     * {@code RecipeManager.fromJson} takes - and its result stack compared to the catalogue entry with the
     * same index. Re-run {@code runData} if the data side moved; edit both sides together, never one.
     */
    @Test
    public void theElixirCatalogueMatchesTheGeneratedCondenserRecipes() {
        List<ItemStack> catalogue = ItemElixir.condenserElixirs();
        assertEquals(31, catalogue.size(), "legacy listed 31 distinct condenser results");
        assertEquals(RECIPE_IDS.size(), catalogue.size(), "one catalogue entry per generated recipe row");

        for (int i = 0; i < RECIPE_IDS.size(); i++) {
            CondenserRecipe recipe = decodeGeneratedCondenser(RECIPE_IDS.get(i));
            ItemStack result = recipe.getResultItem(null);
            assertTrue(result.is(RusticItems.ELIXIR.get()), RECIPE_IDS.get(i) + " does not craft an elixir");
            assertSingleEffect(result, EXPECTED_ELIXIRS.get(i), RECIPE_IDS.get(i));
            assertSingleEffect(catalogue.get(i), EXPECTED_ELIXIRS.get(i), "catalogue " + RECIPE_IDS.get(i));
        }
    }

    // ------------------------------------------------------------------ the moves out of the other tabs

    /**
     * The herbs left FARMING for ALCHEMY (legacy {@code BlockHerbBase} / {@code ItemHerbEdible} were
     * alchemyTab members), and the still blocks left DECOR's old blanket machine append - while every one
     * of them stays registered, so existing worlds are untouched.
     */
    @Test
    public void herbsAndStillBlocksLeftTheirWrongTabsButStayRegistered() {
        List<String> farming = tabPaths(RusticCreativeTabs.FARMING.get());
        for (var herb : RusticFarmBlocks.herbs()) {
            assertFalse(farming.contains(herb.getId().getPath()),
                    herb.getId() + " is an alchemyTab block and leaked into FARMING");
        }
        List<String> decor = tabPaths(RusticCreativeTabs.DECOR.get());
        for (String still : List.of("condenser", "retort", "condenser_advanced", "retort_advanced")) {
            assertFalse(decor.contains(still), "rustic:" + still + " is an alchemyTab block and leaked into DECOR");
        }

        // Still registered (a tab move is not a registry move).
        assertNotNull(RusticMachineBlocks.RETORT.get().asItem(), "the retort lost its block item");
        assertNotNull(RusticMachineBlocks.RETORT_ADVANCED.get().asItem(), "the advanced retort lost its block item");
        RusticFarmBlocks.herbs().forEach(herb -> assertNotNull(BuiltInRegistries.BLOCK.get(herb.getId()),
                herb.getId() + " left the register, not just the tab"));
    }

    /** The machines legacy hung on FARMING arrived there when they left the DECOR append. */
    @Test
    public void theProcessMachinesAreOfferedInTheFarmingTab() {
        List<String> farming = tabPaths(RusticCreativeTabs.FARMING.get());
        for (String machine : List.of("crushing_tub", "evaporating_basin", "brewing_barrel", "apiary")) {
            assertTrue(farming.contains(machine), "rustic:" + machine + " is a farmingTab machine: " + farming);
        }
        List<String> decor = tabPaths(RusticCreativeTabs.DECOR.get());
        for (String storage : List.of("barrel", "liquid_barrel", "cabinet", "vase")) {
            assertTrue(decor.contains(storage), "rustic:" + storage + " stayed decorTab (BlockBase default): " + decor);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static List<ItemStack> alchemyDisplayItems() {
        CreativeModeTab alchemy = RusticCreativeTabs.ALCHEMY.get();
        alchemy.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        // getDisplayItems hands back the tab's live collection; copy it so index assertions read a stable list.
        return List.copyOf(alchemy.getDisplayItems());
    }

    private static List<String> tabPaths(CreativeModeTab tab) {
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        return tab.getDisplayItems().stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
                .toList();
    }

    /** A single stored effect matching {@code expected} on identity, duration and amplifier. */
    private static void assertSingleEffect(ItemStack stack, MobEffectInstance expected, String label) {
        List<MobEffectInstance> effects = ElixirUtils.getEffects(stack);
        assertEquals(1, effects.size(), label + " must carry exactly one effect");
        MobEffectInstance actual = effects.get(0);
        MobEffect effect = expected.getEffect().value();
        assertSame(effect, actual.getEffect().value(), label + " effect identity");
        assertEquals(expected.getDuration(), actual.getDuration(), label + " duration");
        assertEquals(expected.getAmplifier(), actual.getAmplifier(), label + " amplifier");
    }

    /** Read a generated condenser recipe off the classpath and decode it through the registered serializer. */
    private static CondenserRecipe decodeGeneratedCondenser(String id) {
        String path = "/data/rustic/recipe/condenser/" + id + ".json";
        JsonObject obj;
        try (InputStream in = RusticAlchemyTabTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, "missing generated recipe on the classpath: " + path
                    + " - re-run runData rather than writing the file by hand");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                obj = JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new AssertionError("could not read " + path, e);
        }
        obj.remove("type");
        MapLike<JsonElement> map = JsonOps.INSTANCE.getMap(obj)
                .getOrThrow(error -> new AssertionError("not a JSON object: " + path + " - " + error));
        return RusticRegistries.CONDENSER_SERIALIZER.get().codec().decode(JsonOps.INSTANCE, map)
                .getOrThrow(error -> new AssertionError("failed to decode generated recipe " + path + ": " + error));
    }
}
