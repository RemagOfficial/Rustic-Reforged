package rustic.content.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import rustic.config.RusticConfig;
import rustic.content.client.hud.ArmorHudLayout.Icon;

/**
 * S9.5: draws the armor rows vanilla omits. {@code Gui#renderArmor} paints a single row of ten icons and
 * clips anything above 20 armour points, so a player carrying Rustic's iron-skin {@code Attributes.ARMOR}
 * bonus loses the excess from view. Registered immediately above {@code VanillaGuiLayers.ARMOR_LEVEL}, this
 * layer paints only the overflow rows (row index 1+) directly on top of the vanilla row, in the vanilla
 * full/half/empty sprites so the two halves read as one continuous meter.
 *
 * <p>No-ops when {@code EXTRA_ARMOR_HUD} is off or armour fits in one row; the vanilla layer is left
 * untouched, so the common case is pixel-identical to an unmodded client.
 */
@OnlyIn(Dist.CLIENT)
public final class ArmorOverflowLayer implements LayeredDraw.Layer {

    private static final ResourceLocation ARMOR_FULL = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation ARMOR_HALF = ResourceLocation.withDefaultNamespace("hud/armor_half");
    private static final ResourceLocation ARMOR_EMPTY = ResourceLocation.withDefaultNamespace("hud/armor_empty");

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!RusticConfig.EXTRA_ARMOR_HUD.get()) {
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
        int armor = player.getArmorValue();
        int rows = ArmorHudLayout.rows(armor);
        if (rows <= 1) {
            // Everything fits in the single row vanilla already drew.
            return;
        }

        int rowHeight = ArmorHudLayout.rowHeight(rows);
        int x0 = guiGraphics.guiWidth() / 2 - 91;
        // Row 0 (the vanilla row) sat at guiHeight - leftHeightBefore; leftHeight has since been bumped by 10,
        // so reconstruct it, then stack each overflow row one rowHeight higher.
        int baselineY = guiGraphics.guiHeight() - minecraft.gui.leftHeight + ArmorHudLayout.BASE_ROW_HEIGHT;

        RenderSystem.enableBlend();
        for (int row = 1; row < rows; row++) {
            int y = baselineY + ArmorHudLayout.extraRowOffsetY(row, rowHeight);
            for (int col = 0; col < ArmorHudLayout.ICONS_PER_ROW; col++) {
                int x = x0 + col * 8;
                guiGraphics.blitSprite(spriteFor(ArmorHudLayout.iconFor(armor, row, col)), x, y, 9, 9);
            }
        }
        RenderSystem.disableBlend();
    }

    private static ResourceLocation spriteFor(Icon icon) {
        return switch (icon) {
            case FULL -> ARMOR_FULL;
            case HALF -> ARMOR_HALF;
            case EMPTY -> ARMOR_EMPTY;
        };
    }
}
