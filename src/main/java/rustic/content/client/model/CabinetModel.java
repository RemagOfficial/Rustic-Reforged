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

/**
 * The 1.21 replacement for the legacy {@code ModelCabinet} / {@code ModelCabinetDouble} {@code ModelBase}
 * pair (and the four instances the legacy {@code CabinetRenderer} kept: simple, simple-mirror, double,
 * double-mirror).
 *
 * <p><b>Coordinate space (round 16, done right).</b> An earlier pass re-authored these boxes into block-corner
 * space (0..16, front +Z) and paired them with a 180-degree X rotation in the BER. That split the model and the
 * transform away from the original: the geometry no longer matched the {@code scale(1,-1,-1)} mirror the legacy
 * renderer used, so the side panels' faces sampled the wrong part of the sheet and needed hand-tuned {@code texOffs}
 * (and a front-lip patch) to compensate. We now reproduce the legacy {@code ModelRenderer} geometry <em>verbatim</em>
 * - centred/Y-down model space, the original rotation points, the original {@code texOffs} and box dimensions, no uv
 * mirror - and let {@code CabinetRenderer} restore the legacy transform. Because {@code ModelPart.Cube} derives a
 * face's UV from {@code texOffs} plus box dimensions using the same algorithm 1.12 {@code ModelRenderer.addBox} did,
 * identical inputs give identical pixels.
 *
 * <p>Four {@link LayerDefinition}s are baked - single/single-mirror/double/double-mirror - because the mirror flag
 * moves the door hinge (and its handle) to the opposite vertical edge, exactly as the legacy
 * {@code ModelCabinet(boolean mirror)} constructor chose the door/handle box signs. The handle is a child of the door
 * so it inherits the swing, matching the legacy {@code handle.rotateAngleY = door.rotateAngleY} line in
 * {@code renderAll}.
 *
 * <p><b>Texture choice.</b> The legacy renderer had two cabinet sheets: {@code cabinet.png}, a grayscale tint MASK
 * meant to be multiplied by the colour of the wood the cabinet was crafted from (the {@code ItemColorCache} path),
 * and {@code cabinet_color.png}, a pre-coloured brown-plank sheet. The ported {@code CabinetBlockEntity} stores no
 * material stack (its recipe carries none), so there is no wood colour to tint the mask with and drawing
 * {@code cabinet.png} directly rendered the cabinet flat light gray (the reported in-game defect). We therefore bind
 * the {@code _color} sheets, which carry the intended wooden look with no tint pass required. Wiring real per-wood
 * tinting back would mean storing the crafting wood in the BE and re-adding the colour handler - deliberately left
 * out of scope for this phase.
 */
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

    /** Bakes the four layers through the supplied baker (a {@code Context::bakeLayer} or {@code EntityModelSet::bakeLayer}). */
    public CabinetModel(Function<ModelLayerLocation, ModelPart> baker) {
        this.single = new Holder(baker.apply(SINGLE));
        this.singleMirror = new Holder(baker.apply(SINGLE_MIRROR));
        this.doubleHolder = new Holder(baker.apply(DOUBLE));
        this.doubleMirror = new Holder(baker.apply(DOUBLE_MIRROR));
    }

    /**
     * Draws one cabinet. {@code openness} is the interpolated lid openness ({@code 0} closed, {@code 1} open); the
     * legacy cubic easing ramp {@code f = 1-(1-open)^3} is reproduced so the door accelerates off the latch. The door
     * swings about the vertical (Y) axis via its own {@code yRot} (the handle, being its child, follows), exactly as
     * the legacy renderer set {@code door.rotateAngleY}. The render type is {@link RenderType#entityCutoutNoCull}
     * because the BER applies the legacy {@code scale(1,-1,-1)} mirror, which reverses triangle winding - culling the
     * back faces would otherwise delete half the shell.
     */
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
        // The handle is a child of the door, so rendering the door draws the handle with the same swing.
        holder.door.render(poseStack, consumer, packedLight, packedOverlay);
    }

    /**
     * The single/double cabinet body layer for the given mirror and height variant, a 1:1 transcription of the legacy
     * {@code ModelCabinet} / {@code ModelCabinetDouble} constructors. Each part's rotationPoint becomes a
     * {@link PartPose} offset and the {@code addBox} arguments/texOffsets are copied literally from the source.
     */
    public static LayerDefinition createBodyLayer(boolean mirror, boolean doubleHeight) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int texHeight = doubleHeight ? 128 : 64;
        int panelHeight = doubleHeight ? 30 : 14;
        // bottom: texOffs(0,0), addBox(0,0,0,16,1,16), rotationPoint(-8,23,-8).
        root.addOrReplaceChild("bottom",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 16.0F),
                PartPose.offset(-8.0F, 23.0F, -8.0F));
        // top: texOffs(64,0), same 16x1x16 slab; the double's top rides up into the second block (RP y=-8 vs 8).
        float topPoseY = doubleHeight ? -8.0F : 8.0F;
        root.addOrReplaceChild("top",
                CubeListBuilder.create().texOffs(64, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 16.0F),
                PartPose.offset(-8.0F, topPoseY, -8.0F));
        // back: texOffs(0,17), 16 x panelHeight x 1, RP(-8, 9 single / -7 double, 7).
        float backPoseY = doubleHeight ? -7.0F : 9.0F;
        root.addOrReplaceChild("back",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(0.0F, 0.0F, 0.0F, 16.0F, (float) panelHeight, 1.0F),
                PartPose.offset(-8.0F, backPoseY, 7.0F));
        // sides: right texOffs(0, sideTexV) at RP(-8, sideY, -8); left texOffs(32, sideTexV) at RP(7, sideY, -8).
        // sideTexV = 32 (single, cabinet_color rows 47..60) / 48 (double, cabinet_double_color rows 63..92).
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
        // door + handle: the legacy hinge is the door's rotationPoint at x = -7 (normal) / 7 (mirror), z = -6.5. The
        // door box hangs off that pivot; the handle shares the exact same rotationPoint in legacy, so here it is a
        // CHILD of the door at PartPose.ZERO. Setting door.yRot swings the door and handle together.
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

    /**
     * The door swing for a given openness, reproducing the legacy cubic ramp: {@code eased = 1-(1-openness)^3} (so the
     * door lingers shut then snaps open), scaled to a quarter turn. The sign is the legacy renderer's verbatim
     * {@code (mirror ? -(f*PI/2) : (f*PI/2))}: the mirror variant, whose hinge sits on the opposite edge, swings the
     * other way. There is no compensating flip here because the BER now uses the real {@code scale(1,-1,-1)} transform
     * (and {@code ModelPart.yRot} uses the same sign convention as 1.12 {@code ModelRenderer.rotateAngleY}).
     */
    public static float doorAngle(float openness, boolean mirror) {
        float eased = 1.0F - openness;
        eased = 1.0F - eased * eased * eased;
        return (mirror ? -1.0F : 1.0F) * eased * (float) (Math.PI / 2.0);
    }

    /** Pulls the six rendered parts out of a baked root (the handle rides under the door and needs no field). */
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
