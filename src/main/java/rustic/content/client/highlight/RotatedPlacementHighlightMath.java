package rustic.content.client.highlight;

import net.minecraft.world.phys.AABB;

/**
 * Pure geometry behind the S9.7 rotated-placement highlight, transcribed verbatim from the 1.12.2
 * {@code EventHandlerClient.onBlockHighlight} / {@code drawRotablePlacementBlockHighlight} pair so the arithmetic can
 * be unit-tested without a render context.
 *
 * <p>The legacy code drew a diagonal "X" marker on the top or bottom face of a hovered block that implements
 * {@link rustic.content.blocks.AdvancedRotationPlacement}, so the player can see which of the four horizontal
 * directions a click on that face will rotate the block toward. Two guards and three coordinate derivations carry
 * over unchanged:
 * <ul>
 *   <li>the marker is only drawn when the selection box is symmetric about the block centre on both horizontal axes
 *   (an off-centre shape would put the diagonals outside the visible footprint);</li>
 *   <li>the face is the one that was clicked - {@code UP} uses the box's {@code maxY}, anything else its
 *   {@code minY};</li>
 *   <li>the four marker corners are the inset square {@code [max(minX,minZ), min(maxX,maxZ)]} on that face, so a
 *   non-square footprint still gets a square, centred marker.</li>
 * </ul>
 */
public final class RotatedPlacementHighlightMath {

    /**
     * The legacy {@code isAABBCenteredXZ}: the horizontal centre of the box must land on {@code 0.5} in block-local
     * space. Both the forward and reverse midpoint expressions are reproduced because the original wrote them as an
     * {@code ||}; they evaluate to the same value but the guard is kept character-for-character.
     */
    public static boolean isCenteredXZ(AABB box) {
        double centerX = box.minX + (box.maxX - box.minX) / 2.0D;
        double altCenterX = box.maxX - (box.maxX - box.minX) / 2.0D;
        double centerZ = box.minZ + (box.maxZ - box.minZ) / 2.0D;
        double altCenterZ = box.maxZ - (box.maxZ - box.minZ) / 2.0D;
        return (centerX == 0.5D || altCenterX == 0.5D) && (centerZ == 0.5D || altCenterZ == 0.5D);
    }

    /** {@code y = (side == UP) ? AABB.maxY : AABB.minY} - the face the marker is drawn on, before the block offset. */
    public static double faceY(AABB box, boolean clickedUp) {
        return clickedUp ? box.maxY : box.minY;
    }

    /** {@code minXZ = Math.max(AABB.minX, AABB.minZ)}: the inset square that keeps the marker inside the footprint. */
    public static double insetMinXZ(AABB box) {
        return Math.max(box.minX, box.minZ);
    }

    /** {@code maxXZ = Math.min(AABB.maxX, AABB.maxZ)}. */
    public static double insetMaxXZ(AABB box) {
        return Math.min(box.maxX, box.maxZ);
    }

    private RotatedPlacementHighlightMath() {
    }
}
