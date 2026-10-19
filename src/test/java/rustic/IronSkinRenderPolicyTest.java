package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import rustic.content.client.IronSkinRenderPolicy;

public class IronSkinRenderPolicyTest {

    @Test
    void opacityLadderMatchesLegacy() {

        assertEquals(0.92F, IronSkinRenderPolicy.opacityFor(-1, true, false), 0.0F, "fullmetal, no ironskin");
        assertEquals(0.92F, IronSkinRenderPolicy.opacityFor(2, true, true), 0.0F, "fullmetal wins over ironskin");

        assertEquals(0.875F, IronSkinRenderPolicy.opacityFor(0, false, true), 0.0F, "ironskin amp 0");
        assertEquals(0.89F, IronSkinRenderPolicy.opacityFor(1, false, true), 0.0F, "ironskin amp 1");
        assertEquals(0.91F, IronSkinRenderPolicy.opacityFor(2, false, true), 0.0F, "ironskin amp 2");
        assertEquals(0.92F, IronSkinRenderPolicy.opacityFor(3, false, true), 0.0F, "ironskin amp 3+");
        assertEquals(0.92F, IronSkinRenderPolicy.opacityFor(9, false, true), 0.0F, "ironskin high amp clamps to 0.92");

        assertEquals(IronSkinRenderPolicy.DEFAULT_OPACITY, IronSkinRenderPolicy.opacityFor(-1, false, false), 0.0F,
                "no effect default");
    }

    @Test
    void overlaySkipsInvisibleAndUneffected() {
        assertFalse(IronSkinRenderPolicy.shouldOverlay(true, true, true, true),
                "an invisible entity never paints, even with effects and the force flag");

        assertTrue(IronSkinRenderPolicy.shouldOverlay(false, true, false, false), "ironskin paints");
        assertTrue(IronSkinRenderPolicy.shouldOverlay(false, false, true, false), "fullmetal paints");
        assertTrue(IronSkinRenderPolicy.shouldOverlay(false, false, false, true), "debug force paints");

        assertFalse(IronSkinRenderPolicy.shouldOverlay(false, false, false, false),
                "a plain visible player with no effects and no force is left untouched");
    }

    @Test
    void blacklistMatchesRawEntityIds() {
        List<String> blacklist = List.of("minecraft:slime", "minecraft:shulker");

        assertTrue(IronSkinRenderPolicy.isEntityBlacklisted(ResourceLocation.fromNamespaceAndPath("minecraft", "slime"), blacklist),
                "slime is on the list");
        assertTrue(IronSkinRenderPolicy.isEntityBlacklisted(ResourceLocation.fromNamespaceAndPath("minecraft", "shulker"), blacklist),
                "shulker is on the list");
        assertFalse(IronSkinRenderPolicy.isEntityBlacklisted(ResourceLocation.fromNamespaceAndPath("minecraft", "player"), blacklist),
                "player is not on the list");

        assertFalse(IronSkinRenderPolicy.isEntityBlacklisted(null, blacklist), "null id is never blacklisted");
        assertFalse(IronSkinRenderPolicy.isEntityBlacklisted(ResourceLocation.fromNamespaceAndPath("minecraft", "slime"), null),
                "null list never blacklists");
        assertFalse(IronSkinRenderPolicy.isEntityBlacklisted(ResourceLocation.fromNamespaceAndPath("minecraft", "slime"), List.of("", "  ")),
                "blank entries are ignored");
    }
}
