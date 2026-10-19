package rustic.content.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public final class FluidTankRenderUtil {

    private FluidTankRenderUtil() {
    }

    public static TextureAtlasSprite stillSprite(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return null;
        }
        ResourceLocation still = IClientFluidTypeExtensions.of(fluid.getFluid().getFluidType()).getStillTexture();
        if (still == null) {
            return null;
        }
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
    }

    public static void renderFluidSurface(FluidStack fluid, int capacity,
                                          float yBase, float yScale,
                                          float minX, float maxX, float minZ, float maxZ, float uvInset,
                                          PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (fluid.isEmpty() || capacity <= 0) {
            return;
        }
        TextureAtlasSprite sprite = stillSprite(fluid);
        if (sprite == null) {
            return;
        }
        int color = IClientFluidTypeExtensions.of(fluid.getFluid().getFluidType()).getTintColor(fluid);
        float y = surfaceHeight(fluid.getAmount(), capacity, yBase, yScale);

        float diffU = sprite.getU1() - sprite.getU0();
        float diffV = sprite.getV1() - sprite.getV0();
        float minU = sprite.getU0() + diffU * uvInset;
        float maxU = sprite.getU1() - diffU * uvInset;
        float minV = sprite.getV0() + diffV * uvInset;
        float maxV = sprite.getV1() - diffV * uvInset;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();
        consumer.addVertex(matrix, minX, y, minZ).setColor(color).setUv(minU, minV)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, minX, y, maxZ).setColor(color).setUv(minU, maxV)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, maxX, y, maxZ).setColor(color).setUv(maxU, maxV)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix, maxX, y, minZ).setColor(color).setUv(maxU, minV)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 1.0F, 0.0F);
    }

    public static float surfaceHeight(int amount, int capacity, float yBase, float yScale) {
        return yBase + yScale * (amount / (float) capacity);
    }

    public static void renderStackedItems(ItemStack stack, int itemsPerStack, long seed,
                                          ItemRenderer itemRenderer, PoseStack poseStack, MultiBufferSource bufferSource,
                                          Level level, int packedLight) {
        if (stack.isEmpty() || itemsPerStack <= 0 || level == null) {
            return;
        }
        int layers = (int) Math.ceil(stack.getCount() / (float) itemsPerStack);
        RandomSource random = RandomSource.create(seed);
        for (int i = 0; i < layers; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.062D + i * 0.0625D, 0.0D);
            poseStack.translate(0.5D, 0.0D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.translate(-0.5D, 0.0D, -0.5D);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(0.5D, -0.1875D, 0.0D);
            poseStack.translate(0.0D, 0.6875D, 0.0D);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, bufferSource, level, 0);
            poseStack.popPose();
        }
    }
}
