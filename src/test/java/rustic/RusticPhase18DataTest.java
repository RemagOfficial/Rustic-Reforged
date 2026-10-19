package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class RusticPhase18DataTest {

    private static final Set<String> KNOWN_TRIGGERS = Set.of(
            "minecraft:consume_item", "minecraft:inventory_changed",
            "minecraft:effects_changed", "minecraft:recipe_unlocked");

    private static final String ADV_DIR = "/data/rustic/advancement/main/";
    private static final List<String> MAIN_ADVANCEMENTS =
            List.of("root", "almanac", "the_good_stuff", "max_drunk", "winemaxxing");

    @Test
    public void theRelocatedAdvancementsAreValidAndReferenceKnownTriggers() {
        Set<String> ids = new HashSet<>(MAIN_ADVANCEMENTS);
        for (String name : MAIN_ADVANCEMENTS) {
            JsonObject adv = json(ADV_DIR + name + ".json");
            JsonObject criteria = adv.getAsJsonObject("criteria");
            assertNotNull(criteria, name + " has no criteria object");
            assertTrue(criteria.size() > 0, name + " has an empty criteria set");
            criteria.entrySet().forEach(entry -> {
                String trigger = entry.getValue().getAsJsonObject().get("trigger").getAsString();
                assertTrue(KNOWN_TRIGGERS.contains(trigger),
                        name + "/" + entry.getKey() + " uses an unknown trigger: " + trigger);
            });
            if (adv.has("parent")) {
                String parent = adv.get("parent").getAsString();
                assertTrue(parent.startsWith("rustic:"), name + " parent must be a rustic id: " + parent);
                String parentPath = ADV_DIR + parent.substring("rustic:main.".length()) + ".json";
                assertTrue(ids.contains(parent.substring("rustic:main.".length())),
                        name + " references a parent that is not one of the shipped main advancements: " + parent
                                + " (expected at " + parentPath + ")");
            }
        }
    }

    @Test
    public void everyAdvancementTranslateKeyExistsInEnglishLang() {
        JsonObject en = json("/assets/rustic/lang/en_us.json");
        for (String name : MAIN_ADVANCEMENTS) {
            JsonObject adv = json(ADV_DIR + name + ".json");
            if (!adv.has("display")) {
                continue;
            }
            JsonObject display = adv.getAsJsonObject("display");
            for (String field : new String[] {"title", "description"}) {
                if (display.has(field) && display.getAsJsonObject(field).has("translate")) {
                    String key = display.getAsJsonObject(field).get("translate").getAsString();
                    assertTrue(en.has(key),
                            name + "." + field + " references missing lang key: " + key);
                }
            }
        }
    }

    @Test
    public void theAlcoholAdvancementsUseThePredicatesMapNotTheLegacyTypeKey() {
        for (String name : List.of("the_good_stuff", "winemaxxing")) {
            JsonObject adv = json(ADV_DIR + name + ".json");
            String raw = text(ADV_DIR + name + ".json");
            assertFalse(raw.contains("\"type\": \"rustic:alcohol\"") || raw.contains("\"type\":\"rustic:alcohol\""),
                    name + " still carries the 1.12 top-level rustic:alcohol type form");

            JsonElement first = adv.getAsJsonObject("criteria").entrySet().iterator().next().getValue();
            JsonObject item = first.getAsJsonObject().getAsJsonObject("conditions")
                    .getAsJsonArray("items").get(0).getAsJsonObject();
            JsonObject predicates = item.getAsJsonObject("predicates");
            assertNotNull(predicates, name + " items[0] has no predicates map");
            assertTrue(predicates.has("rustic:alcohol"), name + " predicates must key rustic:alcohol");
            JsonObject alcohol = predicates.getAsJsonObject("rustic:alcohol");
            assertTrue(alcohol.getAsJsonObject("quality").has("min"), name + " alcohol quality needs a min bound");
            if (name.equals("winemaxxing")) {
                assertEquals("rustic:wine", alcohol.get("fluid").getAsString(),
                        "winemaxxing must narrow the alcohol predicate to the wine fluid id");
            }
        }
    }

    @Test
    public void theFluidBottleIconsCarryFluidContentAndQualityComponents() {
        assertBottleIcon("max_drunk", "rustic:ale");
        assertBottleIcon("winemaxxing", "rustic:wine");
    }

    private static void assertBottleIcon(String name, String expectedFluid) {
        JsonObject icon = json(ADV_DIR + name + ".json").getAsJsonObject("display").getAsJsonObject("icon");
        assertEquals("rustic:fluid_bottle", icon.get("id").getAsString(), name + " icon item");
        JsonObject content = icon.getAsJsonObject("components").getAsJsonObject("rustic:fluid_content");
        assertEquals(expectedFluid, content.get("id").getAsString(), name + " icon fluid id");
        assertEquals(1000, content.get("amount").getAsInt(), name + " icon amount (full bottle)");
        assertEquals(1.0D, content.getAsJsonObject("components").get("rustic:quality").getAsDouble(),
                name + " icon must display a max-quality bottle");
    }

    @Test
    public void theMaxDrunkAdvancementChecksTheTipsyEffectAmplifier() {
        JsonObject adv = json(ADV_DIR + "max_drunk.json");
        assertEquals("goal", adv.getAsJsonObject("display").get("frame").getAsString());
        JsonObject tipsy = adv.getAsJsonObject("criteria").getAsJsonObject("has_max_inebriation")
                .getAsJsonObject("conditions").getAsJsonObject("effects").getAsJsonObject("rustic:tipsy");
        assertEquals(3, tipsy.get("amplifier").getAsInt(), "max drunk is the amplifier-3 tipsy cap");
        assertFalse(tipsy.get("ambient").getAsBoolean(), "the tipsy roll is not ambient");
    }

    @Test
    public void theRootAdvancementUsesTheSingularBlockTexturePath() {
        String background = json(ADV_DIR + "root.json").getAsJsonObject("display").get("background").getAsString();
        assertEquals("rustic:textures/block/planks_ironwood.png", background);
    }

    @Test
    public void theLegacyAssetAdvancementFilesAreRetainedAsTheSpec() {
        assertResourcePresent("/assets/rustic/advancements/main/the_good_stuff.json");
        assertResourcePresent("/assets/rustic/advancements/recipes/almanac.json");
    }

    private static final List<String> TRANSLATED_LOCALES =
            List.of("de_de", "es_es", "fr_fr", "pt_br", "ru_ru", "zh_cn", "zh_tw");

    @Test
    public void everyTranslatedLocaleHasAJsonFile() {
        for (String locale : TRANSLATED_LOCALES) {
            json("/assets/rustic/lang/" + locale + ".json");
        }
    }

    @Test
    public void eachLocaleJsonIsTheExactRemappedImageOfItsLangAndMatchesPlaceholders() {
        JsonObject en = json("/assets/rustic/lang/en_us.json");
        for (String locale : TRANSLATED_LOCALES) {
            Map<String, String> fromLang = remapLang(readLang("/assets/rustic/lang/" + localeLangName(locale) + ".lang", locale));
            JsonObject json = json("/assets/rustic/lang/" + locale + ".json");

            Set<String> langKeys = new HashSet<>(fromLang.keySet());
            Set<String> jsonKeys = new HashSet<>(json.keySet());
            assertEquals(langKeys, jsonKeys,
                    locale + ": the .json key set must equal the remapped .lang key set (nothing lost/invented)");

            List<String> mismatches = new ArrayList<>();
            for (Map.Entry<String, String> entry : fromLang.entrySet()) {
                String key = entry.getKey();
                if (!en.has(key)) {
                    continue;

                }
                List<String> enPlaceholders = placeholders(en.get(key).getAsString());
                List<String> localePlaceholders = placeholders(json.get(key).getAsString());
                if (!enPlaceholders.equals(localePlaceholders)) {
                    mismatches.add(key + ": en_us " + enPlaceholders + " vs " + locale + " " + localePlaceholders);
                }
            }
            assertTrue(mismatches.isEmpty(),
                    locale + ": placeholder mismatch against the en_us baseline:\n  " + String.join("\n  ", mismatches));
        }
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?[a-zA-Z]");

    static String remapKey(String key) {
        if (key.equals("item.rustic.fluid_bottle.name")) {

            return key;
        }
        String nameSuffix = ".name";
        if (key.matches("tile\\.rustic\\..+\\.name")) {
            return "block.rustic." + key.substring("tile.rustic.".length(), key.length() - nameSuffix.length());
        }
        if (key.matches("item\\.tile\\.rustic\\..+\\.name")) {
            return "item.rustic." + key.substring("item.tile.rustic.".length(), key.length() - nameSuffix.length());
        }
        if (key.matches("item\\.rustic\\..+\\.name")) {
            return "item.rustic." + key.substring("item.rustic.".length(), key.length() - nameSuffix.length());
        }
        if (key.matches("entity\\.rustic\\..+\\.name")) {
            return "entity.rustic." + key.substring("entity.rustic.".length(), key.length() - nameSuffix.length());
        }
        if (key.startsWith("fluid.") && !key.startsWith("fluid_type.")) {
            return "fluid_type.rustic." + key.substring("fluid.".length());
        }
        if (key.startsWith("effect.")) {
            return "effect.rustic." + key.substring("effect.".length());
        }
        return key;
    }

    private static List<String[]> readLang(String resource, String locale) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceStream(resource), StandardCharsets.UTF_8))) {
            List<String[]> pairs = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq < 1) {
                    continue;
                }
                pairs.add(new String[] {t.substring(0, eq).trim(), t.substring(eq + 1)});
            }
            return pairs;
        } catch (IOException e) {
            return fail("could not read lang " + resource + " for locale " + locale, e);
        }
    }

    private static Map<String, String> remapLang(List<String[]> langPairs) {
        Map<String, String> out = new TreeMap<>();
        for (String[] pair : langPairs) {
            out.put(remapKey(pair[0]), pair[1]);
        }
        return out;
    }

    private static List<String> placeholders(String value) {

        String cleaned = value.replace("%%", "");
        List<String> found = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(cleaned);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        found.sort(null);
        return found;
    }

    private static String toLangResourceBase(String locale) {

        return locale.equals("fr_fr") ? "fr_FR" : locale;
    }

    private static String localeLangName(String locale) {
        return toLangResourceBase(locale);
    }

    private static JsonObject json(String classpathResource) {
        try (InputStream in = resourceStream(classpathResource)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            return fail("could not read " + classpathResource, e);
        }
    }

    private static String text(String classpathResource) {
        try (InputStream in = resourceStream(classpathResource)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return fail("could not read " + classpathResource, e);
        }
    }

    private static InputStream resourceStream(String resource) {
        InputStream in = RusticPhase18DataTest.class.getResourceAsStream(resource);
        assertNotNull(in, "missing resource on the classpath: " + resource);
        return in;
    }

    private static void assertResourcePresent(String resource) {
        assertNotNull(RusticPhase18DataTest.class.getResource(resource), "expected " + resource + " on the classpath");
    }
}
