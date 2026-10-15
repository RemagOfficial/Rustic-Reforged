package rustic.content.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.Rustic;
import rustic.content.blockentity.CondenserBlockEntityBase;
import rustic.content.menu.CondenserMenu;

/**
 * The basic condenser GUI, the 1.21 form of legacy {@code GuiCondenser}. It draws {@code condenser.png}, the water
 * tank as a bottom-up fill from the block-atlas still sprite, the burning flame and the brew-progress bar - all three
 * driven by the four synced furnace {@code DataSlot}s plus the block-update-synced tank on the client BE copy.
 *
 * <p><b>Art status.</b> The background is referenced at its legacy path and only renders once Phase 11 ships the
 * texture; a missing background here is the expected placeholder, not a bug. The fluid fill itself always draws.
 */
public class RusticCondenserScreen extends AbstractContainerScreen<CondenserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/condenser.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    public RusticCondenserScreen(CondenserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);

        CondenserBlockEntityBase condenser = this.menu.getTile();
        if (condenser == null) {
            return;
        }

        drawTank(guiGraphics, condenser.getTank().getFluid(), condenser.getTank().getCapacity(), 133, 27, 16, 32);

        // Flame, scaled off the current fuel's burn length (legacy getBurnLeftScaled(13)).
        if (condenser.isBurning()) {
            int scale = condenser.currentItemBurnTime == 0 ? 200 : condenser.currentItemBurnTime;
            int k = condenser.condenserBurnTime * 13 / scale;
            guiGraphics.blit(TEXTURE, this.leftPos + 67, this.topPos + 46 + 12 - k, 176, 12 - k, 14, k + 1);
        }

        // Brew progress (legacy getBrewProgressScaled(50), drawn 28 tall at (44,29)).
        int l = brewProgress(50);
        if (l > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 44, this.topPos + 29, 176, 14, l, 28);
        }
    }

    /** Legacy {@code getBrewProgressScaled}: {@code brewTime * pixels / totalBrewTime + 1} when both are non-zero. */
    private int brewProgress(int pixels) {
        CondenserBlockEntityBase condenser = this.menu.getTile();
        if (condenser == null || condenser.brewTime == 0 || condenser.totalBrewTime == 0) {
            return 0;
        }
        return condenser.brewTime * pixels / condenser.totalBrewTime + 1;
    }

    private void drawTank(GuiGraphics guiGraphics, FluidStack fluid, int capacity, int x, int y, int width, int height) {
        if (fluid.isEmpty() || capacity <= 0) {
            return;
        }
        int fillHeight = height * fluid.getAmount() / capacity;
        if (fillHeight <= 0) {
            return;
        }
        ResourceLocation still = IClientFluidTypeExtensions.of(fluid.getFluid().getFluidType()).getStillTexture();
        if (still == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        int top = this.topPos + y + (height - fillHeight);
        guiGraphics.blit(this.leftPos + x, top, 0, width, fillHeight, sprite);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTankTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        CondenserBlockEntityBase condenser = this.menu.getTile();
        if (condenser == null) {
            return;
        }
        if (isHovered(mouseX, mouseY, 133, 27, 16, 32)) {
            FluidStack fluid = condenser.getTank().getFluid();
            int capacity = condenser.getTank().getCapacity();
            MutableComponent text;
            if (fluid.isEmpty()) {
                text = Component.translatable("tooltip.rustic.empty")
                        .append(Component.literal(" 0/" + capacity).withStyle(Style.EMPTY.withColor(0xAAAAAA)));
            } else {
                text = fluid.getHoverName().copy();
                text.append(Component.literal(" " + fluid.getAmount() + "/" + capacity)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
            }
            guiGraphics.renderTooltip(this.font, text, mouseX, mouseY);
        }
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + height;
    }
}
