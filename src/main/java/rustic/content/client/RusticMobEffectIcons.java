package rustic.content.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import rustic.Rustic;

public final class RusticMobEffectIcons {

    public static final ResourceLocation POTION_ICONS =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/potion_icons.png");

    public static final int TILE = 18;

    public static final int SHEET = 256;

    public static final int FIRST_ROW_BAND = 11;

    public static int[] tileFor(int iconX, int iconY) {
        return new int[] { iconX * TILE, (FIRST_ROW_BAND + iconY) * TILE };
    }

    public static IClientMobEffectExtensions forTile(int u, int v) {
        return new IconExtension(u, v);
    }

    private record IconExtension(int u, int v) implements IClientMobEffectExtensions {

        @Override
        public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen,
                GuiGraphics guiGraphics, int x, int y, int blitOffset) {

            guiGraphics.blit(POTION_ICONS, x, y + 7, this.u, this.v, TILE, TILE, SHEET, SHEET);
            return true;
        }

        @Override
        public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics guiGraphics,
                int x, int y, float z, float alpha) {

            guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            guiGraphics.blit(POTION_ICONS, x + 3, y + 3, this.u, this.v, TILE, TILE, SHEET, SHEET);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            return true;
        }
    }

    private RusticMobEffectIcons() {
    }
}
