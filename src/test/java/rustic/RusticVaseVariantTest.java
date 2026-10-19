package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import rustic.content.RusticCreativeTabs;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.blocks.VaseBlock;

public class RusticVaseVariantTest {

    @Test
    public void itemOrderIsAPermutationOfTheThirteenVariants() {
        assertEquals(13, VaseBlock.ITEM_ORDER.length, "ITEM_ORDER length drifted from 1.12.2");
        boolean[] seen = new boolean[13];
        for (int variant : VaseBlock.ITEM_ORDER) {
            assertTrue(variant >= 0 && variant <= 12, "ITEM_ORDER entry out of range: " + variant);
            assertTrue(!seen[variant], "ITEM_ORDER is not a permutation, variant " + variant + " appears twice");
            seen[variant] = true;
        }
    }

    @Test
    public void wheelWalkFollowsTheItemOrderTable() {
        assertEquals(5, VaseBlock.nextVariant(0, 1), "0 -> 1 notch = brown-horse (5)");
        assertEquals(6, VaseBlock.nextVariant(5, 1), "5 -> 1 notch = white (6)");
        assertEquals(2, VaseBlock.nextVariant(6, 1), "6 -> 1 notch = white-blue (2)");
        assertEquals(0, VaseBlock.nextVariant(11, 1), "the last table entry (11) wraps to 0");
        assertEquals(11, VaseBlock.nextVariant(0, -1), "one notch back from 0 = 11");
        assertEquals(0, VaseBlock.nextVariant(0, 13), "a full cycle is the identity");
        assertEquals(0, VaseBlock.nextVariant(0, -13), "a full backward cycle is the identity");
        assertEquals(6, VaseBlock.nextVariant(0, 2), "two notches ahead walk two table entries (0 -> 5 -> 6)");
        assertEquals(9, VaseBlock.nextVariant(6, 7), "the mid-table long jump lands on the alt white (index 2 + 7 = 9)");
    }

    @Test
    public void offsetWrapNormalisesNegatives() {

        for (int variant = VaseBlock.MIN_VARIANT; variant <= VaseBlock.MAX_VARIANT; variant++) {
            assertEquals(variant, VaseBlock.nextVariant(variant, 13), "variant " + variant + " is not wheel-stable");
            assertEquals(variant, VaseBlock.nextVariant(variant, -13), "variant " + variant + " is not wheel-stable");
        }
    }

    @Test
    public void variantStackRoundTripsThroughTheBlockStateComponent() {
        for (int variant = VaseBlock.MIN_VARIANT; variant <= VaseBlock.MAX_VARIANT; variant++) {
            assertEquals(variant, VaseBlock.variantOf(VaseBlock.variantStack(variant)),
                    "variant " + variant + " did not survive the BLOCK_STATE round trip");
        }
        assertEquals(0, VaseBlock.variantOf(new ItemStack(RusticMachineBlocks.VASE.get())),
                "an unstamped vase stack must read as the default style");
    }

    @Test
    public void pickBlockClonesThePlacedVariant() {
        Block vase = RusticMachineBlocks.VASE.get();
        assertSame(VaseBlock.class, vase.getClass(), "rustic:vase is not a VaseBlock");
        ItemStack clone = vase.getCloneItemStack(null, null, vase.defaultBlockState().setValue(VaseBlock.VARIANT, 3));
        assertTrue(clone.is(RusticMachineBlocks.VASE.get().asItem()), "pick-block lost the vase item itself");
        assertEquals(3, VaseBlock.variantOf(clone), "the cloned stack dropped the placed variant");
    }

    @Test
    public void blockstateMapsEveryVariantToItsOwnModel() {
        JsonObject variants = json("/assets/rustic/blockstates/vase.json").getAsJsonObject("variants");
        assertEquals(13, variants.size(), "the vase blockstate should carry all thirteen variants");
        for (int variant = 0; variant <= 12; variant++) {
            JsonObject state = variants.getAsJsonObject("variant=" + variant);
            assertNotNull(state, "no blockstate entry for variant=" + variant);
            assertEquals("rustic:block/vase_" + variant, state.get("model").getAsString(),
                    "variant " + variant + " is still aliased or drifted from its own model");
        }
    }

    @Test
    public void restoredVariantModelsRemapTheNumericTextureKeys() {
        for (int variant = 6; variant <= 12; variant++) {
            String parent = variant <= 7 ? "rustic:block/vase_0" : "rustic:block/vase_1";
            JsonObject model = json("/assets/rustic/models/block/vase_" + variant + ".json");
            assertEquals(parent, model.get("parent").getAsString(),
                    "vase_" + variant + " does not parent its geometry family");
            JsonObject textures = model.getAsJsonObject("textures");
            assertEquals("rustic:block/vases/vase_side_" + variant, textures.get("0").getAsString(),
                    "vase_" + variant + " side key");
            assertEquals("rustic:block/vases/vase_top_" + variant, textures.get("1").getAsString(),
                    "vase_" + variant + " top key");
            assertEquals("rustic:block/vases/vase_bottom_" + variant, textures.get("2").getAsString(),
                    "vase_" + variant + " bottom key");
            assertEquals("rustic:block/vases/vase_side_" + variant, textures.get("particle").getAsString(),
                    "vase_" + variant + " particle key");
        }
    }

    @Test
    public void everyVariantHasItsTextureSet() {
        for (int variant = 0; variant <= 12; variant++) {
            for (String part : new String[] {"side", "top", "bottom"}) {
                String path = "/assets/rustic/textures/block/vases/vase_" + part + "_" + variant + ".png";
                assertNotNull(RusticVaseVariantTest.class.getResource(path), "missing texture: " + path);
            }
        }
    }

    @Test
    public void itemModelCarriesTwelveVariantOverrides() {
        JsonObject item = json("/assets/rustic/models/item/vase.json");
        assertEquals("rustic:block/vase_0", item.get("parent").getAsString());
        JsonArray overrides = item.getAsJsonArray("overrides");
        assertEquals(12, overrides.size(), "one override per non-default variant");
        for (int variant = 1; variant <= 12; variant++) {
            JsonObject override = overrides.get(variant - 1).getAsJsonObject();
            assertEquals(variant, override.getAsJsonObject("predicate").get("rustic:vase_variant").getAsInt(),
                    "override " + variant + " carries the wrong predicate value");
            assertEquals("rustic:block/vase_" + variant, override.get("model").getAsString(),
                    "override " + variant + " points at the wrong model");
        }
    }

    @Test
    public void decorTabOffersExactlyOneVaseStack() {
        List<String> decor = tabIds(RusticCreativeTabs.DECOR.get());
        int count = 0;
        for (String id : decor) {
            if ("rustic:vase".equals(id)) {
                count++;
            }
        }
        assertEquals(1, count, "DECOR must list the vase once, as 1.12.2 did (wheel-picker, not 13 sub-items): " + decor);
    }

    private static List<String> tabIds(CreativeModeTab tab) {
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        List<String> ids = new ArrayList<>();
        for (ItemStack stack : tab.getDisplayItems()) {
            ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        return ids;
    }

    private static JsonObject json(String classpathResource) {
        try (Reader reader = new InputStreamReader(resourceStream(classpathResource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            return fail("could not read " + classpathResource, e);
        }
    }

    private static java.io.InputStream resourceStream(String resource) {
        java.io.InputStream in = RusticVaseVariantTest.class.getResourceAsStream(resource);
        assertNotNull(in, "missing resource on the classpath: " + resource
                + " - if this is new content, re-run runData rather than writing the file by hand");
        return in;
    }
}
