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

/**
 * A single, immediate-mode, full-screen textured quad composited with straight alpha.
 *
 * <p>Both first-person screen tints in this mod (the fullmetal metallic sheen and the fluid-submersion overlay)
 * need exactly this, and both previously reached for {@link GuiGraphics#blit(ResourceLocation, int, int, int, int,
 * float, float, int, int, int, int)} which routes to the {@code POSITION_TEX} {@code innerBlit} - a shader path that
 * carries no per-vertex colour and (as observed in-game) composited the quad at the texture's own alpha regardless of
 * {@link GuiGraphics#setColor}, so the tint came out fully opaque. {@code POSITION_TEX_COLOR} with the alpha baked
 * into each vertex is the primitive that actually respects transparency, and is the same one vanilla's
 * {@code ScreenEffectRenderer#renderFluid} uses for the water overlay.
 *
 * <p>Drawn immediately (its own {@link BufferUploader#drawWithShader} call) rather than through the {@code GuiGraphics}
 * batch, so the blend/depth state is only toggled for the duration of this one quad and cannot be deferred to a flush
 * that happens after the caller has restored state.
 */
final class GuiScreenOverlay {

    private GuiScreenOverlay() {
    }

    /**
     * Paint {@code texture} stretched across the whole GUI viewport, multiplied by {@code (r, g, b, alpha)}. The
     * caller's pending {@link GuiGraphics} geometry is flushed first so this quad lands on top of it in draw order.
     */
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
