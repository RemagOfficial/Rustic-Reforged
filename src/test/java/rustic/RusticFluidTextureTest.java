package rustic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Guards the fix for the "placing any Rustic fluid crashes the client (and every world load after it)"
 * bug. The crash came from {@code FluidSpriteCache.getFluidSprites} doing {@code Map.of().getOrDefault(null,
 * ...)}: the port registered {@link net.neoforged.neoforge.fluids.FluidType}s with no
 * {@link net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions}, so
 * {@code getStillTexture()} returned {@code DEFAULT}'s null. {@code RusticClient#registerFluidExtensions}
 * now supplies each fluid's still/flow/overlay sprite locations; this test proves those locations match real
 * texture files on the classpath, so the atlas stitches them and a placed fluid renders instead of throwing.
 *
 * <p>The mapping below is intentionally a duplicate of the literals in {@code RusticClient}: if a texture is
 * renamed or a path typo'd, the reference silently becomes the missing-texture sprite (and this test fails),
 * which is exactly the class of mistake that turns a cosmetic gap back into a crash.</p>
 */
public class RusticFluidTextureTest {

    /** Fluids that render as placed blocks (the eight placeables) plus vanta oil: still + flow + overlay. */
    private static final List<String> PLACEABLES = List.of(
            "olive_oil", "ironberry_juice", "wildberry_juice", "grape_juice", "apple_juice", "ale_wort",
            "honey", "golden_apple_juice", "vanta_oil");

    /** The seven boozes: still + flow under the booze/ subfolder, deliberately no overlay. */
    private static final List<String> BOOZE = List.of(
            "ale", "cider", "iron_wine", "mead", "wildberry_wine", "wine", "ambrosia");

    @Test
    public void everyPlaceableFluidHasStillFlowAndOverlayTextures() {
        for (String name : PLACEABLES) {
            assertTextureExists("assets/rustic/textures/block/fluids/" + name + "_still.png");
            assertTextureExists("assets/rustic/textures/block/fluids/" + name + "_flow.png");
            assertTextureExists("assets/rustic/textures/block/fluids/" + name + "_overlay.png");
        }
    }

    @Test
    public void everyBoozeFluidHasStillAndFlowTextures() {
        for (String name : BOOZE) {
            assertTextureExists("assets/rustic/textures/block/fluids/booze/" + name + "_still.png");
            assertTextureExists("assets/rustic/textures/block/fluids/booze/" + name + "_flow.png");
        }
    }

    /**
     * Confirms the null overlay the boozes register is backed by reality: none of them ship an overlay texture,
     * so {@code getOverlayTexture()} returning null for them is correct (an absent overlay falls back to the
     * still/flow sprite in {@code FluidSpriteCache}, so a non-null location there would only be a wrong lookup).
     */
    @Test
    public void noBoozeShipsAnOverlayTexture() {
        for (String name : BOOZE) {
            assertNull(RusticFluidTextureTest.class.getClassLoader().getResource(
                            "assets/rustic/textures/block/fluids/booze/" + name + "_overlay.png"),
                    name + " should not have an overlay texture; the client extension passes null and that is right");
        }
    }

    private static void assertTextureExists(String classpathResource) {
        assertNotNull(RusticFluidTextureTest.class.getClassLoader().getResource(classpathResource),
                "the fluid client extension references " + classpathResource + ", but it is not on the classpath");
        assertTrue(classpathResource.endsWith(".png"), classpathResource);
    }
}
