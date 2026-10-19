package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class RusticRecipeDatagenTest {

    private static final String DIR = "/data/rustic/recipe/";

    @Test
    public void stonePillarCraftsSixFromStone() {
        JsonObject recipe = recipe("pillar_stone");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(6, resultCount(recipe));
        assertEquals("rustic:stone_pillar", resultId(recipe));
        assertEquals("minecraft:stone", key(recipe, "S").get("item").getAsString());
    }

    @Test
    public void slateChainKeepsLegacyResultIdsAndCounts() {
        assertEquals("rustic:slate_brick", resultId(recipe("slate_brick")));
        assertEquals(4, resultCount(recipe("slate_brick")));

        assertEquals("rustic:slate_tile", resultId(recipe("slate_tile")));
        assertEquals("rustic:slate_tile", resultId(recipe("slate_tile_1")));
        assertEquals("rustic:slate_chiseled", resultId(recipe("slate_chiseled")));

        assertEquals("rustic:stairs_slate_roof", resultId(recipe("slate_roof_stairs")));
        assertEquals("rustic:slate_roof_slab", resultId(recipe("slate_roof_slab")));
    }

    @Test
    public void clayWallUsesThePlanksTagAndEightResult() {
        JsonObject recipe = recipe("clay_wall");
        assertEquals(8, resultCount(recipe));
        assertEquals("rustic:clay_wall", resultId(recipe));
        assertEquals("minecraft:planks", key(recipe, "P").get("tag").getAsString());
        assertEquals("minecraft:clay", key(recipe, "C").get("item").getAsString());
    }

    @Test
    public void furnitureUsesPerWoodPlanksAndLegacyRecipeIds() {

        JsonObject oakChair = recipe("oak_chair");
        assertEquals(4, resultCount(oakChair));
        assertEquals("rustic:chair_oak", resultId(oakChair));
        assertEquals("minecraft:oak_planks", key(oakChair, "P").get("item").getAsString());

        assertEquals("minecraft:dark_oak_planks", key(recipe("big_oak_chair"), "P").get("item").getAsString());

        JsonObject oliveTable = recipe("olive_table");
        assertEquals(2, resultCount(oliveTable));
        assertEquals("rustic:table_olive", resultId(oliveTable));
        assertEquals("rustic:planks_olive", key(oliveTable, "P").get("item").getAsString());
    }

    @Test
    public void ironLatticeCraftsSixteenFromIron() {
        JsonObject recipe = recipe("iron_lattice");
        assertEquals(16, resultCount(recipe));
        assertEquals("rustic:iron_lattice", resultId(recipe));

        assertEquals("c:ingots/iron", key(recipe, "I").get("tag").getAsString());
    }

    @Test
    public void cabinetIsOneShapedRecipeWithTheTrapdoorOnTheRight() {
        JsonObject recipe = recipe("cabinet");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("rustic:cabinet", resultId(recipe));
        assertEquals(1, resultCount(recipe));
        JsonArray pattern = recipe.getAsJsonArray("pattern");
        assertEquals(3, pattern.size());
        assertEquals("PPP", pattern.get(0).getAsString());
        assertEquals("P T", pattern.get(1).getAsString());
        assertEquals("PPP", pattern.get(2).getAsString());
        assertEquals("minecraft:planks", key(recipe, "P").get("tag").getAsString());
        assertEquals("minecraft:wooden_trapdoors", key(recipe, "T").get("tag").getAsString());
        assertFalse(recipe.has("neoforge:conditions"), "the cabinet must always be craftable");
    }

    @Test
    public void paintedWoodKeepsLegacySilverIdAndDyeTag() {

        JsonObject recipe = recipe("painted_wood_silver");
        assertEquals(8, resultCount(recipe));
        assertEquals("rustic:painted_wood_silver", resultId(recipe));
        assertEquals("c:dyes/light_gray", key(recipe, "D").get("tag").getAsString());
        assertEquals("minecraft:planks", key(recipe, "P").get("tag").getAsString());

        assertEquals("rustic:painted_wood_blue", resultId(recipe("painted_wood_blue")));
    }

    @Test
    public void silverRecipesCarryTheTagEmptySilverCondition() {
        for (String id : new String[] {"candle_silver", "candle_double_silver", "candle_lever_silver",
                "chain_silver", "chandelier_silver", "silver_lantern"}) {
            JsonObject recipe = recipe(id);
            JsonArray conditions = recipe.getAsJsonArray("neoforge:conditions");
            assertNotNull(conditions, id + " must be condition-gated");
            assertEquals(1, conditions.size());
            JsonObject not = conditions.get(0).getAsJsonObject();
            assertEquals("neoforge:not", not.get("type").getAsString());
            JsonObject tagEmpty = not.getAsJsonObject("value");
            assertEquals("neoforge:tag_empty", tagEmpty.get("type").getAsString());
            assertEquals("c:ingots/silver", tagEmpty.get("tag").getAsString());
        }
    }

    @Test
    public void silverLanternTreatsCoalAndCharcoalAsOneSlot() {
        JsonElement coal = keyElement(recipe("silver_lantern"), "C");
        assertTrue(coal.isJsonArray(), "the coal input is a coal-or-charcoal choice");
        String[] items = {coal.getAsJsonArray().get(0).getAsJsonObject().get("item").getAsString(),
                coal.getAsJsonArray().get(1).getAsJsonObject().get("item").getAsString()};
        assertEquals("minecraft:coal", items[0]);
        assertEquals("minecraft:charcoal", items[1]);
    }

    @Test
    public void ironAndGoldMountsCraftFromTheConventionalMetalTags() {
        assertMount("", "rustic:chain", "rustic:candle", "rustic:candle_double", "rustic:candle_lever",
                "rustic:chandelier", "rustic:iron_lantern", "iron_lantern", "c:ingots/iron");
        assertMount("_gold", "rustic:chain_gold", "rustic:candle_gold", "rustic:candle_double_gold",
                "rustic:candle_lever_gold", "rustic:chandelier_gold", "rustic:golden_lantern",
                "golden_lantern", "c:ingots/gold");

        JsonObject ironTorch = recipe("iron_torch");
        assertEquals("minecraft:crafting_shaped", ironTorch.get("type").getAsString());
        assertEquals("rustic:iron_torch", resultId(ironTorch));
        assertEquals(4, resultCount(ironTorch));
        assertEquals("c:nuggets/iron", key(ironTorch, "I").get("tag").getAsString());
        assertEquals("minecraft:stick", key(ironTorch, "S").get("item").getAsString());
    }

    private void assertMount(String idSuffix, String chain, String candle, String candleDouble,
                             String candleLever, String chandelier, String lantern, String lanternId, String ingots) {
        JsonObject chainRecipe = recipe("chain" + idSuffix);
        assertEquals("minecraft:crafting_shaped", chainRecipe.get("type").getAsString());
        assertEquals(chain, resultId(chainRecipe));
        assertEquals(12, resultCount(chainRecipe));
        assertEquals(ingots, key(chainRecipe, "I").get("tag").getAsString(), chain + " ingot slot");
        assertFalse(chainRecipe.has("neoforge:conditions"), chain + " must always be craftable");

        assertEquals(candle, resultId(recipe("candle" + idSuffix)));
        assertEquals(4, resultCount(recipe("candle" + idSuffix)));
        assertEquals("c:wax", key(recipe("candle" + idSuffix), "W").get("tag").getAsString());
        assertEquals("minecraft:string", key(recipe("candle" + idSuffix), "S").get("item").getAsString());
        assertEquals(candleDouble, resultId(recipe("candle_double" + idSuffix)));
        assertEquals(candleLever, resultId(recipe("candle_lever" + idSuffix)));
        assertEquals(candle, key(recipe("candle_lever" + idSuffix), "C").get("item").getAsString(),
                "the lever is the plain candle with a redstone switch");
        assertEquals("c:dusts/redstone", key(recipe("candle_lever" + idSuffix), "R").get("tag").getAsString());
        assertEquals(chandelier, resultId(recipe("chandelier" + idSuffix)));
        assertEquals(2, resultCount(recipe("chandelier" + idSuffix)));
        assertEquals(chain, key(recipe("chandelier" + idSuffix), "C").get("item").getAsString());

        JsonObject lanternRecipe = recipe(lanternId);
        assertEquals(lantern, resultId(lanternRecipe));
        assertEquals(4, resultCount(lanternRecipe));
        assertEquals(ingots, key(lanternRecipe, "I").get("tag").getAsString());

        JsonElement coal = keyElement(lanternRecipe, "C");
        assertTrue(coal.isJsonArray(), lantern + " coal slot must offer both fuels");
        assertEquals("minecraft:coal", coal.getAsJsonArray().get(0).getAsJsonObject().get("item").getAsString());
        assertEquals("minecraft:charcoal", coal.getAsJsonArray().get(1).getAsJsonObject().get("item").getAsString());
    }

    @Test
    public void workstationsCraftFromPlanksWoodenSlabsAndIron() {
        assertEquals(2, resultCount(recipe("barrel")));
        assertEquals("rustic:barrel", resultId(recipe("barrel")));
        assertEquals(2, resultCount(recipe("liquid_barrel")));
        assertEquals("rustic:liquid_barrel", resultId(recipe("liquid_barrel")));
        assertEquals("rustic:brewing_barrel", resultId(recipe("brewing_barrel")));
        assertEquals("rustic:crushing_tub", resultId(recipe("crushing_tub")));
        for (String id : new String[] {"barrel", "liquid_barrel", "brewing_barrel", "crushing_tub"}) {
            JsonObject recipe = recipe(id);
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), id);
            assertEquals("minecraft:planks", key(recipe, "P").get("tag").getAsString(), id);
            assertEquals("minecraft:wooden_slabs", key(recipe, "S").get("tag").getAsString(), id);
            assertEquals("c:ingots/iron", key(recipe, "I").get("tag").getAsString(), id);
            assertFalse(recipe.has("neoforge:conditions"), id + " must always be craftable");
        }

        JsonObject basin = recipe("evaporating_basin");
        assertEquals("minecraft:terracotta", key(basin, "#").get("item").getAsString());
        assertEquals("rustic:evaporating_basin", resultId(basin));

        JsonObject condenser = recipe("condenser");
        assertEquals("minecraft:white_terracotta", key(condenser, "C").get("item").getAsString(),
                "stained_hardened_clay meta 0 is white terracotta");
        assertEquals("minecraft:bucket", key(condenser, "E").get("item").getAsString());
        assertEquals("rustic:condenser", resultId(condenser));
        JsonObject advanced = recipe("condenser_advanced");
        assertEquals("minecraft:iron_block", key(advanced, "I").get("item").getAsString());
        assertEquals("rustic:condenser_advanced", resultId(advanced));
    }

    @Test
    public void miscDecorCraftsFromTheLegacyPatterns() {
        assertEquals(12, resultCount(recipe("rope")));
        assertEquals("minecraft:string", key(recipe("rope"), "S").get("item").getAsString());
        assertEquals(3, resultCount(recipe("crop_stake")));
        assertEquals("rustic:crop_stake", resultId(recipe("crop_stake")));
        assertEquals(6, resultCount(recipe("vase")));
        assertEquals("rustic:vase", resultId(recipe("vase")));
        assertEquals("rustic:lantern_wood", resultId(recipe("lantern_wood")));
        assertEquals(2, resultCount(recipe("gargoyle")));
        assertEquals("rustic:gargoyle", resultId(recipe("gargoyle")));
        assertEquals("minecraft:stone_slab", key(recipe("gargoyle"), "S").get("item").getAsString(),
                "the legacy stone_slab meta 0 was the smooth-stone slab, which is what that id means now");

        JsonArray fertile = recipe("fertile_soil").getAsJsonArray("ingredients");
        assertEquals("minecraft:bone_meal", fertile.get(0).getAsJsonObject().get("item").getAsString(),
                "dye meta 15 (bone meal) is its own item now");
        assertEquals("minecraft:dirt", fertile.get(1).getAsJsonObject().get("item").getAsString());
        assertEquals("rustic:fertile_soil", resultId(recipe("fertile_soil")));

        assertEquals("rustic:grape_stem", resultId(recipe("grape_stem")));
    }

    @Test
    public void rusticWoodPartsCraftInVanillasOwnPatterns() {
        for (String wood : new String[] {"olive", "ironwood"}) {
            JsonObject planks = recipe(wood + "_planks");
            assertEquals("minecraft:crafting_shapeless", planks.get("type").getAsString());
            assertEquals(4, resultCount(planks));
            assertEquals("rustic:planks_" + wood, resultId(planks));
            assertEquals("rustic:log_" + wood,
                    planks.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString());

            JsonObject slab = recipe(wood + "_slab");
            assertEquals(6, resultCount(slab));
            assertEquals("rustic:" + wood + "_slab", resultId(slab));
            assertEquals("PPP", slab.getAsJsonArray("pattern").get(0).getAsString());
            assertEquals("rustic:planks_" + wood, key(slab, "P").get("item").getAsString());

            JsonObject stairs = recipe(wood + "_stairs");
            assertEquals(4, resultCount(stairs));
            assertEquals("rustic:stairs_" + wood, resultId(stairs));

            assertEquals(3, resultCount(recipe(wood + "_fence")));
            assertEquals("rustic:fence_" + wood, resultId(recipe(wood + "_fence")));
            assertEquals("rustic:fence_gate_" + wood, resultId(recipe(wood + "_fence_gate")));
        }
    }

    @Test
    public void smeltingRowsMapLegacyInputsToOutputs() {
        assertSmelt("beeswax_from_smelting", "rustic:honeycomb", "rustic:beeswax", 0.3F);
        assertSmelt("charcoal_from_olive_log", "rustic:log_olive", "minecraft:charcoal", 0.15F);
        assertSmelt("charcoal_from_ironwood_log", "rustic:log_ironwood", "minecraft:charcoal", 0.15F);
        assertSmelt("iron_nugget_from_smelting", "rustic:dust_tiny_iron", "minecraft:iron_nugget", 0.15F);
        assertSmelt("gold_ingot_from_smelting", "rustic:dust_gold", "minecraft:gold_ingot", 0.5F);
        assertSmelt("tallow_from_smelting", "minecraft:rotten_flesh", "rustic:tallow", 0.3F);
    }

    private void assertSmelt(String id, String input, String output, float experience) {
        JsonObject recipe = recipe(id);
        assertEquals("minecraft:smelting", recipe.get("type").getAsString(), id);
        assertEquals(input, recipe.getAsJsonObject("ingredient").get("item").getAsString(), id);
        assertEquals(output, resultId(recipe), id);
        assertEquals(200, recipe.get("cookingtime").getAsInt(), id);
        assertEquals(experience, recipe.get("experience").getAsFloat(), 0.0F, id);
    }

    @Test
    public void furnaceFuelDataMapCarriesTheLegacyBurnTimes() {
        JsonObject values = json("/data/neoforge/data_maps/item/furnace_fuels.json").getAsJsonObject("values");
        assertEquals(100, values.getAsJsonObject("rustic:sapling_olive").get("burn_time").getAsInt());
        assertEquals(100, values.getAsJsonObject("rustic:sapling_ironwood").get("burn_time").getAsInt());
        assertEquals(200, values.getAsJsonObject("rustic:wildberry_bush").get("burn_time").getAsInt());
    }

    private static JsonObject recipe(String fileName) {
        return json(DIR + fileName + ".json");
    }

    private static String resultId(JsonObject recipe) {
        return recipe.getAsJsonObject("result").get("id").getAsString();
    }

    private static int resultCount(JsonObject recipe) {
        return recipe.getAsJsonObject("result").get("count").getAsInt();
    }

    private static JsonObject key(JsonObject recipe, String symbol) {
        return keyElement(recipe, symbol).getAsJsonObject();
    }

    private static JsonElement keyElement(JsonObject recipe, String symbol) {
        return recipe.getAsJsonObject("key").get(symbol);
    }

    private static JsonObject json(String classpathResource) {
        try (InputStream in = RusticRecipeDatagenTest.class.getResourceAsStream(classpathResource)) {
            assertNotNull(in, "missing generated recipe on the classpath: " + classpathResource
                    + " - re-run runData rather than writing the file by hand");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new RuntimeException("could not read " + classpathResource, e);
        }
    }
}
