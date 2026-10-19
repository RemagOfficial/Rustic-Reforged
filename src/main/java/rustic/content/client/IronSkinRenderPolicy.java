package rustic.content.client;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class IronSkinRenderPolicy {

    public static final float DEFAULT_OPACITY = 0.9F;

    public static float opacityFor(int amplifier, boolean fullmetal, boolean ironSkin) {
        if (fullmetal) {
            return 0.92F;
        }
        if (ironSkin) {
            if (amplifier == 0) return 0.875F;
            if (amplifier == 1) return 0.89F;
            if (amplifier == 2) return 0.91F;
            return 0.92F;
        }
        return DEFAULT_OPACITY;
    }

    public static boolean shouldOverlay(boolean invisible, boolean hasIronSkin, boolean hasFullmetal, boolean forceRender) {
        if (invisible) {
            return false;
        }
        return hasIronSkin || hasFullmetal || forceRender;
    }

    public static boolean isEntityBlacklisted(ResourceLocation entityId, List<? extends String> blacklist) {
        if (entityId == null || blacklist == null) {
            return false;
        }
        String id = entityId.toString();
        for (String entry : blacklist) {
            if (entry != null && !entry.isBlank() && entry.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private IronSkinRenderPolicy() {
    }
}
