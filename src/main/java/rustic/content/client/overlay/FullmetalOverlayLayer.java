package rustic.content.client.overlay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import rustic.Rustic;
import rustic.content.potions.RusticMobEffects;

/**
 * S9.6: the first-person "your hands have turned to metal" screen tint. The 1.12.2 original hooked
 * {@code RenderHandEvent}, cancelled it, re-rendered the held item, then drew a full-screen {@code POSITION_TEX}
 * quad of {@code rustic:textures/misc/fullmetal_overlay.png} multiplied by the scene brightness and a
 * base-alpha * duration-fade term.
 *
 * <p>1.21 has no clean way to inject a stretched full-screen textured quad inside the hand-render pass, so the
 * overlay is instead a bottom-most {@link LayeredDraw.Layer} (registered via {@code registerBelowAll}) that paints
 * the whole GUI viewport <em>under</em> the rest of the HUD. It reproduces the legacy visibility guards - first
 * person, not a spectator, GUI not hidden, not sleeping, no open screen - and the legacy alpha via
 * {@link FullmetalOverlayMath}. The per-vertex brightness * alpha tint is applied through
 * {@link GuiGraphics#setColor} around a UV {@code 0..1} blit stretched to the screen.
 */
@OnlyIn(Dist.CLIENT)
public final class FullmetalOverlayLayer implements LayeredDraw.Layer {

    private static final ResourceLocation FULLMETAL_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/misc/fullmetal_overlay.png");

    // The legacy overlay composited with a GL multiply blend (SRC_COLOR, ZERO): even at the legacy baseAlpha of 0.9625
    // the metal sheet only darkened/tinted the scene, it never hid it. GuiGraphics.blit can only do straight SRC_ALPHA
    // blending, so reusing 0.9625 painted a near-opaque film that blanked the screen (the reported "can't see anything"
    // defect). We keep the faithful duration-fade ramp but scale the whole thing down to a see-through metallic sheen.
    private static final float SCREEN_OPACITY = 0.22F;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        // Only the in-world first-person view gets the tint; a live screen owns the display otherwise.
        if (minecraft.screen != null) {
            return;
        }
        if (!minecraft.options.getCameraType().isFirstPerson() || minecraft.options.hideGui) {
            return;
        }
        Player player = minecraft.getCameraEntity() instanceof Player p ? p : null;
        if (player == null || player.isSpectator() || player.isSleeping()) {
            return;
        }
        MobEffectInstance effect = player.getEffect(RusticMobEffects.FULLMETAL);
        if (effect == null) {
            return;
        }

        float brightness = player.getLightLevelDependentMagicValue();
        float fade = FullmetalOverlayMath.durationFade(effect.getDuration(), deltaTracker.getGameTimeDeltaPartialTick(false));
        float alpha = SCREEN_OPACITY * fade;

        // POSITION_TEX_COLOR with the alpha baked into every vertex (via GuiScreenOverlay) is what actually composites
        // the sheen translucently. GuiGraphics#blit's POSITION_TEX path ignores the tint alpha entirely, which is why
        // the earlier setColor + enableBlend attempts still painted a fully-opaque film.
        GuiScreenOverlay.draw(guiGraphics, FULLMETAL_OVERLAY, brightness, brightness, brightness, alpha);
    }
}
