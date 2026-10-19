package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import rustic.content.client.RusticMobEffectIcons;
import rustic.content.potions.ElixirUtils;

public class RusticPhase17VisualsTest {

    @Test
    public void elixirItemModelCarriesTheTwoLegacyLayers() {
        JsonObject elixir = json("/assets/rustic/models/item/elixir.json");
        assertEquals("minecraft:item/generated", elixir.get("parent").getAsString(),
                "the elixir should be a flat generated item with the vanilla parent");
        JsonObject textures = elixir.getAsJsonObject("textures");
        assertEquals("rustic:item/elixir_overlay", textures.get("layer0").getAsString(),
                "layer0 must be the tintable overlay mask (tint index 0 -> ElixirUtils.getColor)");
        assertEquals("rustic:item/elixir_bottle", textures.get("layer1").getAsString(),
                "layer1 must be the glass bottle (tint index 1, left white by the colour handler)");
    }

    @Test
    public void elixirLayerTexturesExist() {
        assertResourcePresent("/assets/rustic/textures/item/elixir_overlay.png");
        assertResourcePresent("/assets/rustic/textures/item/elixir_bottle.png");
    }

    @Test
    public void elixirTintIsOpaqueOnBothLayers() {
        ItemStack stack = ItemStack.EMPTY;
        int liquid = ElixirUtils.getTint(stack, 0);
        int glass = ElixirUtils.getTint(stack, 1);
        assertEquals(0xFF, liquid >>> 24,
                "the liquid-mask tint must be opaque or the elixir renders invisible");
        assertEquals(0xFF, glass >>> 24,
                "the glass-bottle tint must be opaque or the elixir renders invisible");
        assertEquals(0xFF000000 | ElixirUtils.getColor(stack), liquid,
                "index 0 should be the opaque first-effect colour");
        assertEquals(0xFFFFFFFF, glass, "index 1 should be opaque white glass");
    }

    @Test
    public void theDynamicTreesFruitBlocksHaveRealModels() {
        assertFruit("olives", "rustic:item/olives");
        assertFruit("ironberries", "rustic:item/ironberries");
    }

    private static void assertFruit(String fruit, String texture) {
        JsonObject blockstate = json("/assets/rustic/blockstates/" + fruit + ".json");
        JsonObject variants = blockstate.getAsJsonObject("variants");
        for (int age = 0; age <= 3; age++) {
            String key = "age=" + age;
            assertNotNull(variants.get(key), fruit + " blockstate is missing the " + key + " variant");
            assertEquals("rustic:block/" + fruit,
                    variants.getAsJsonObject(key).getAsJsonObject("apply") == null
                            ? variants.getAsJsonObject(key).get("model").getAsString()
                            : variants.getAsJsonObject(key).getAsJsonObject("apply").get("model").getAsString(),
                    fruit + " " + key + " should point at the authored fruit model, not a missing cube");
        }
        JsonObject model = json("/assets/rustic/models/block/" + fruit + ".json");
        assertEquals(texture, model.getAsJsonObject("textures").get("cross").getAsString(),
                fruit + " model should draw the legacy cluster sprite");
        assertResourcePresent("/assets/rustic/textures/" + texture.substring("rustic:".length()) + ".png");
    }

    @Test
    public void theElevenEffectIconTilesMatchTheLegacyGrid() {

        assertTile(0, 0, 0, 198);

        assertTile(1, 0, 18, 198);

        assertTile(2, 0, 36, 198);

        assertTile(3, 0, 54, 198);

        assertTile(4, 0, 72, 198);

        assertTile(5, 0, 90, 198);

        assertTile(6, 0, 108, 198);

        assertTile(7, 0, 126, 198);

        assertTile(0, 1, 0, 216);

        assertTile(1, 1, 18, 216);

        assertTile(2, 1, 36, 216);

    }

    private static void assertTile(int x, int y, int expectedU, int expectedV) {
        int[] tile = RusticMobEffectIcons.tileFor(x, y);
        assertEquals(expectedU, tile[0], "tileFor(" + x + ", " + y + ") u drifted from the legacy cell");
        assertEquals(expectedV, tile[1], "tileFor(" + x + ", " + y + ") v drifted from the legacy cell");
    }

    @Test
    public void thePotionIconSheetIsPresent() {
        assertResourcePresent("/assets/rustic/textures/gui/potion_icons.png");
    }

    @Test
    public void theSweptLegacyBlockModelsAreGone() {
        for (String name : new String[] {
                "apple_0", "apple_0_1", "apple_1", "apple_1_1", "apple_2", "apple_2_1",
                "ironwoodbranch", "ironwoodroot", "olivebranch", "wildberries_0", "wildberries_1"}) {
            assertResourceGone("/assets/rustic/models/block/" + name + ".json");
        }
    }

    @Test
    public void theSweptLegacyItemModelsAreGone() {
        assertResourceGone("/assets/rustic/models/item/ironwoodbranch.json");
        assertResourceGone("/assets/rustic/models/item/olivebranch.json");
        for (int variant = 0; variant <= 12; variant++) {
            assertResourceGone("/assets/rustic/models/item/vase_" + variant + ".json");
        }

        assertResourcePresent("/assets/rustic/models/item/vase.json");
        assertResourcePresent("/assets/rustic/models/block/vase_0.json");
    }

    private static JsonObject json(String classpathResource) {
        try (Reader reader = new InputStreamReader(resourceStream(classpathResource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            return fail("could not read " + classpathResource, e);
        }
    }

    private static InputStream resourceStream(String resource) {
        InputStream in = RusticPhase17VisualsTest.class.getResourceAsStream(resource);
        assertNotNull(in, "missing resource on the classpath: " + resource
                + " - if this is new content, re-run runData rather than writing the file by hand");
        return in;
    }

    private static void assertResourcePresent(String resource) {
        assertNotNull(RusticPhase17VisualsTest.class.getResource(resource),
                "expected " + resource + " on the classpath");
    }

    private static void assertResourceGone(String resource) {
        assertNull(RusticPhase17VisualsTest.class.getResource(resource),
                resource + " was deleted in the Phase 17 orphan sweep and must not be on the classpath");
    }
}
