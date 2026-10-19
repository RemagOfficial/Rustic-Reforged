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
import rustic.content.blockentity.BrewingBarrelBlockEntity;
import rustic.content.fluids.RusticBooze;
import rustic.content.menu.BrewingBarrelMenu;

public class RusticBrewingBarrelScreen extends AbstractContainerScreen<BrewingBarrelMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/gui/brewing_barrel.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    public RusticBrewingBarrelScreen(BrewingBarrelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;

        this.inventoryLabelY = this.imageHeight + 1000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);

        BrewingBarrelBlockEntity barrel = this.menu.getTile();
        if (barrel == null) {
            return;
        }

        drawTank(guiGraphics, barrel.getInputTank().getFluid(), BrewingBarrelBlockEntity.INPUT_CAPACITY, 62, 27, 16, 32);
        drawTank(guiGraphics, barrel.getOutputTank().getFluid(), BrewingBarrelBlockEntity.OUTPUT_CAPACITY, 116, 27, 16, 32);
        drawTank(guiGraphics, barrel.getAuxiliaryTank().getFluid(), BrewingBarrelBlockEntity.AUX_CAPACITY, 26, 35, 16, 16);

        if (this.menu.isMachineSlotEmpty(BrewingBarrelBlockEntity.INPUT_IN_SLOT)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 7, 176, 54, 16, 16);
        }
        if (this.menu.isMachineSlotEmpty(BrewingBarrelBlockEntity.OUTPUT_IN_SLOT)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 116, this.topPos + 7, 176, 70, 16, 16);
        }
        if (this.menu.isMachineSlotEmpty(BrewingBarrelBlockEntity.AUX_IN_SLOT)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 15, 176, 70, 16, 16);
        }

        int brewTime = barrel.getBrewTime();
        int maxBrew = barrel.getMaxBrewTime();
        if (brewTime > 0 && maxBrew > 0) {
            int bubbles = (int) ((brewTime / (float) maxBrew) * 5600) % 28;
            guiGraphics.blit(TEXTURE, this.leftPos + 139, this.topPos + 29 + 28 - bubbles,
                    176, 28 - bubbles, 11, bubbles);
            int progress = brewTime * 24 / maxBrew + 1;
            guiGraphics.blit(TEXTURE, this.leftPos + 85, this.topPos + 35, 176, 28, progress, 16);
            int secondary = brewTime * 10 / maxBrew;
            guiGraphics.blit(TEXTURE, this.leftPos + 45, this.topPos + 38, 176, 44, secondary, 10);
        }
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

        renderTankTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderTankTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        BrewingBarrelBlockEntity barrel = this.menu.getTile();
        if (barrel == null) {
            return;
        }
        if (isHovered(mouseX, mouseY, 62, 27, 16, 32)) {
            guiGraphics.renderTooltip(this.font, fluidTooltip(barrel.getInputTank().getFluid(),
                    BrewingBarrelBlockEntity.INPUT_CAPACITY), mouseX, mouseY);
        } else if (isHovered(mouseX, mouseY, 116, 27, 16, 32)) {
            guiGraphics.renderTooltip(this.font, fluidTooltip(barrel.getOutputTank().getFluid(),
                    BrewingBarrelBlockEntity.OUTPUT_CAPACITY), mouseX, mouseY);
        } else if (isHovered(mouseX, mouseY, 26, 35, 16, 16)) {
            guiGraphics.renderTooltip(this.font, fluidTooltip(barrel.getAuxiliaryTank().getFluid(),
                    BrewingBarrelBlockEntity.AUX_CAPACITY), mouseX, mouseY);
        }
    }

    private Component fluidTooltip(FluidStack fluid, int capacity) {
        if (fluid.isEmpty()) {
            return Component.translatable("tooltip.rustic.empty")
                    .append(Component.literal(" 0/" + capacity).withStyle(Style.EMPTY.withColor(0xAAAAAA)));
        }
        MutableComponent name = fluid.getHoverName().copy();
        if (fluid.getFluid() instanceof RusticBooze booze) {
            float quality = booze.getQuality(fluid);
            int color = RusticBooze.getQualityLabelColor(quality);
            name.append(Component.translatable("tooltip.rustic.quality")
                    .append(Component.literal(String.format(" %.0f%%", quality * 100))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)))));
        }
        name.append(Component.literal(" " + fluid.getAmount() + "/" + capacity)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
        return name;
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + height;
    }
}
