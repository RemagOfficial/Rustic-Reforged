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
import rustic.content.blockentity.CrushingTubBlockEntity;

/**
 * 1.21 form of the legacy {@code rustic.client.renderer.CrushingTubRenderer}. Draws the tub's input item as one
 * flat layer per eight items and the tank's fluid as a top surface quad.
 *
 * <p>Faithful numbers come straight from the legacy renderer: the item stack steps every {@code 8} items
 * ({@code ceil(count/8.0)}) and the fluid quad spans {@code 0.0625..0.9375} in x/z at
 * {@code y = 0.0625 + 0.5 * amount/capacity} with a {@code 0.0625} sprite UV inset. Both passes are delegated to
 * {@link FluidTankRenderUtil}.
 */
public class CrushingTubRenderer implements BlockEntityRenderer<CrushingTubBlockEntity> {

    /**
     * Round-5 diagnostic: logs the tank contents exactly once per game session so an in-game "no fluid visible"
     * report can be split into "tank is actually empty (fill wiring broken)" vs "fluid present but the surface quad
     * did not draw (rendering broken)" from {@code run/logs/latest.log} alone. Remove once the Phase 9 verdict is green.
     */
    private static final AtomicBoolean FLUID_DIAG_LOGGED = new AtomicBoolean();

    private final ItemRenderer itemRenderer;

    public CrushingTubRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CrushingTubBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // The block is ENTITYBLOCK_ANIMATED, so the chunk renderer skips its model and this renderer must draw the
        // open-tub container itself before layering the fluid surface and stacked items on top of it. We render the
        // block's own baked model through the item renderer. NOTE: BlockRenderDispatcher#renderSingleBlock is NOT
        // usable here - it switches on the block's render shape and, for ENTITYBLOCK_ANIMATED, delegates to the
        // item's custom renderer instead of drawing the model. ItemRenderer#renderStatic with the NONE display
        // context leaves the model at full scale but translated by (-0.5,-0.5,-0.5): applyTransform's +0.5/-0.5
        // centring cancels for the identity NO_TRANSFORM, yet ItemRenderer still subtracts a further 0.5, so the
        // block's top corner landed on the block centre in-game. A matching +0.5 pre-translate puts it back in the
        // block's local 0..1 space, aligned with the fluid quad below.
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        this.itemRenderer.renderStatic(new ItemStack(be.getBlockState().getBlock()), ItemDisplayContext.NONE,
                packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), (int) be.getBlockPos().asLong());
        poseStack.popPose();
        ItemStack input = be.getItemHandler().getStackInSlot(CrushingTubBlockEntity.INPUT_SLOT);
        if (!input.isEmpty()) {
            FluidTankRenderUtil.renderStackedItems(input, 8, be.getBlockPos().asLong(),
                    this.itemRenderer, poseStack, bufferSource, be.getLevel(), packedLight);
            // Legacy drew the item billboards first and the translucent fluid quad second, so the surface blends OVER
            // the submerged items (they sit INSIDE the liquid, not on top). Batched into one MultiBufferSource the
            // fluid's translucent layer can flush before the item layer and depth-cull the items below it (the
            // round-9 "items vanish under fluid" defect); committing the items here reproduces the legacy draw order.
            if (bufferSource instanceof MultiBufferSource.BufferSource commits) {
                commits.endBatch();
            }
        }
        FluidStack fluid = be.getTank().getFluid();
        if (FLUID_DIAG_LOGGED.compareAndSet(false, true)) {
            Rustic.LOGGER.info("Phase 9 diag: crushing tub BER saw fluid='{}' amount={}mb capacity={}mb at {}",
                    fluid.isEmpty() ? "<empty>" : fluid.getFluid().getFluidType(), fluid.getAmount(),
                    CrushingTubBlockEntity.CAPACITY, be.getBlockPos());
        }
        FluidTankRenderUtil.renderFluidSurface(fluid, CrushingTubBlockEntity.CAPACITY,
                0.0625F, 0.5F, 0.0625F, 0.9375F, 0.0625F, 0.9375F, 0.0625F,
                poseStack, bufferSource, packedLight);
    }
}
