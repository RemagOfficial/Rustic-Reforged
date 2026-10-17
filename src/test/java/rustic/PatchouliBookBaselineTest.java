package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Phase 10 S10.4b baseline for the Patchouli almanac book.
 *
 * <p>The legacy 1.12.2 in-game book ({@code ItemBook} + {@code GuiBook} + {@code BookManager}) is replaced by a pure-data
 * Patchouli book authored as JSON under {@code data/rustic/patchouli_books/book/} and
 * {@code assets/rustic/patchouli_books/book/en_us/}. No Java glue exists for the book. Note that Patchouli 1.21 has no
 * per-book item: {@code BookRegistry} discovers the book (whose id is still {@code rustic:book}) at reload, and the
 * almanac is delivered as the single {@code patchouli:guide_book} item carrying a {@code patchouli:book="rustic:book"}
 * data component ({@code ItemModBook.forBook}). Because none of it is read at unit-test boot, this test is the only
 * offline guard: it walks every shipped file the way Patchouli would and asserts the 1:1 mapping back to
 * {@code BookManager.init()}, plus the lang-key and recipe/advancement gating.
 */
public class PatchouliBookBaselineTest {

    private static final String BOOK_DIR = "/data/rustic/patchouli_books/book/";
    private static final String LANG_BOOK_DIR = "/assets/rustic/patchouli_books/book/en_us/";
    private static final String LANG_JSON = "/assets/rustic/lang/en_us.json";

    private static final String[] DECORATION = {"pots", "barrels", "cabinets", "rope", "chain", "candles",
            "chandeliers", "lanterns", "lattice", "tables", "chairs", "gargoyles", "slate", "pillars",
            "clay_walls", "painted_wood"};
    private static final String[] AGRICULTURE = {"bees", "fertile_soil", "crop_stakes", "tomatoes", "chili_peppers",
            "wildberries", "grapes", "apple_trees", "olive_trees", "ironwood_trees", "herbs"};
    private static final String[] PRODUCTION = {"alchemy", "elixirs", "vanta_oil", "brewing",
            "alcoholic_beverages", "crushing", "drying"};
    private static final String[] CATEGORIES = {"decoration", "agriculture", "production"};

    // ------------------------------------------------------------------ book.json

    @Test
    public void bookJsonIsWellFormedAndI18n() {
        JsonObject book = json(BOOK_DIR + "book.json");
        assertTrue(book.get("i18n").getAsBoolean(), "book name/text are lang keys");
        assertEquals("item.rustic.book", book.get("name").getAsString());
        assertEquals("book.rustic.landing", book.get("landing_text").getAsString());
        assertEquals("rustic:main/root", book.get("advancements_tab").getAsString());
        // Patchouli renders book.model with an "item/" prefix applied internally, so the value must be bare.
        assertEquals("rustic:book", book.get("model").getAsString());
        // A book only lands in a creative tab (and so in JEI's item list) when creative_tab names a registered tab;
        // Patchouli's processCreativeTabs adds ItemModBook.forBook(book) only while Book.creativeTab != null.
        assertEquals("rustic:alchemy", book.get("creative_tab").getAsString(),
                "the almanac must be parented to a Rustic creative tab so it shows in-game and in JEI");
        // Patchouli 1.20+ hard-throws in the Book ctor (book skipped, item never registered) unless this is true:
        // all categories/entries live clientside under assets/, only book.json stays under data/.
        assertTrue(book.get("use_resource_pack").getAsBoolean(),
                "book.json MUST set use_resource_pack:true or Patchouli refuses to load the book");
        // The book uses Patchouli's auto-generated item (no dont_generate_book / custom_book_item).
        assertTrue(!book.has("dont_generate_book") && !book.has("custom_book_item"),
                "the almanac relies on Patchouli's automatic book item");
    }

    // ------------------------------------------------------------------ categories

    @Test
    public void allThreeCategoriesExistAndAreNamed() {
        for (String category : CATEGORIES) {
            JsonObject obj = json(LANG_BOOK_DIR + "categories/" + category + ".json");
            assertEquals("book.rustic.category." + category, obj.get("name").getAsString());
            assertEquals("book.rustic.category." + category + ".desc", obj.get("description").getAsString());
            assertTrue(obj.has("icon"), category + " needs an icon");
        }
    }

    // ------------------------------------------------------------------ entries: existence + shape

    @Test
    public void allThirtyTwoEntriesExistAndParse() {
        int count = 0;
        for (String category : CATEGORIES) {
            String[] names = namesFor(category);
            for (String name : names) {
                JsonObject entry = entry(category, name);
                assertEquals("book.rustic.entry." + name, entry.get("name").getAsString(), name + " display name key");
                assertTrue(entry.has("icon"), name + " needs an icon");
                assertEquals("rustic:" + category, entry.get("category").getAsString(), name + " category link");
                assertTrue(entry.get("readbydefault").getAsBoolean(), name + " must start unlocked (pure data)");
                JsonArray pages = entry.getAsJsonArray("pages");
                assertTrue(pages.size() >= 1, name + " needs at least one page");
                count++;
            }
        }
        assertEquals(34, count, "16 decoration + 11 agriculture + 7 production");
    }

    /** The three config-gated-but-always-shown entries carry no {@code flag} field (pure-data policy). */
    @Test
    public void entriesCarryNoConfigFlag() {
        for (String category : CATEGORIES) {
            for (String name : namesFor(category)) {
                assertTrue(!entry(category, name).has("flag"), name + " must always show, no flag gating");
            }
        }
    }

    // ------------------------------------------------------------------ entries: related links resolve

    @Test
    public void relatedIdsPointAtRealSiblingEntries() {
        Set<String> allNames = new LinkedHashSet<>();
        for (String category : CATEGORIES) {
            for (String name : namesFor(category)) {
                allNames.add(name);
            }
        }
        Pattern idPattern = Pattern.compile("^rustic:([A-Za-z0-9_]+)$");
        for (String category : CATEGORIES) {
            for (String name : namesFor(category)) {
                JsonElement relatedElement = entry(category, name).get("related");
                if (relatedElement == null) {
                    continue;
                }
                for (JsonElement element : relatedElement.getAsJsonArray()) {
                    Matcher matcher = idPattern.matcher(element.getAsString());
                    assertTrue(matcher.matches(), name + " related id malformed: " + element.getAsString());
                    assertTrue(allNames.contains(matcher.group(1)),
                            name + " relates to unknown entry: " + element.getAsString());
                }
            }
        }
    }

    // ------------------------------------------------------------------ entries: page count fidelity

    /** Spot-check the page counts that {@code BookManager.init()} hard-coded, so a lost page fails fast. */
    @Test
    public void pageCountMatchesLegacyBookManager() {
        assertEquals(2, pages("decoration", "barrels"), "barrel_item + barrel_fluid");
        assertEquals(2, pages("decoration", "rope"), "rope + rope_1");
        assertEquals(4, pages("agriculture", "bees"), "bees, apiaries, honeycomb, honey");
        assertEquals(3, pages("agriculture", "chili_peppers"), "chili_peppers, _1, _2");
        assertEquals(14, pages("agriculture", "herbs"), "overview + 13 herbs");
        assertEquals(7, pages("production", "alchemy"), "intro + 3 basic + 3 advanced");
        assertEquals(11, pages("production", "alcoholic_beverages"), "intro 3 + 8 beverages");
        assertEquals(7, pages("production", "brewing"), "brewing through brewing_6");
        assertEquals(2, pages("production", "crushing"), "crushing + crushing_1");
    }

    // ------------------------------------------------------------------ item model texture

    /**
     * Patchouli's {@code replaceBookModel} renders {@code book.model} (here {@code rustic:book} -> the
     * {@code rustic:item/book} model) for the shared guide_book whenever the {@code patchouli:book} component is
     * {@code rustic:book}. The almanac PNG actually lives at {@code assets/rustic/textures/item/book.png}, so the
     * model must reference {@code rustic:item/book}; the 1.12-era {@code rustic:items/book} resolves to a missing
     * texture and renders a broken icon.
     */
    @Test
    public void bookItemModelPointsAtTheRealTextureFolder() {
        JsonObject model = json("/assets/rustic/models/item/book.json");
        JsonObject textures = model.getAsJsonObject("textures");
        assertEquals("rustic:item/book", textures.get("0").getAsString());
        assertEquals("rustic:item/book", textures.get("particle").getAsString());
    }

    // ------------------------------------------------------------------ recipe gating

    @Test
    public void almanacRecipeIsPatchouliGatedAndCraftsTheBook() {
        JsonObject recipe = json("/data/rustic/recipe/almanac.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        // Patchouli 1.21 has no rustic:book item: the almanac is guide_book + the patchouli:book data component.
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals("patchouli:guide_book", result.get("id").getAsString());
        assertEquals("rustic:book",
                result.getAsJsonObject("components").get("patchouli:book").getAsString());
        assertEquals("c:nuggets/iron",
                recipe.getAsJsonObject("key").getAsJsonObject("I").get("tag").getAsString());
        assertPatchouliCondition(recipe);
    }

    // ------------------------------------------------------------------ advancement gating

    @Test
    public void almanacAdvancementIsPatchouliGatedAndParentedToRoot() {
        JsonObject almanac = json("/data/rustic/advancement/main/almanac.json");
        assertEquals("rustic:main/root", almanac.get("parent").getAsString());
        // Icon (and the inventory_changed items condition) must target guide_book + patchouli:book component.
        JsonObject icon = almanac.getAsJsonObject("display").getAsJsonObject("icon");
        assertEquals("patchouli:guide_book", icon.get("id").getAsString());
        assertEquals("rustic:book", icon.getAsJsonObject("components").get("patchouli:book").getAsString());
        JsonObject itemCondition = almanac.getAsJsonObject("criteria").getAsJsonObject("has_almanac")
                .getAsJsonObject("conditions").getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("patchouli:guide_book",
                itemCondition.getAsJsonArray("items").get(0).getAsString());
        assertEquals("rustic:book",
                itemCondition.getAsJsonObject("components").get("patchouli:book").getAsString());
        assertPatchouliCondition(almanac);
    }

    @Test
    public void rootAdvancementExistsAsAlmanacParent() {
        JsonObject root = json("/data/rustic/advancement/main/root.json");
        assertEquals("advancements.rustic.root",
                root.getAsJsonObject("display").getAsJsonObject("title").get("translate").getAsString());
        // The tab root is a vanilla Rustic advancement, so it must NOT be Patchouli-gated.
        assertTrue(!root.has("neoforge:conditions"), "the root tab is present whether or not Patchouli is installed");
    }

    // ------------------------------------------------------------------ lang-key coverage (drift guard)

    /**
     * Every lang key a book file references (entry names, page text, category names/descriptions, the book name and
     * landing) must exist in the ported {@code en_us.json}. This is the guard that catches a page renamed here but not
     * migrated there.
     */
    @Test
    public void everyReferencedLangKeyExists() {
        JsonObject lang = json(LANG_JSON);
        Set<String> referenced = new LinkedHashSet<>();

        JsonObject book = json(BOOK_DIR + "book.json");
        referenced.add(book.get("name").getAsString());
        referenced.add(book.get("landing_text").getAsString());

        for (String category : CATEGORIES) {
            JsonObject obj = json(LANG_BOOK_DIR + "categories/" + category + ".json");
            referenced.add(obj.get("name").getAsString());
            referenced.add(obj.get("description").getAsString());
        }

        Pattern keyPattern = Pattern.compile("book\\.rustic\\.[A-Za-z0-9_.]+");
        for (String category : CATEGORIES) {
            for (String name : namesFor(category)) {
                JsonObject entry = entry(category, name);
                referenced.add(entry.get("name").getAsString());
                for (JsonElement page : entry.getAsJsonArray("pages")) {
                    JsonObject asObject = page.getAsJsonObject();
                    if (asObject.has("text")) {
                        Matcher matcher = keyPattern.matcher(asObject.get("text").getAsString());
                        while (matcher.find()) {
                            referenced.add(matcher.group());
                        }
                    }
                }
            }
        }

        for (String key : referenced) {
            assertTrue(lang.has(key), "lang key referenced by a book file but missing from en_us.json: " + key);
        }
    }

    @Test
    public void advancementLangKeysExist() {
        JsonObject lang = json(LANG_JSON);
        for (String key : new String[] {"advancements.rustic.root", "advancements.rustic.root.desc",
                "advancements.rustic.almanac", "advancements.rustic.almanac.desc"}) {
            assertTrue(lang.has(key), "missing advancement lang key: " + key);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void assertPatchouliCondition(JsonObject obj) {
        JsonArray conditions = obj.getAsJsonArray("neoforge:conditions");
        assertNotNull(conditions, "expected a neoforge:conditions gate on " + obj);
        boolean found = false;
        for (JsonElement element : conditions) {
            JsonObject condition = element.getAsJsonObject();
            if ("neoforge:mod_loaded".equals(condition.get("type").getAsString())
                    && "patchouli".equals(condition.get("modid").getAsString())) {
                found = true;
            }
        }
        assertTrue(found, "must be gated behind neoforge:mod_loaded patchouli");
    }

    private static String[] namesFor(String category) {
        switch (category) {
            case "decoration":
                return DECORATION;
            case "agriculture":
                return AGRICULTURE;
            case "production":
                return PRODUCTION;
            default:
                throw new IllegalArgumentException("unknown category " + category);
        }
    }

    private static JsonObject entry(String category, String name) {
        return json(LANG_BOOK_DIR + "entries/" + category + "/" + name + ".json");
    }

    private static int pages(String category, String name) {
        return entry(category, name).getAsJsonArray("pages").size();
    }

    private static JsonObject json(String classpathResource) {
        try (InputStream in = PatchouliBookBaselineTest.class.getResourceAsStream(classpathResource)) {
            assertNotNull(in, "missing book resource on the classpath: " + classpathResource);
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new RuntimeException("could not read " + classpathResource, e);
        }
    }
}
