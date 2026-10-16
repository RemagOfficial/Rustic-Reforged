package rustic.content.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import rustic.content.blockentity.CabinetBlockEntity;
import rustic.content.blocks.CabinetBlock;
import rustic.content.client.model.CabinetModel;

/**
 * The 1.21 {@link BlockEntityRenderer} that replaces the legacy {@code CabinetRenderer}
 * ({@code TileEntitySpecialRenderer}). It draws the bottom half of a cabinet (or a whole vertical double) as the
 * {@link CabinetModel} body whose door swings open by the interpolated lid angle, exactly as the legacy renderer did
 * - the top half is skipped because the bottom draws the full-height double model.
 *
 * <p>The transform is a 1:1 reproduction of the legacy TESR: the BER PoseStack is at the block corner, so legacy
 * {@code translate(x, y+1, z+1)} becomes {@code translate(0, 1, 1)}, followed by the {@code scale(1,-1,-1)} mirror,
 * the facing rotation about the block centre, and the {@code translate(0.5, -0.5, 0.5)} the legacy renderer applied
 * before {@code renderAll}. The model itself (see {@link CabinetModel}) is authored in the same centred/Y-down space
 * the legacy {@code ModelRenderer}s used, so geometry and transform stay coupled exactly as the original. The material
 * tint branch of the legacy renderer is dead code against the ported block entity (no material is stored), so the model
 * always draws untinted.
 *
 * <p>Openness comes from {@link CabinetBlockEntity#getOpenNess(float)} (the {@code LidBlockEntity} contract, driven
 * client-side by the lid ticker). For a double, the max of the two halves is used so opening either partner swings
 * the single bottom-rendered door.
 */
@OnlyIn(Dist.CLIENT)
public class CabinetRenderer implements BlockEntityRenderer<CabinetBlockEntity> {

    private final CabinetModel model;

    public CabinetRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new CabinetModel(context::bakeLayer);
    }

    @Override
    public void render(CabinetBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        BlockState state = blockEntity.getBlockState();
        // The upper half never draws: the lower half renders the whole double. The item dummy has TOP = false.
        if (state.getValue(CabinetBlock.TOP)) {
            return;
        }

        boolean doubleHeight = false;
        float openness = blockEntity.getOpenNess(partialTick);
        if (level != null) {
            BlockPos pos = blockEntity.getBlockPos();
            BlockState above = level.getBlockState(pos.above());
            if (above.is(state.getBlock()) && above.getValue(CabinetBlock.TOP)) {
                doubleHeight = true;
                // Opening either half of a double should swing the one bottom-rendered door: take the higher openness.
                if (level.getBlockEntity(pos.above()) instanceof CabinetBlockEntity partner) {
                    openness = Math.max(openness, partner.getOpenNess(partialTick));
                }
            }
        }

        boolean mirror = state.getValue(CabinetBlock.MIRROR);
        var facing = state.getValue(CabinetBlock.FACING);

        poseStack.pushPose();
        // Faithful 1:1 reproduction of the legacy 1.12.2 CabinetRenderer world transform. The BER PoseStack already
        // sits at the block's corner, so the legacy `translate(x, y+1, z+1)` becomes the block-local
        // `translate(0, 1, 1)`. The original model (which CabinetModel now reproduces verbatim) was authored in
        // centred/Y-down space and rendered under `scale(1,-1,-1)` - a true mirror (determinant -1) that reverses
        // triangle winding, which is why CabinetModel renders with entityCutoutNoCull. The facing switch
        // (SOUTH 0 / WEST 90 / NORTH 180 / EAST 270) is reproduced POSITIVELY, not with vanilla's negation. Verified
        // against the engine source: `Direction.toYRot()` is `(data2d & 3) * 90` over the S-W-N-E order, so it already
        // EQUALS legacy's switch value. Vanilla `ChestRenderer` writes `Axis.YP.rotationDegrees(-toYRot())` because a
        // chest model is authored un-flipped; copying that negation here rotates the cabinet the wrong way about Y
        // under the `scale(1,-1,-1)` mirror, so WEST/EAST cabinets faced their opposite and the hinged door edge (the
        // mirror flag is positional, not a UV mirror) landed on the wrong side - the reported side/edge defect.
        // The sandwich is about the block centre exactly as legacy, and the legacy `translate(0.5, -0.5, 0.5)`
        // before renderAll is kept. The door swing itself is
        // a per-part yRot set inside CabinetModel (legacy set door.rotateAngleY, not a world rotation).
        poseStack.translate(0.0F, 1.0F, 1.0F);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        poseStack.translate(0.5F, -0.5F, 0.5F);
        // FULL_BRIGHT: entityCutout lights purely from the lightmap (no directional face shading) and the block-entity
        // packedLight resolved dark for this furniture, so the correctly-coloured wood always reads. Drop back to
        // packedLight here if it looks unnaturally flat next to shadowed blocks.
        model.render(poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay, mirror, doubleHeight, openness);
        poseStack.popPose();
    }

    /**
     * A single cabinet occupies its own cell; the lower half of a double must not be culled while its door (and the
     * upper cell it renders into) is in view, so its box grows to the two blocks.
     */
    @Override
    public AABB getRenderBoundingBox(CabinetBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        Level level = blockEntity.getLevel();
        boolean doubleHeight = false;
        if (level != null && !blockEntity.getBlockState().getValue(CabinetBlock.TOP)) {
            BlockState above = level.getBlockState(pos.above());
            doubleHeight = above.is(blockEntity.getBlockState().getBlock()) && above.getValue(CabinetBlock.TOP);
        }
        int height = doubleHeight ? 2 : 1;
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + height, pos.getZ() + 1.0);
    }
}
