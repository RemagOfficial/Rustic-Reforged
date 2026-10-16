package rustic.content.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import rustic.Rustic;
import rustic.content.client.IronSkinRenderPolicy;
import rustic.content.potions.RusticMobEffects;

/**
 * The 1.21 reimplementation of the 1.12.2 {@code LayerIronSkin} body paint: a translucent metal
 * sheet drawn over a player who is under the iron-skin or fullmetal effect.
 *
 * <p>The legacy class hand-rolled fixed-function GL - {@code GL_COMBINE} multitexture, display-list
 * swapping through a reflective {@code ModelRendererOverrider}, a bespoke {@code VertexFormatMultiTex}
 * and {@code glPolygonOffset} to defeat z-fighting - none of which survive on the buffered render
 * pipeline. The modern equivalent is far smaller: because the parent {@link PlayerRenderer} has
 * already posed its model by the time a layer runs, we simply re-render that same model to a
 * {@link RenderType#entityTranslucent} buffer textured with the metal sheet, tinted white with an
 * alpha equal to the effect's opacity. The overlay is scaled a hair above 1 to sit proud of the skin
 * mesh, which is the render-thread replacement for the old polygon offset.
 *
 * <p>All decision logic (opacity ladder, skip conditions, blacklist) lives in
 * {@link IronSkinRenderPolicy} so it can be unit-tested without a GL context.
 */
@OnlyIn(Dist.CLIENT)
public class IronSkinRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /**
     * The player model is always laid out on a 64x32 sheet ({@code ModelLayers.PLAYER} /
     * {@code PLAYER_SLIM}), so unlike the legacy code - which picked a metal size from the model's
     * runtime {@code textureWidth/Height} - the player's metal texture is a fixed sprite.
     */
    private static final ResourceLocation METAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/entity/layer/fullmetal_64_32.png");

    /** The legacy {@code FORCE_IRONSKIN_RENDER()} debug switch; kept off in production. */
    private static final boolean FORCE_RENDER = false;

    public IronSkinRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        MobEffectInstance ironSkin = player.getEffect(RusticMobEffects.IRON_SKIN);
        boolean hasIronSkin = ironSkin != null;
        boolean hasFullmetal = player.hasEffect(RusticMobEffects.FULLMETAL);

        if (!IronSkinRenderPolicy.shouldOverlay(player.isInvisible(), hasIronSkin, hasFullmetal, FORCE_RENDER)) {
            return;
        }

        int amplifier = hasIronSkin ? ironSkin.getAmplifier() : -1;
        float opacity = IronSkinRenderPolicy.opacityFor(amplifier, hasFullmetal, hasIronSkin);
        // Vertex colour is packed ARGB32; the RGB channels stay white and only alpha carries opacity,
        // matching the legacy GlStateManager.color(1F, 1F, 1F, opacity).
        int color = FastColor.ARGB32.color((int) (opacity * 255.0F), 255, 255, 255);

        poseStack.pushPose();
        // Nudge the sheet just off the skin mesh so the two do not z-fight - the buffered-pipeline
        // stand-in for the old glPolygonOffset(1.0, 1.0).
        poseStack.scale(1.001F, 1.001F, 1.001F);
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityTranslucent(METAL_TEXTURE));
        this.getParentModel().renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();
    }
}
