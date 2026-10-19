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

                if (level.getBlockEntity(pos.above()) instanceof CabinetBlockEntity partner) {
                    openness = Math.max(openness, partner.getOpenNess(partialTick));
                }
            }
        }

        boolean mirror = state.getValue(CabinetBlock.MIRROR);
        var facing = state.getValue(CabinetBlock.FACING);

        poseStack.pushPose();

        poseStack.translate(0.0F, 1.0F, 1.0F);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        poseStack.translate(0.5F, -0.5F, 0.5F);

        model.render(poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay, mirror, doubleHeight, openness);
        poseStack.popPose();
    }

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
