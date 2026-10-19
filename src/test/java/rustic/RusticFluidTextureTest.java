package rustic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class RusticFluidTextureTest {

    private static final List<String> PLACEABLES = List.of(
            "olive_oil", "ironberry_juice", "wildberry_juice", "grape_juice", "apple_juice", "ale_wort",
            "honey", "golden_apple_juice", "vanta_oil");

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
