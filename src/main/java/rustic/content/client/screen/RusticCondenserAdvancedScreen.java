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
import rustic.content.menu.CondenserAdvancedMenu;

public class RusticCondenserAdvancedScreen extends AbstractContainerScreen<CondenserAdvancedMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/condenser_advanced.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    public RusticCondenserAdvancedScreen(CondenserAdvancedMenu menu, Inventory playerInventory, Component title) {
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

        if (condenser.isBurning()) {
            int scale = condenser.currentItemBurnTime == 0 ? 200 : condenser.currentItemBurnTime;
            int k = condenser.condenserBurnTime * 13 / scale;
            guiGraphics.blit(TEXTURE, this.leftPos + 67, this.topPos + 46 + 12 - k, 176, 12 - k, 14, k + 1);
        }

        int l = brewProgress(50);
        if (l > 0) {

            guiGraphics.blit(TEXTURE, this.leftPos + 44, this.topPos + 17, 176, 14, l, 53);
        }
    }

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
