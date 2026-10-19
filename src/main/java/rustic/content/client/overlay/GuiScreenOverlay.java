package rustic.content.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

final class GuiScreenOverlay {

    private GuiScreenOverlay() {
    }

    static void draw(GuiGraphics guiGraphics, ResourceLocation texture, float r, float g, float b, float alpha) {
        guiGraphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        PoseStack poseStack = guiGraphics.pose();
        Matrix4f matrix = poseStack.last().pose();
        float width = guiGraphics.guiWidth();
        float height = guiGraphics.guiHeight();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, 0.0F, height, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, width, height, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, alpha);
        buffer.addVertex(matrix, width, 0.0F, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, alpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
