package rustic.content.client;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The pure decision logic behind the iron-skin / fullmetal body paint, lifted out of
 * {@link rustic.content.client.renderer.IronSkinRenderLayer} so it can be asserted headlessly (the
 * layer itself needs a live {@code PoseStack}/{@code MultiBufferSource} and so cannot run under a
 * plain JUnit gate).
 *
 * <p>Every number here is transcribed verbatim from the 1.12.2 {@code LayerIronSkin.doRenderLayer}
 * (the {@code opacity} ladder at lines 163-171 and the skip ladder at lines 120-130), so the ported
 * overlay darkens a player's skin to the same degree per effect amplifier as the original did.
 *
 * <p>Scope note: 1.12.2 attached the layer to <em>every</em> {@code RenderLivingBase}. 1.21 entity
 * models no longer expose their {@code textureWidth}/{@code textureHeight} to a render layer, so the
 * legacy size-matched UV trick is only reliably reproducible against the fixed 64x32 player model.
 * This slice therefore paints players; the mob-wide repaint is knowingly narrowed (documented in the
 * handoff), not silently dropped.
 */
public final class IronSkinRenderPolicy {

    /** Opacity used when neither effect is active but a render is nonetheless forced (debug path). */
    public static final float DEFAULT_OPACITY = 0.9F;

    /**
     * The overlay alpha for the given effect state, mirroring the legacy ladder:
     * fullmetal always wins at {@code 0.92}; otherwise iron-skin maps amplifier 0/1/2 to
     * {@code 0.875}/{@code 0.89}/{@code 0.91} and any higher amplifier to {@code 0.92}; with no
     * effect at all the {@link #DEFAULT_OPACITY} applies (only reachable through the force flag).
     *
     * @param amplifier the iron-skin amplifier ({@code MobEffectInstance#getAmplifier}, 0-based);
     *                  ignored when {@code ironSkin} is {@code false}
     * @param fullmetal whether the fullmetal effect is active
     * @param ironSkin  whether the iron-skin effect is active
     */
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

    /**
     * Whether the metal overlay should be painted at all. The legacy skip ladder: an invisible
     * entity never paints; otherwise it paints only while iron-skin or fullmetal is active, plus a
     * debug force switch that ignores both effects.
     */
    public static boolean shouldOverlay(boolean invisible, boolean hasIronSkin, boolean hasFullmetal, boolean forceRender) {
        if (invisible) {
            return false;
        }
        return hasIronSkin || hasFullmetal || forceRender;
    }

    /**
     * Whether {@code entityId} is on the {@code ironskin_render_entity_blacklist}. Compared against the
     * string form (e.g. {@code "minecraft:slime"}), matching how the config stores raw ids. A
     * {@code null} id or list is never blacklisted; blank entries are ignored.
     */
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
