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

/**
 * 1.21 form of the legacy {@code rustic.client.renderer.EvaporatingBasinRenderer}. Draws the crystallised dust in
 * the output slot as stacked flat layers and the evaporating fluid as a low top surface quad.
 *
 * <p>Faithful numbers from the legacy renderer: the item stack steps every {@code 16} items (the basin's divisor
 * differs from the tub's {@code 8}), and the fluid quad spans {@code 0.1875..0.8125} in x/z at
 * {@code y = 0.0625 + 0.1875 * amount/capacity} - the shallow fill of a wide basin - with a {@code 0.1875} sprite
 * UV inset. Both passes delegate to {@link FluidTankRenderUtil}.
 */
public class EvaporatingBasinRenderer implements BlockEntityRenderer<EvaporatingBasinBlockEntity> {

    /** Round-5 diagnostic, see {@link CrushingTubRenderer#FLUID_DIAG_LOGGED}; remove once the Phase 9 verdict is green. */
    private static final AtomicBoolean FLUID_DIAG_LOGGED = new AtomicBoolean();

    private final ItemRenderer itemRenderer;

    public EvaporatingBasinRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(EvaporatingBasinBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // ENTITYBLOCK_ANIMATED: draw the shallow open-tray container first (via the block's own baked model through
        // the item renderer), then the evaporating fluid and the crystallised-dust layers on top of it.
        // renderSingleBlock is unusable and renderStatic(NONE) leaves the model at full scale but translated by
        // (-0.5,-0.5,-0.5), so a +0.5 pre-translate recentres it in the block's 0..1 space - see CrushingTubRenderer.
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        this.itemRenderer.renderStatic(new ItemStack(be.getBlockState().getBlock()), ItemDisplayContext.NONE,
                packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), (int) be.getBlockPos().asLong());
        poseStack.popPose();
        ItemStack dust = be.getItemHandler().getStackInSlot(EvaporatingBasinBlockEntity.OUTPUT_SLOT);
        if (!dust.isEmpty()) {
            FluidTankRenderUtil.renderStackedItems(dust, 16, be.getBlockPos().asLong(),
                    this.itemRenderer, poseStack, bufferSource, be.getLevel(), packedLight);
            // Commit the dust billboards before the fluid so the translucent surface blends over the submerged dust
            // (legacy order); see CrushingTubRenderer for why the batched flush order matters.
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
