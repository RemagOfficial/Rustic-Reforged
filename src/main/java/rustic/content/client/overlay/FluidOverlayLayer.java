package rustic.content.client.overlay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidType;
import rustic.content.client.RusticClient;

/**
 * The first-person fluid-submersion screen tint. Reproduces the legacy Rustic behaviour of painting the held-back
 * overlay sheet (the {@code *_overlay.png} textures under {@code block/fluids/}) across the viewport whenever the
 * camera is inside one of the mod's fluids.
 *
 * <p><b>Why not the engine hook.</b> NeoForge already offers {@code IClientFluidTypeExtensions#getRenderOverlayTexture}
 * - {@code ScreenEffectRenderer#renderScreenEffect} calls it and composites the result through {@code renderFluid},
 * but at a hard-coded {@code 0.1} alpha that is barely perceptible over a bright scene (the reported "no overlay"
 * symptom). We therefore leave that hook returning its null default and own the pass here, drawing the same sheet at
 * {@link #SUBMERSION_OPACITY} through {@link GuiScreenOverlay} so it actually reads as a tint.
 *
 * <p><b>Detection.</b> {@code Entity#getEyeInFluidType()} is the same lookup the engine uses, so the trigger matches
 * vanilla water exactly; the brightness multiplier mirrors {@code renderFluid}'s so the tint still darkens in a lit
 * cave the way the water overlay does.
 */
@OnlyIn(Dist.CLIENT)
public final class FluidOverlayLayer implements LayeredDraw.Layer {

    // Clearly visible but still see-through; the engine's 0.1 was the whole reason the tint read as "missing".
    private static final float SUBMERSION_OPACITY = 0.45F;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }
        Level level = minecraft.level;
        Player player = minecraft.getCameraEntity() instanceof Player p ? p : null;
        if (level == null || player == null || player.isSpectator()) {
            return;
        }
        FluidType eyeType = player.getEyeInFluidType();
        if (eyeType.isAir()) {
            return;
        }
        ResourceLocation overlay = RusticClient.FLUID_RENDER_OVERLAYS.get(eyeType);
        if (overlay == null) {
            return;
        }
        BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        float brightness = LightTexture.getBrightness(level.dimensionType(), level.getMaxLocalRawBrightness(eyePos));
        GuiScreenOverlay.draw(guiGraphics, overlay, brightness, brightness, brightness, SUBMERSION_OPACITY);
    }
}
