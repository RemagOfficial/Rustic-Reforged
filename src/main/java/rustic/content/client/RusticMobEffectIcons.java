package rustic.content.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import rustic.Rustic;

/**
 * The status-effect HUD / inventory icons, the Phase 17 S17.5 port of the legacy
 * {@code PotionBase.getStatusIconIndex()} override. 1.12.2 bound
 * {@code rustic:textures/gui/potion_icons.png} and drew the effect's
 * {@code setIconIndex(iconU, iconV)} grid cell from it; 1.21 has no per-effect icon index, so the same sheet is
 * painted through {@link IClientMobEffectExtensions}, which lets a mod blit its own texture for both the
 * inventory ({@code EffectRenderingInventoryScreen}) and the HUD ({@code Gui}) effect rows.
 *
 * <p><b>Sheet geometry.</b> {@code potion_icons.png} is a 256x256 sheet whose effect grid is laid out in
 * 18x18 cells anchored to the bottom-left: the legacy {@code setIconIndex(x, y)} cell (x, y) sits at pixel
 * {@code (x * 18, (11 + y) * 18)}. That row-11 origin is simply where the 1.12.2 author painted the eleven
 * Rustic icons (row 0 = ironskin..wither_ward at y=198, row 1 = fullmetal/undying/fire_power at y=216); the
 * magenta cells elsewhere on the sheet are unused padding and are never sampled.
 */
public final class RusticMobEffectIcons {

    /** The legacy status-icon sheet, bound directly by {@code GuiGraphics#blit}. */
    public static final ResourceLocation POTION_ICONS =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/potion_icons.png");
    /** One icon cell is 18x18, matching vanilla's effect-icon size. */
    public static final int TILE = 18;
    /** {@code potion_icons.png} is a 256x256 sheet. */
    public static final int SHEET = 256;
    /** The first icon row's band index on the sheet (see the class javadoc for the layout). */
    public static final int FIRST_ROW_BAND = 11;

    /**
     * The top-left pixel of the legacy {@code setIconIndex(x, y)} cell on {@link #POTION_ICONS}, as
     * {@code {u, v}}. Exposed so the effect-to-tile mapping can be asserted without a render pass.
     */
    public static int[] tileFor(int iconX, int iconY) {
        return new int[] { iconX * TILE, (FIRST_ROW_BAND + iconY) * TILE };
    }

    /** An extension that blits the given {@link #POTION_ICONS} tile for both the inventory and HUD icon slots. */
    public static IClientMobEffectExtensions forTile(int u, int v) {
        return new IconExtension(u, v);
    }

    private record IconExtension(int u, int v) implements IClientMobEffectExtensions {

        @Override
        public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen,
                GuiGraphics guiGraphics, int x, int y, int blitOffset) {
            // Vanilla draws the default sprite at (x, y + 7); match that anchor so the icon lines up with the
            // effect-name text row.
            guiGraphics.blit(POTION_ICONS, x, y + 7, this.u, this.v, TILE, TILE, SHEET, SHEET);
            return true;
        }

        @Override
        public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics guiGraphics,
                int x, int y, float z, float alpha) {
            // The HUD background is a 24x24 plate at (x, y); vanilla centres the 18x18 icon at +3, +3 and
            // applies the expiry-blink alpha, which the caller has already computed for us.
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            guiGraphics.blit(POTION_ICONS, x + 3, y + 3, this.u, this.v, TILE, TILE, SHEET, SHEET);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            return true;
        }
    }

    private RusticMobEffectIcons() {
    }
}
