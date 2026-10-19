package rustic.content.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import rustic.Rustic;

public class RusticChestScreen extends AbstractContainerScreen<ChestMenu> {

    public static final ResourceLocation TEXTURE_27 =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/generic_27.png");

    public static final ResourceLocation TEXTURE_54 =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/generic_54.png");

    private final ResourceLocation texture;

    public RusticChestScreen(ChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        boolean sixRows = menu.getRowCount() >= 6;
        this.texture = sixRows ? TEXTURE_54 : TEXTURE_27;

        this.imageHeight = sixRows ? 222 : 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.blit(this.texture, left, top, 0, 0, this.imageWidth, this.imageHeight);
    }
}
