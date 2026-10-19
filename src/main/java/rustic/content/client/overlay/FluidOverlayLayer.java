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

@OnlyIn(Dist.CLIENT)
public final class FluidOverlayLayer implements LayeredDraw.Layer {

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
