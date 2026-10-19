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

/**
 * Phase 17 (§C5 / §C7) characterisation test for the client-visual residuals that were parked in Phase 11
 * and are now restored: the elixir bottle's two-layer tinted model, the eleven potion status icons painted
 * from the legacy {@code potion_icons.png} sheet, and the sweep that deleted the unreferenced legacy
 * {@code rustic:blocks/}-era model files.
 *
 * <p>Like {@code RusticVaseVariantTest}, the asset half reads the JSON/datagen output straight off the test
 * classpath (so it doubles as a {@code runData} freshness gate), and the icon half pins
 * {@link RusticMobEffectIcons#tileFor} — the pure legacy {@code setIconIndex(x, y)} grid arithmetic — so a
 * future tweak to the sheet layout fails loudly instead of silently blitting a neighbour's tile. The actual
 * blit (HUD/inventory) needs a live render and is left to the user's in-game verdict, matching the plan.</p>
 */
public class RusticPhase17VisualsTest {

    // ------------------------------------------------------------------ S17.4 elixir two-layer model

    /**
     * The 1.12.2 {@code item/elixir.json} layered the tintable {@code elixir_overlay} mask over the plain
     * {@code elixir_bottle} glass. Phase 11 had collapsed it to a single un-tinted {@code elixir_bottle}
     * layer; S17.4 restored the pair so the item colour handler can tint index 0 (overlay) while index 1
     * (bottle) stays white. This asserts the regenerated datagen model carries both layers in the legacy order.
     */
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

    /** The two elixir textures the model references are both on the classpath. */
    @Test
    public void elixirLayerTexturesExist() {
        assertResourcePresent("/assets/rustic/textures/item/elixir_overlay.png");
        assertResourcePresent("/assets/rustic/textures/item/elixir_bottle.png");
    }

    /**
     * S17.4 round-2 fix: the elixir rendered <em>invisible</em> (not a missing texture) because the item colour
     * handler returned raw {@code 0xRRGGBB} values. NeoForge 1.21 honours the alpha byte of an item tint - unlike
     * 1.12.2, which ignored it - so an alpha-0 return makes that layer fully transparent; with both layers clear
     * the whole bottle vanished. Vanilla's own {@code ItemColors} never returns a bare RGB (it uses {@code -1} or
     * {@code FastColor.ARGB32.opaque(...)}), and {@link ElixirUtils#getTint} now does the same. Pin both tint
     * layers opaque while keeping the legacy split (index 0 = effect colour, index 1 = white glass).
     */
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

    // ------------------------------------------------------------------ S17.6 DT tree-pack fruit assets

    /**
     * Dynamic Trees registers the olive and ironberry fruit blocks at runtime under {@code rustic:olives} /
     * {@code rustic:ironberries} (an {@code age} 0..3 property, see the vendored {@code Fruit}/{@code FruitBlock}),
     * but — exactly as it does for its own apple — it ships no render asset for an addon's fruit. Without a static
     * {@code blockstates/<fruit>.json} the client falls back to a missing-texture cube (the in-game bug this guards).
     * Assert each age resolves to a real block model and that model references the legacy cluster sprite.
     */
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

    // ------------------------------------------------------------------ S17.5 potion status icons

    /**
     * The eleven legacy {@code PotionBase.getStatusIconIndex()} cells, mapped through
     * {@link RusticMobEffectIcons#tileFor} to their top-left pixel on the 256x256 sheet. The grid is 18x18
     * anchored bottom-left with the first Rustic row in band 11: cell {@code (x, y)} sits at
     * {@code (x * 18, (11 + y) * 18)}. Row 0 (y=198) is ironskin..wither_ward across columns 0..7; row 1
     * (y=216) is fullmetal/undying/fire_power across columns 0..2 — matching the {@code registerEffectIcon}
     * calls in {@code RusticClient}.
     */
    @Test
    public void theElevenEffectIconTilesMatchTheLegacyGrid() {
        // Row 0 (y=0 -> v=198): the eight first-row effects, columns 0..7.
        assertTile(0, 0, 0, 198);    // ironskin
        assertTile(1, 0, 18, 198);   // feather
        assertTile(2, 0, 36, 198);   // blazing_trail
        assertTile(3, 0, 54, 198);   // shame
        assertTile(4, 0, 72, 198);   // tipsy
        assertTile(5, 0, 90, 198);   // full
        assertTile(6, 0, 108, 198);  // magic_resistance
        assertTile(7, 0, 126, 198);  // wither_ward
        // Row 1 (y=1 -> v=216): the three second-row effects, columns 0..2.
        assertTile(0, 1, 0, 216);    // fullmetal
        assertTile(1, 1, 18, 216);   // undying
        assertTile(2, 1, 36, 216);   // fire_power
    }

    private static void assertTile(int x, int y, int expectedU, int expectedV) {
        int[] tile = RusticMobEffectIcons.tileFor(x, y);
        assertEquals(expectedU, tile[0], "tileFor(" + x + ", " + y + ") u drifted from the legacy cell");
        assertEquals(expectedV, tile[1], "tileFor(" + x + ", " + y + ") v drifted from the legacy cell");
    }

    /** The shared sheet every icon blits from is on the classpath (the extension hard-references this path). */
    @Test
    public void thePotionIconSheetIsPresent() {
        assertResourcePresent("/assets/rustic/textures/gui/potion_icons.png");
    }

    // ------------------------------------------------------------------ S17.7 orphan-art sweep (§C7)

    /**
     * §C7 deleted the legacy {@code rustic:blocks/}-era model files that nothing referenced any more (the
     * apple stage twins, the branch/root models DT now renders through runtime loaders, the wildberries
     * overlays and the thirteen dead {@code item/vase_N} files). None may reappear on the classpath: a
     * hand-recreated file would either duplicate a datagen output (failing processResources) or shadow a live
     * model with a broken legacy reference.
     */
    @Test
    public void theSweptLegacyBlockModelsAreGone() {
        for (String name : new String[] {
                "apple_0", "apple_0_1", "apple_1", "apple_1_1", "apple_2", "apple_2_1",
                "ironwoodbranch", "ironwoodroot", "olivebranch", "wildberries_0", "wildberries_1"}) {
            assertResourceGone("/assets/rustic/models/block/" + name + ".json");
        }
    }

    /**
     * The dead {@code item/} twins of the sweep: the branch models and the thirteen per-variant vase items.
     * The vase picker (Phase 16) rides a single {@code item/vase.json} with block-state overrides, so an
     * {@code item/vase_N} model is pure dead weight - unlike {@code block/vase_N}, which legitimately stays.
     */
    @Test
    public void theSweptLegacyItemModelsAreGone() {
        assertResourceGone("/assets/rustic/models/item/ironwoodbranch.json");
        assertResourceGone("/assets/rustic/models/item/olivebranch.json");
        for (int variant = 0; variant <= 12; variant++) {
            assertResourceGone("/assets/rustic/models/item/vase_" + variant + ".json");
        }
        // Guard the sanity boundary: the survivors the sweep deliberately kept must still be present, proving
        // the assertions above are about the deleted item models and not a stale/absent whole classpath.
        assertResourcePresent("/assets/rustic/models/item/vase.json");
        assertResourcePresent("/assets/rustic/models/block/vase_0.json");
    }

    // ------------------------------------------------------------------ helpers

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
