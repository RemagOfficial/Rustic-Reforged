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

/**
 * Shared fluid-surface and stacked-item drawing for the {@code Phase 9} fluid-tank block entity renderers
 * (crushing tub, evaporating basin, liquid barrel). It is the 1.21 replacement for legacy
 * {@code rustic.client.util.FluidClientUtil} plus the per-renderer {@code POSITION_TEX_LMAP_COLOR} quad code that
 * {@code CrushingTubRenderer}/{@code EvaporatingBasinRenderer}/{@code LiquidBarrelRenderer} each duplicated.
 *
 * <p><b>What carries over.</b> Every legacy tank drew a single UP-facing fluid surface quad whose height is
 * {@code amount/capacity} scaled between a base {@code y} and a {@code yScale}, whose horizontal bounds and sprite
 * UV inset were hard-coded per block, and which was coloured by the fluid tint and lit by the block's combined
 * light. Those exact numbers are passed in by each caller; nothing here guesses them.
 *
 * <p><b>What changed.</b> The legacy {@code GlStateManager}/{@code Tessellator} immediate-mode block is replaced by
 * a batched {@link RenderType#translucent()} draw through the BER's {@link MultiBufferSource} - the same path the
 * vanilla chunk renderer uses for glass, with the block atlas bound via {@code BLOCK_SHEET_MIPPED} and the
 * {@code BLOCK} vertex format (position, colour, uv, overlay, light, normal). Source verification of
 * {@code RenderType} ({@code TRANSLUCENT = create("translucent", DefaultVertexFormat.BLOCK, QUADS, ..., 
 * translucentState(RENDERTYPE_TRANSLUCENT_SHADER))} with {@code BLOCK_SHEET_MIPPED} + {@code TRANSLUCENT_TARGET})
 * and of {@code LevelRenderer} (the block-entity pass {@code endBatch()}es the translucent layer) shows this is the
 * supported route. The previous hand-rolled {@code Tesselator.begin(...)} + {@code BufferUploader.drawWithShader}
 * immediate block is falsified: it produced zero visible output across four consecutive in-game rounds regardless of
 * depthMask state, with the tanks provably filled and no render exceptions logged - so it was dropped entirely.
 * The {@code getCombinedLight(pos, luminosity)} call has no 1.21 BER equivalent, so the fluid reuses the
 * {@code packedLight} the render pass already hands the block - identical for the juices/oils/boozes these tanks
 * actually hold, all of which have zero luminosity.
 */
public final class FluidTankRenderUtil {

    private FluidTankRenderUtil() {
    }

    /** Resolve the fluid's still texture into a block-atlas sprite, or {@code null} when there is nothing to draw. */
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

    /**
     * Draw the fluid's top surface quad at {@code yBase + yScale * amount/capacity} across
     * {@code [minX,maxX] x [minZ,maxZ]} (block-local coordinates), using the still sprite's UVs inset toward the
     * centre by {@code uvInset}, coloured by the fluid tint and lit by {@code packedLight}. Does nothing for an
     * empty tank or a fluid with no still sprite. The quad is batched into {@code bufferSource}'s
     * {@link RenderType#translucent()} layer - the vanilla glass path, flushed by the block-entity pass - because
     * the hand-rolled immediate {@code drawWithShader} variant of this method rendered nothing in four consecutive
     * in-game rounds despite filled tanks and clean logs.
     */
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

        // BLOCK-format quad into the batched translucent layer (atlas bound by RenderType.translucent()'s
        // BLOCK_SHEET_MIPPED shard). Winding runs west-edge north-to-south then back east-side south-to-north so
        // the cross product points +Y: translucent() culls by default, and the legacy surface only ever showed its
        // top face to a viewer above it, which an up-facing normal preserves (the old disableCull double-sidedness
        // is not worth reproducing - seen from below the vessel floor hides the surface anyway).
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

    /**
     * The legacy fluid-surface height: the top quad sits at {@code yBase} when the tank is empty and rises linearly
     * to {@code yBase + yScale} when full. Extracted so the per-block {@code amount/capacity} maths is unit-testable
     * without booting the client; not clamped, matching the legacy behaviour for an over-full tank.
     */
    public static float surfaceHeight(int amount, int capacity, float yBase, float yScale) {
        return yBase + yScale * (amount / (float) capacity);
    }

    /**
     * Stack one flat item layer per {@code itemsPerStack} items ({@code ceil(count/itemsPerStack)} layers, legacy's
     * tub=8 / basin=16), each raised by {@code 0.062 + i*0.0625} and yaw-rotated by an RNG seeded from
     * {@code seed}. The seed is the block position rather than legacy's world seed (there is no
     * {@code Level#getSeed()} in 1.21); it keeps every layer's rotation stable across frames, exactly as the legacy
     * re-seeded {@code Random} achieved, while letting distinct tanks differ instead of all sharing one arrangement.
     * The GL translate/rotate/scale chain is reproduced one-for-one as {@link PoseStack} operations.
     *
     * <p>Round-10: the items sit at legacy's fixed {@code 0.062 + i*0.0625} INSIDE the fluid (they are NOT lifted
     * above the surface - the round-9 lift was reverted as wrong). The caller must flush this item geometry before
     * drawing the fluid (see {@link #renderFluidSurface}) so the translucent surface blends over the items instead of
     * depth-culling them, matching the legacy renderer which drew items first then the fluid quad.
     */
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
