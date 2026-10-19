package rustic.content.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import rustic.Rustic;

@OnlyIn(Dist.CLIENT)
public final class CabinetModel {

    public static final ModelLayerLocation SINGLE = layer("cabinet");
    public static final ModelLayerLocation SINGLE_MIRROR = layer("cabinet_mirror");
    public static final ModelLayerLocation DOUBLE = layer("cabinet_double");
    public static final ModelLayerLocation DOUBLE_MIRROR = layer("cabinet_double_mirror");

    private static final ResourceLocation TEXTURE_NORMAL = texture("cabinet_color");
    private static final ResourceLocation TEXTURE_DOUBLE = texture("cabinet_double_color");

    private final Holder single;
    private final Holder singleMirror;
    private final Holder doubleHolder;
    private final Holder doubleMirror;

    public CabinetModel(Function<ModelLayerLocation, ModelPart> baker) {
        this.single = new Holder(baker.apply(SINGLE));
        this.singleMirror = new Holder(baker.apply(SINGLE_MIRROR));
        this.doubleHolder = new Holder(baker.apply(DOUBLE));
        this.doubleMirror = new Holder(baker.apply(DOUBLE_MIRROR));
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                       boolean mirror, boolean doubleHeight, float openness) {
        Holder holder = doubleHeight ? (mirror ? doubleMirror : doubleHolder) : (mirror ? singleMirror : single);
        ResourceLocation texture = doubleHeight ? TEXTURE_DOUBLE : TEXTURE_NORMAL;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        holder.door.yRot = doorAngle(openness, mirror);
        holder.bottom.render(poseStack, consumer, packedLight, packedOverlay);
        holder.top.render(poseStack, consumer, packedLight, packedOverlay);
        holder.back.render(poseStack, consumer, packedLight, packedOverlay);
        holder.right.render(poseStack, consumer, packedLight, packedOverlay);
        holder.left.render(poseStack, consumer, packedLight, packedOverlay);

        holder.door.render(poseStack, consumer, packedLight, packedOverlay);
    }

    public static LayerDefinition createBodyLayer(boolean mirror, boolean doubleHeight) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int texHeight = doubleHeight ? 128 : 64;
        int panelHeight = doubleHeight ? 30 : 14;

        root.addOrReplaceChild("bottom",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 16.0F),
                PartPose.offset(-8.0F, 23.0F, -8.0F));

        float topPoseY = doubleHeight ? -8.0F : 8.0F;
        root.addOrReplaceChild("top",
                CubeListBuilder.create().texOffs(64, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 16.0F),
                PartPose.offset(-8.0F, topPoseY, -8.0F));

        float backPoseY = doubleHeight ? -7.0F : 9.0F;
        root.addOrReplaceChild("back",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(0.0F, 0.0F, 0.0F, 16.0F, (float) panelHeight, 1.0F),
                PartPose.offset(-8.0F, backPoseY, 7.0F));

        int sideTexV = doubleHeight ? 48 : 32;
        float sidePoseY = doubleHeight ? -7.0F : 9.0F;
        root.addOrReplaceChild("right",
                CubeListBuilder.create().texOffs(0, sideTexV)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, (float) panelHeight, 15.0F),
                PartPose.offset(-8.0F, sidePoseY, -8.0F));
        root.addOrReplaceChild("left",
                CubeListBuilder.create().texOffs(32, sideTexV)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, (float) panelHeight, 15.0F),
                PartPose.offset(7.0F, sidePoseY, -8.0F));

        int doorTexU = doubleHeight ? 0 : 64;
        int doorTexV = doubleHeight ? 93 : 17;
        float doorPoseX = mirror ? 7.0F : -7.0F;
        float doorPoseY = doubleHeight ? 8.0F : 16.0F;
        float doorBoxX = mirror ? -14.0F : 0.0F;
        float doorBoxY = doubleHeight ? -15.0F : -7.0F;
        PartDefinition door = root.addOrReplaceChild("door",
                CubeListBuilder.create().texOffs(doorTexU, doorTexV)
                        .addBox(doorBoxX, doorBoxY, -0.5F, 14.0F, (float) panelHeight, 1.0F),
                PartPose.offset(doorPoseX, doorPoseY, -6.5F));
        int handleTexU = doubleHeight ? 0 : 94;
        int handleTexV = doubleHeight ? 124 : 17;
        float handleBoxX = mirror ? -13.0F : 12.0F;
        door.addOrReplaceChild("handle",
                CubeListBuilder.create().texOffs(handleTexU, handleTexV)
                        .addBox(handleBoxX, -1.0F, -1.5F, 1.0F, 2.0F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, texHeight);
    }

    public static float doorAngle(float openness, boolean mirror) {
        float eased = 1.0F - openness;
        eased = 1.0F - eased * eased * eased;
        return (mirror ? -1.0F : 1.0F) * eased * (float) (Math.PI / 2.0);
    }

    private record Holder(ModelPart bottom, ModelPart top, ModelPart back, ModelPart right, ModelPart left, ModelPart door) {
        Holder(ModelPart root) {
            this(root.getChild("bottom"), root.getChild("top"), root.getChild("back"),
                    root.getChild("right"), root.getChild("left"), root.getChild("door"));
        }
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Rustic.MODID, name), "main");
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "textures/models/" + name + ".png");
    }
}
