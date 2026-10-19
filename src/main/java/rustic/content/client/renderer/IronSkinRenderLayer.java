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

@OnlyIn(Dist.CLIENT)
public class IronSkinRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation METAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/entity/layer/fullmetal_64_32.png");

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

        int color = FastColor.ARGB32.color((int) (opacity * 255.0F), 255, 255, 255);

        poseStack.pushPose();

        poseStack.scale(1.001F, 1.001F, 1.001F);
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityTranslucent(METAL_TEXTURE));
        this.getParentModel().renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();
    }
}
