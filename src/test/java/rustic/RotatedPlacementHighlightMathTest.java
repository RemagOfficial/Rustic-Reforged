package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.AABB;
import rustic.content.client.highlight.RotatedPlacementHighlightMath;

/**
 * Baseline guard for {@link RotatedPlacementHighlightMath}, the pure coordinate rules behind the S9.7
 * rotated-placement marker. The expected values are transcribed from the 1.12.2
 * {@code EventHandlerClient.onBlockHighlight} guards so a refactor fails here rather than only shifting the in-game
 * outline. All boxes are block-local ({@code 0..1}), matching {@code BlockState.getShape(...).bounds()}.
 */
public class RotatedPlacementHighlightMathTest {

    @Test
    void fullCubeIsCentered() {
        assertTrue(RotatedPlacementHighlightMath.isCenteredXZ(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)),
                "a full block is symmetric about 0.5 on both horizontal axes");
    }

    @Test
    void insetSquareFootprintIsCentered() {
        // The vase-style box: 0.125..0.875 horizontally, whose midpoint is exactly 0.5.
        assertTrue(RotatedPlacementHighlightMath.isCenteredXZ(new AABB(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D)),
                "a symmetric inset footprint still centres on 0.5");
    }

    @Test
    void nonSquareButSymmetricFootprintIsCentered() {
        // Different inset per axis but both symmetric about 0.5, so the guard passes and the marker uses the tighter
        // (inset) square.
        assertTrue(RotatedPlacementHighlightMath.isCenteredXZ(new AABB(0.25D, 0.0D, 0.125D, 0.75D, 1.0D, 0.875D)),
                "as long as each axis is centred the marker may be drawn");
    }

    @Test
    void offCenterFootprintIsRejected() {
        assertFalse(RotatedPlacementHighlightMath.isCenteredXZ(new AABB(0.0D, 0.0D, 0.0D, 0.5D, 1.0D, 0.5D)),
                "a block pushed to one corner has no centred face, so the legacy guard skips it");
    }

    @Test
    void faceYTracksTheClickedSide() {
        AABB box = new AABB(0.0D, 0.2D, 0.0D, 1.0D, 0.9D, 1.0D);
        assertEquals(0.9D, RotatedPlacementHighlightMath.faceY(box, true), 1e-9D, "UP uses the top face");
        assertEquals(0.2D, RotatedPlacementHighlightMath.faceY(box, false), 1e-9D, "DOWN uses the bottom face");
    }

    @Test
    void markerSquareIsTheTighterInset() {
        // minX 0.1 vs minZ 0.3 -> the square starts at 0.3; maxX 0.9 vs maxZ 0.7 -> it ends at 0.7.
        AABB box = new AABB(0.1D, 0.0D, 0.3D, 0.9D, 1.0D, 0.7D);
        assertEquals(0.3D, RotatedPlacementHighlightMath.insetMinXZ(box), 1e-9D, "min corner is the larger of the mins");
        assertEquals(0.7D, RotatedPlacementHighlightMath.insetMaxXZ(box), 1e-9D, "max corner is the smaller of the maxes");
    }
}
