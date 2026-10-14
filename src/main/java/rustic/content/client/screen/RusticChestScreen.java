package rustic.content.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import rustic.Rustic;

/**
 * The chest-style screen shared by every Rustic container that drew 1.12.2's generic background.
 *
 * <p>{@code GuiBarrel}, {@code GuiVase} and {@code GuiCabinet} were three copies of the same class:
 * {@code WIDTH = 176}, {@code HEIGHT = 166}, {@code guiContainer = textures/gui/generic_27.png}, three
 * rows of nine over the player inventory. {@code ContainerCabinetDouble} was the same file at six rows
 * ({@code generic_54.png}, {@code HEIGHT = 222}). One parameterised screen reproduces all four because
 * the {@link ChestMenu} they open already lays the slots out identically - the only things that differ
 * are the background texture and the window height, both handed in by the menu that opens this.
 */
public class RusticChestScreen extends AbstractContainerScreen<ChestMenu> {

    /** The 3-row background the legacy single barrel/vase/cabinet GUIs used. */
    public static final ResourceLocation TEXTURE_27 =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/generic_27.png");
    /** The 6-row background the legacy double-cabinet GUI used. */
    public static final ResourceLocation TEXTURE_54 =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/generic_54.png");

    private final ResourceLocation texture;

    public RusticChestScreen(ChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // The row count the ChestMenu already carries decides both the panel height and which generic
        // background to draw, so one screen class covers the 3-row barrel/vase/cabinet and the 6-row
        // double cabinet exactly as the four near-identical legacy Gui classes did.
        boolean sixRows = menu.getRowCount() >= 6;
        this.texture = sixRows ? TEXTURE_54 : TEXTURE_27;
        // imageWidth stays 176. imageHeight is set from the caller's panel rather than left at the base
        // default, and the "Inventory" label row is measured from that height (the base computed it
        // against the old 166 before this line ran).
        this.imageHeight = sixRows ? 222 : 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        // Blit the whole background panel from the top-left corner of the texture, exactly the legacy
        // drawTexturedModalRect(left, top, 0, 0, imageWidth, imageHeight) this replaces.
        guiGraphics.blit(this.texture, left, top, 0, 0, this.imageWidth, this.imageHeight);
    }
}
