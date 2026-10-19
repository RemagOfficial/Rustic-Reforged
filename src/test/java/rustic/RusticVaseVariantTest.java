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

/**
 * Phase 16 ({@code UNPORTED_BACKLOG} §C1) characterisation test for the vase variant picker: the thirteen
 * styles that 1.12.2 rode on item damage now ride the stack's {@code DataComponents.BLOCK_STATE}, cycled
 * by the sneak + mouse-wheel handler through the legacy {@code ITEM_ORDER} table.
 *
 * <p>Asserts the pure wheel arithmetic (verbatim from {@code EventHandlerClient.onVaseMouseWheel}), the
 * component round-trip the placement/pick-block/drop paths share, and the asset side the datagen gate just
 * produced: a 13-variant blockstate with no {@code vase_0} aliasing left, the seven restored {@code vase_6..12}
 * statics with correct parents and texture-key remaps, and the hand-authored override item model. The live
 * wheel event and the server packet need a running game and are checked in the user's in-game verdict, not here.
 */
public class RusticVaseVariantTest {

    // ------------------------------------------------------------------ wheel arithmetic

    /** Legacy {@code BlockVase.ITEM_ORDER}: a full permutation of 0..12, cycled in table order, not numeric. */
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

    /**
     * The wheel walk of {@code EventHandlerClient.onVaseMouseWheel}: stepping forward from brown (0) goes to
     * the horse style (5), not to variant 1; negative offsets walk backwards; a full cycle of 13 notches is
     * the identity; and multiple notches (a fast scroll, the old {@code dwheel / 120} could exceed 1) skip
     * that many table entries.
     */
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

    /**
     * The legacy else-branch: an out-of-table value (unreachable through the picker, reachable from old-save
     * damage) wraps raw 0..12 instead of the table. The port's variant property clamps to 0..12, so the raw
     * branch only pins the wrap maths for offsets.
     */
    @Test
    public void offsetWrapNormalisesNegatives() {
        // Every legal variant is in the table, so exercise the wrap through the table branch: entering at
        // each index and leaving 13 notches later must return to the same style.
        for (int variant = VaseBlock.MIN_VARIANT; variant <= VaseBlock.MAX_VARIANT; variant++) {
            assertEquals(variant, VaseBlock.nextVariant(variant, 13), "variant " + variant + " is not wheel-stable");
            assertEquals(variant, VaseBlock.nextVariant(variant, -13), "variant " + variant + " is not wheel-stable");
        }
    }

    // ------------------------------------------------------------------ component round-trip

    /**
     * The 1.21 replacement for legacy's damage-as-variant: a stamped stack reads back the variant, and an
     * unstamped stack (a plain creative tab stack) defaults to 0, brown ceramic.
     */
    @Test
    public void variantStackRoundTripsThroughTheBlockStateComponent() {
        for (int variant = VaseBlock.MIN_VARIANT; variant <= VaseBlock.MAX_VARIANT; variant++) {
            assertEquals(variant, VaseBlock.variantOf(VaseBlock.variantStack(variant)),
                    "variant " + variant + " did not survive the BLOCK_STATE round trip");
        }
        assertEquals(0, VaseBlock.variantOf(new ItemStack(RusticMachineBlocks.VASE.get())),
                "an unstamped vase stack must read as the default style");
    }

    /**
     * Pick-block (middle-click on a placed green vase) must hand back a green vase stack, the 1.21 form of
     * legacy's damage-carrying {@code ItemBlock#getStackUseAmount}. {@code getCloneItemStack} ignores the
     * level/pos arguments, so nulls are safe in the unit-test JVM.
     */
    @Test
    public void pickBlockClonesThePlacedVariant() {
        Block vase = RusticMachineBlocks.VASE.get();
        assertSame(VaseBlock.class, vase.getClass(), "rustic:vase is not a VaseBlock");
        ItemStack clone = vase.getCloneItemStack(null, null, vase.defaultBlockState().setValue(VaseBlock.VARIANT, 3));
        assertTrue(clone.is(RusticMachineBlocks.VASE.get().asItem()), "pick-block lost the vase item itself");
        assertEquals(3, VaseBlock.variantOf(clone), "the cloned stack dropped the placed variant");
    }

    // ------------------------------------------------------------------ blockstate datagen

    /**
     * The §C1 bug itself: before Phase 16 the generated blockstate aliased variant=6..12 onto
     * {@code rustic:block/vase_0}. Each of the thirteen states must now name its own model.
     */
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

    // ------------------------------------------------------------------ the restored models

    /**
     * The seven statics authored in Phase 16. vase_6/7 parent vase_0 and vase_8..12 parent vase_1 (the two
     * legacy geometry families), and each child must remap the PARENT'S numeric texture keys 0/1/2 (the
     * upstream 1.12 item jsons remapped non-existent side/top/bottom keys, which is why variants 6+ rendered
     * broken even then) plus the particle slot onto its own vases/ texture set.
     */
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

    /** The art was never deleted: all thirteen vases/ texture sets exist on the classpath. */
    @Test
    public void everyVariantHasItsTextureSet() {
        for (int variant = 0; variant <= 12; variant++) {
            for (String part : new String[] {"side", "top", "bottom"}) {
                String path = "/assets/rustic/textures/block/vases/vase_" + part + "_" + variant + ".png";
                assertNotNull(RusticVaseVariantTest.class.getResource(path), "missing texture: " + path);
            }
        }
    }

    // ------------------------------------------------------------------ the override item model

    /**
     * The hand-authored item model: parents vase_0 (variant 0 needs no override) and carries exactly twelve
     * overrides, one per non-default variant, each pointing at its own block model. This is the file datagen
     * can no longer produce ({@code simpleBlockItem} has no override support), which is why the generator's
     * vase item line was removed in Phase 16.
     */
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

    // ------------------------------------------------------------------ creative tab

    /**
     * Legacy's ItemBlock never overrode getSubItems, so creative showed exactly ONE vase (variant 0); the
     * wheel picker is the path to the other twelve. The blanket DECOR machine append reproduces that - assert
     * the vase is present exactly once.
     */
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

    // ------------------------------------------------------------------ helpers

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
