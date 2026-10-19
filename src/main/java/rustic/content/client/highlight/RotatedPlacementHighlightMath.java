package rustic.content.client.highlight;

import net.minecraft.world.phys.AABB;

public final class RotatedPlacementHighlightMath {

    public static boolean isCenteredXZ(AABB box) {
        double centerX = box.minX + (box.maxX - box.minX) / 2.0D;
        double altCenterX = box.maxX - (box.maxX - box.minX) / 2.0D;
        double centerZ = box.minZ + (box.maxZ - box.minZ) / 2.0D;
        double altCenterZ = box.maxZ - (box.maxZ - box.minZ) / 2.0D;
        return (centerX == 0.5D || altCenterX == 0.5D) && (centerZ == 0.5D || altCenterZ == 0.5D);
    }

    public static double faceY(AABB box, boolean clickedUp) {
        return clickedUp ? box.maxY : box.minY;
    }

    public static double insetMinXZ(AABB box) {
        return Math.max(box.minX, box.minZ);
    }

    public static double insetMaxXZ(AABB box) {
        return Math.min(box.maxX, box.maxZ);
    }

    private RotatedPlacementHighlightMath() {
    }
}
