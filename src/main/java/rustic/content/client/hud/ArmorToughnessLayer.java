package rustic.content.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import rustic.Rustic;
import rustic.config.RusticConfig;

@OnlyIn(Dist.CLIENT)
public final class ArmorToughnessLayer implements LayeredDraw.Layer {

    private static final ResourceLocation RUSTIC_ICONS =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/icons.png");

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!RusticConfig.TOUGHNESS_HUD.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.gameMode.canHurtPlayer()) {
            return;
        }
        Player player = minecraft.getCameraEntity() instanceof Player p ? p : null;
        if (player == null) {
            return;
        }
        int toughness = Mth.floor(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        int rows = ArmorHudLayout.rows(toughness);
        if (rows <= 0) {
            return;
        }

        int rowHeight = ArmorHudLayout.rowHeight(rows);

        int baselineY = guiGraphics.guiHeight() - minecraft.gui.rightHeight;

        RenderSystem.enableBlend();
        for (int row = 0; row < rows; row++) {
            int y = baselineY - row * rowHeight;
            int right = guiGraphics.guiWidth() / 2 + 82;
            for (int col = 0; col < ArmorHudLayout.ICONS_PER_ROW; col++) {
                int u = ArmorHudLayout.toughnessIconU(ArmorHudLayout.iconFor(toughness, row, col));
                guiGraphics.blit(RUSTIC_ICONS, right, y, u, ArmorHudLayout.TOUGHNESS_ICON_V, 9, 9);
                right -= 8;
            }
        }
        RenderSystem.disableBlend();
    }
}
