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

@OnlyIn(Dist.CLIENT)
public final class FullmetalOverlayLayer implements LayeredDraw.Layer {

    private static final ResourceLocation FULLMETAL_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/misc/fullmetal_overlay.png");

    private static final float SCREEN_OPACITY = 0.22F;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();

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

        GuiScreenOverlay.draw(guiGraphics, FULLMETAL_OVERLAY, brightness, brightness, brightness, alpha);
    }
}
