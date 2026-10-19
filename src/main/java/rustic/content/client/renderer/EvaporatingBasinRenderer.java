package rustic.content.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.Rustic;
import rustic.content.blockentity.EvaporatingBasinBlockEntity;

public class EvaporatingBasinRenderer implements BlockEntityRenderer<EvaporatingBasinBlockEntity> {

    private static final AtomicBoolean FLUID_DIAG_LOGGED = new AtomicBoolean();

    private final ItemRenderer itemRenderer;

    public EvaporatingBasinRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(EvaporatingBasinBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        this.itemRenderer.renderStatic(new ItemStack(be.getBlockState().getBlock()), ItemDisplayContext.NONE,
                packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), (int) be.getBlockPos().asLong());
        poseStack.popPose();
        ItemStack dust = be.getItemHandler().getStackInSlot(EvaporatingBasinBlockEntity.OUTPUT_SLOT);
        if (!dust.isEmpty()) {
            FluidTankRenderUtil.renderStackedItems(dust, 16, be.getBlockPos().asLong(),
                    this.itemRenderer, poseStack, bufferSource, be.getLevel(), packedLight);

            if (bufferSource instanceof MultiBufferSource.BufferSource commits) {
                commits.endBatch();
            }
        }
        FluidStack fluid = be.getTank().getFluid();
        if (FLUID_DIAG_LOGGED.compareAndSet(false, true)) {
            Rustic.LOGGER.info("Phase 9 diag: evaporating basin BER saw fluid='{}' amount={}mb capacity={}mb at {}",
                    fluid.isEmpty() ? "<empty>" : fluid.getFluid().getFluidType(), fluid.getAmount(),
                    EvaporatingBasinBlockEntity.CAPACITY, be.getBlockPos());
        }
        FluidTankRenderUtil.renderFluidSurface(fluid, EvaporatingBasinBlockEntity.CAPACITY,
                0.0625F, 0.1875F, 0.1875F, 0.8125F, 0.1875F, 0.8125F, 0.1875F,
                poseStack, bufferSource, packedLight);
    }
}
