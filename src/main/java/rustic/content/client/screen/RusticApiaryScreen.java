package rustic.content.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import rustic.Rustic;
import rustic.content.menu.ApiaryMenu;

/**
 * The apiary GUI, the 1.21 form of legacy {@code GuiApiary}: the 176x166 {@code apiary.png} panel, the two
 * titles (machine name at 8,6 and "Inventory" at the player-grid header) and nothing else - legacy drew no
 * progress bars or counters, so there is deliberately no extra rendering here. The slot contents sync through
 * the menu itself; the timers the screen never shows stay server-side.
 */
public class RusticApiaryScreen extends AbstractContainerScreen<ApiaryMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/apiary.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    public RusticApiaryScreen(ApiaryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Legacy drawScreen painted the darkening backdrop first, exactly what renderBackground does here.
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Legacy drawGuiContainerForegroundLayer: the apiary title at (8,6) and the inventory label at the
        // player-grid header, both in the vanilla 0x404040 label colour.
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX,
                this.inventoryLabelY, 0x404040, false);
    }
}
